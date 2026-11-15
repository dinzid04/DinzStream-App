/**
 * Vercel Serverless Function for Sticker.ly search & pack scraping.
 * Zero external dependencies (Node 18+ built-in fetch).
 */

const USER_AGENT =
  "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36";

function decodeHtml(str) {
  if (!str) return "";
  return String(str)
    .replace(/&amp;/g, "&")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&#x27;/g, "'")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .trim();
}

function extractPackId(raw) {
  if (!raw) return "";
  const trimmed = String(raw).trim();
  const urlMatch = trimmed.match(/sticker\.ly\/s\/([A-Za-z0-9_-]{3,24})/i);
  if (urlMatch) return urlMatch[1];
  if (/^[A-Za-z0-9_-]{4,16}$/.test(trimmed)) return trimmed;
  return "";
}

function normalizeStickerUrl(url) {
  if (!url) return "";
  return String(url).trim().replace(/^http:\/\//i, "https://");
}

function parseMetaContent(html, property) {
  const escaped = property.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const patterns = [
    new RegExp(
      `<meta\\s+[^>]*property=["']${escaped}["'][^>]*content=["']([^"']*)["']`,
      "i"
    ),
    new RegExp(
      `<meta\\s+[^>]*content=["']([^"']*)["'][^>]*property=["']${escaped}["']`,
      "i"
    ),
  ];
  for (const re of patterns) {
    const m = html.match(re);
    if (m && m[1]) return decodeHtml(m[1]);
  }
  return "";
}

async function probeAndFixStaticWebp(pack) {
  if (!pack || !Array.isArray(pack.stickers) || pack.stickers.length === 0) return pack;
  const firstUrl = pack.stickers[0].url || "";
  if (!/\.webp(\?.*)?$/i.test(firstUrl)) return pack;

  let firstOk = false;
  try {
    const headRes = await fetch(firstUrl, {
      method: "HEAD",
      headers: { "User-Agent": USER_AGENT },
      signal: AbortSignal.timeout(2200),
    });
    firstOk = headRes.ok;
  } catch {
    firstOk = false;
  }
  if (firstOk) return pack;

  const pngCandidate = firstUrl.replace(/\.webp(\?.*)?$/i, ".png$1");
  let pngOk = false;
  try {
    const pngRes = await fetch(pngCandidate, {
      method: "HEAD",
      headers: { "User-Agent": USER_AGENT },
      signal: AbortSignal.timeout(2200),
    });
    pngOk = pngRes.ok;
  } catch {
    pngOk = false;
  }
  if (!pngOk) return pack;

  const fixedStickers = pack.stickers.map((s) => {
    const nextUrl = /\.webp(\?.*)?$/i.test(s.url)
      ? s.url.replace(/\.webp(\?.*)?$/i, ".png$1")
      : s.url;
    return {
      url: nextUrl,
      isAnimated: nextUrl.endsWith(".gif"),
    };
  });
  const fixedThumb = /\.webp(\?.*)?$/i.test(pack.thumbnailUrl || "")
    ? pack.thumbnailUrl.replace(/\.webp(\?.*)?$/i, ".png$1")
    : pack.thumbnailUrl;

  return {
    ...pack,
    thumbnailUrl: fixedThumb || fixedStickers[0].url,
    stickers: fixedStickers,
  };
}

async function fetchPackFromHtml(packId) {
  const sourceUrl = `https://sticker.ly/s/${packId}`;
  const res = await fetch(sourceUrl, {
    headers: {
      "User-Agent": USER_AGENT,
      Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
      "Accept-Language": "id-ID,id;q=0.9,en-US;q=0.8",
    },
    signal: AbortSignal.timeout(7000),
  });

  if (!res.ok) {
    throw new Error(`Sticker.ly HTTP ${res.status}`);
  }

  const html = await res.text();
  const title =
    parseMetaContent(html, "og:title") ||
    decodeHtml((html.match(/<title>([^<]+)<\/title>/i) || [])[1] || "") ||
    `Pack ${packId}`;
  const ogImage = normalizeStickerUrl(parseMetaContent(html, "og:image"));
  const packLooksAnimated = /\.preview\.(png|webp)/i.test(ogImage);

  const authorMatch = html.match(
    /class=["'][^"']*sticker_author[^"']*["'][^>]*>([^<]+)</i
  );
  const author = decodeHtml(authorMatch ? authorMatch[1] : "") || "sticker.ly";

  const ulMatch = html.match(
    /<ul[^>]*id=["']content_images["'][^>]*>([\s\S]*?)<\/ul>/i
  );
  const sectionHtml = ulMatch ? ulMatch[1] : "";

  const seen = new Set();
  const stickers = [];
  const imgTagRegex = /<img\s+[^>]*>/gi;
  let tagMatch;

  while ((tagMatch = imgTagRegex.exec(sectionHtml)) !== null) {
    const tag = tagMatch[0];
    const srcMatch = tag.match(/\bsrc=["'](https:\/\/stickerly\.pstatic\.net\/sticker_pack\/[^"']+)["']/i);
    if (!srcMatch) continue;

    let url = normalizeStickerUrl(srcMatch[1]);
    if (!url || seen.has(url)) continue;
    if (!url.includes(`/${packId}/`) && stickers.length > 0 && sectionHtml === "") {
      continue;
    }

    const onerrorExpr = tag.match(/onerror\s*=\s*["']([^"']*)["']/i)?.[1] || "";
    const hasWebpFallback = /\.webp\b/i.test(onerrorExpr);
    const pngFallbackMatch = onerrorExpr.match(/https?:\/\/stickerly\.pstatic\.net\/sticker_pack\/[^"'\s)]+\.png/i);
    const hasPngFallback = Boolean(pngFallbackMatch) || /\.png\b/i.test(onerrorExpr);
    const isAnimated =
      url.endsWith(".gif") ||
      (url.endsWith(".webp") && (hasWebpFallback || (!hasPngFallback && packLooksAnimated)));

    if (!isAnimated && hasPngFallback && /\.webp(\?.*)?$/i.test(url)) {
      url = normalizeStickerUrl(pngFallbackMatch?.[0] || url.replace(/\.webp(\?.*)?$/i, ".png$1"));
    }

    if (seen.has(url)) continue;
    seen.add(url);
    stickers.push({ url, isAnimated });
  }

  if (stickers.length === 0) {
    throw new Error("Stiker di dalam paket tidak ditemukan");
  }

  const normalizedThumb =
    !packLooksAnimated &&
    stickers.every((s) => !s.isAnimated) &&
    /\.webp(\?.*)?$/i.test(ogImage)
      ? ogImage.replace(/\.webp(\?.*)?$/i, ".png$1")
      : ogImage;

  const thumbnailUrl = normalizedThumb || stickers[0].url;
  return await probeAndFixStaticWebp({
    id: packId,
    name: title,
    author,
    thumbnailUrl,
    sourceUrl,
    stickers,
  });
}

async function fetchPackFromSnowping(packUrl, packId) {
  const res = await fetch(
    `https://api.snowping.cfd/api/downloader/stikerly?url=${encodeURIComponent(packUrl)}`,
    {
      headers: { "User-Agent": USER_AGENT },
      signal: AbortSignal.timeout(5500),
    }
  );
  if (!res.ok) throw new Error(`Snowping HTTP ${res.status}`);
  const data = await res.json();
  const root = data?.result || data?.data || data;
  const rawStickers = Array.isArray(root?.stickers)
    ? root.stickers
    : Array.isArray(root?.sticker)
      ? root.sticker
      : [];

  const stickers = rawStickers
    .map((item) => {
      if (typeof item === "string") {
        const url = normalizeStickerUrl(item);
        return url ? { url, isAnimated: url.endsWith(".gif") || url.endsWith(".webp") } : null;
      }
      const url = normalizeStickerUrl(item?.url || item?.image || item?.src || "");
      if (!url) return null;
      return {
        url,
        isAnimated: Boolean(item?.isAnimated ?? item?.is_animated ?? (url.endsWith(".gif") || url.endsWith(".webp"))),
      };
    })
    .filter(Boolean);

  if (stickers.length === 0) throw new Error("Empty Snowping pack");

  return await probeAndFixStaticWebp({
    id: packId,
    name: root?.name || root?.title || `Pack ${packId}`,
    author: root?.author || root?.creator || "sticker.ly",
    thumbnailUrl: normalizeStickerUrl(root?.thumbnail || root?.thumbnailUrl || stickers[0].url),
    sourceUrl: packUrl,
    stickers,
  });
}

async function fetchQuickThumb(packId) {
  try {
    const res = await fetch(`https://sticker.ly/s/${packId}`, {
      headers: { "User-Agent": USER_AGENT },
      signal: AbortSignal.timeout(2200),
    });
    if (!res.ok) return "";
    const html = await res.text();
    return normalizeStickerUrl(parseMetaContent(html, "og:image"));
  } catch {
    return "";
  }
}

async function searchViaFaa(query) {
  const res = await fetch(
    `https://api-faa.my.id/faa/stickerly?q=${encodeURIComponent(query)}`,
    {
      headers: {
        "User-Agent": USER_AGENT,
        Accept: "application/json, text/plain, */*",
      },
      signal: AbortSignal.timeout(8500),
    }
  );
  if (!res.ok) throw new Error(`FAA HTTP ${res.status}`);
  const data = await res.json();
  const rawList = Array.isArray(data?.results) ? data.results : [];
  if (rawList.length === 0) throw new Error("No FAA results");

  const mapped = [];
  const seen = new Set();
  for (const item of rawList) {
    const id = extractPackId(item?.url);
    if (!id || seen.has(id)) continue;
    seen.add(id);
    mapped.push({
      id,
      name: decodeHtml(item?.title || `Pack ${id}`),
      author: decodeHtml(item?.creator || "sticker.ly"),
      thumbnailUrl: normalizeStickerUrl(item?.thumbnail || ""),
      sourceUrl: `https://sticker.ly/s/${id}`,
    });
    if (mapped.length >= 16) break;
  }

  // Opportunistically enrich top 6 thumbnails within a strict 1500ms budget
  const enrichTargets = mapped.slice(0, 6);
  const enrichWork = Promise.allSettled(
    enrichTargets.map(async (entry) => {
      if (!entry.thumbnailUrl) {
        entry.thumbnailUrl = await fetchQuickThumb(entry.id);
      }
    })
  );
  await Promise.race([
    enrichWork,
    new Promise((resolve) => setTimeout(resolve, 1500)),
  ]);

  return mapped;
}

async function searchViaSnowping(query) {
  const res = await fetch(
    `https://api.snowping.cfd/api/search/stikerly?query=${encodeURIComponent(query)}`,
    {
      headers: {
        "User-Agent": USER_AGENT,
        Accept: "application/json, text/plain, */*",
      },
      signal: AbortSignal.timeout(5500),
    }
  );
  if (!res.ok) throw new Error(`Snowping HTTP ${res.status}`);
  const data = await res.json();
  const rawList = Array.isArray(data?.result)
    ? data.result
    : Array.isArray(data?.results)
      ? data.results
      : Array.isArray(data?.data)
        ? data.data
        : [];
  if (rawList.length === 0) throw new Error("No Snowping results");

  const mapped = [];
  const seen = new Set();
  for (const item of rawList) {
    const id = extractPackId(item?.url || item?.packUrl || item?.id);
    if (!id || seen.has(id)) continue;
    seen.add(id);
    mapped.push({
      id,
      name: decodeHtml(item?.name || item?.title || `Pack ${id}`),
      author: decodeHtml(item?.author || item?.creator || "sticker.ly"),
      thumbnailUrl: normalizeStickerUrl(item?.thumbnail || item?.thumbnailUrl || ""),
      sourceUrl: `https://sticker.ly/s/${id}`,
    });
    if (mapped.length >= 16) break;
  }
  return mapped;
}

async function searchViaMirror(query) {
  const res = await fetch(
    `https://dinz-streaming-khaki.vercel.app/api/stickerly/search?q=${encodeURIComponent(query)}`,
    {
      headers: { Accept: "application/json" },
      signal: AbortSignal.timeout(8000),
    }
  );
  if (!res.ok) throw new Error(`Mirror HTTP ${res.status}`);
  const data = await res.json();
  const rawList = Array.isArray(data?.results) ? data.results : [];
  if (rawList.length === 0) throw new Error("Empty mirror results");

  const mapped = rawList.slice(0, 16).map((item) => ({
    id: item.id,
    name: decodeHtml(item.name || `Pack ${item.id}`),
    author: decodeHtml(item.author || "sticker.ly"),
    thumbnailUrl: normalizeStickerUrl(item.thumbnailUrl || ""),
    sourceUrl: item.sourceUrl || `https://sticker.ly/s/${item.id}`,
  }));

  const enrichWork = Promise.allSettled(
    mapped.slice(0, 6).map(async (entry) => {
      if (!entry.thumbnailUrl && entry.id) {
        entry.thumbnailUrl = await fetchQuickThumb(entry.id);
      }
    })
  );
  await Promise.race([
    enrichWork,
    new Promise((resolve) => setTimeout(resolve, 1500)),
  ]);

  return mapped;
}

export default async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET,OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") {
    res.status(204).end();
    return;
  }

  const urlObj = new URL(req.url || "/", "http://localhost");
  const action = (req.query?.action || urlObj.searchParams.get("action") || "search").toLowerCase();

  try {
    if (action === "pack") {
      const rawUrl = req.query?.url || urlObj.searchParams.get("url") || req.query?.id || urlObj.searchParams.get("id") || "";
      const packId = extractPackId(rawUrl);
      if (!packId) {
        res.status(400).json({ status: false, error: "URL atau ID paket Sticker.ly tidak valid" });
        return;
      }
      const packUrl = `https://sticker.ly/s/${packId}`;
      const pack = await Promise.any([
        fetchPackFromHtml(packId),
        fetchPackFromSnowping(packUrl, packId),
      ]);
      res.setHeader("Cache-Control", "s-maxage=300, stale-while-revalidate=600");
      res.status(200).json({ status: true, pack });
      return;
    }

    const q = (req.query?.q || urlObj.searchParams.get("q") || "").trim();
    if (!q) {
      res.status(400).json({ status: false, error: "Parameter pencarian q wajib diisi" });
      return;
    }

    const results = await Promise.any([
      searchViaSnowping(q),
      searchViaFaa(q),
      searchViaMirror(q),
    ]);

    res.setHeader("Cache-Control", "s-maxage=180, stale-while-revalidate=300");
    res.status(200).json({ status: true, query: q, results });
  } catch (err) {
    const detail = Array.isArray(err?.errors)
      ? err.errors.map((e) => e?.message || String(e)).join("; ")
      : err?.message || "Gagal mengambil data Sticker.ly dari server";
    res.status(502).json({
      status: false,
      error: detail,
    });
  }
}
