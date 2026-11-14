package com.yuranime.dinz.data.firebase

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Helper deteksi media animasi (GIF, WebP animasi, APNG, Video)
 * serta konverter Video -> Animated GIF (GIF89a) secara lokal di perangkat.
 */
object VideoToGifConverter {
    private const val TAG = "VideoToGifConverter"

    private const val MAX_CLIP_DURATION_MS = 5_000L
    private const val MAX_FRAME_COUNT = 42
    private const val MIN_DELAY_CS = 6
    private const val MAX_DELAY_CS = 50

    fun isGifBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 6) return false
        val header = String(bytes, 0, 6, Charsets.US_ASCII)
        return header == "GIF87a" || header == "GIF89a"
    }

    fun isAnimatedWebpBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 21) return false
        val riff = String(bytes, 0, 4, Charsets.US_ASCII)
        val webp = String(bytes, 8, 4, Charsets.US_ASCII)
        if (riff != "RIFF" || webp != "WEBP") return false

        var offset = 12
        while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
            val chunkSize = (bytes[offset + 4].toInt() and 0xFF) or
                ((bytes[offset + 5].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 6].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 7].toInt() and 0xFF) shl 24)
            if (chunkId == "ANIM") return true
            if (chunkId == "VP8X" && offset + 8 < bytes.size) {
                // VP8X flags bit 1 (0x02) menandakan animasi
                if ((bytes[offset + 8].toInt() and 0x02) != 0) return true
            }
            if (chunkSize < 0) break
            val paddedSize = chunkSize + (chunkSize and 1)
            val next = offset.toLong() + 8L + paddedSize.toLong()
            if (next > bytes.size) break
            offset = next.toInt()
        }
        return false
    }

    fun isAnimatedPngBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 24) return false
        val isPng = (bytes[0].toInt() and 0xFF) == 0x89 &&
            bytes[1].toInt() == 0x50 &&
            bytes[2].toInt() == 0x4E &&
            bytes[3].toInt() == 0x47
        if (!isPng) return false

        var offset = 8
        while (offset + 8 <= bytes.size) {
            val length = ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
            val chunkType = String(bytes, offset + 4, 4, Charsets.US_ASCII)
            if (chunkType == "acTL") return true
            if (chunkType == "IDAT" || length < 0) break
            val next = offset.toLong() + 12L + length.toLong()
            if (next > bytes.size) break
            offset = next.toInt()
        }
        return false
    }

    fun isAnimatedImageBytes(bytes: ByteArray): Boolean {
        return isGifBytes(bytes) || isAnimatedWebpBytes(bytes) || isAnimatedPngBytes(bytes)
    }

    fun detectImageMimeAndExt(bytes: ByteArray): Pair<String, String> {
        if (isGifBytes(bytes)) return "image/gif" to "gif"
        if (bytes.size >= 12 &&
            String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP"
        ) {
            return "image/webp" to "webp"
        }
        if (bytes.size >= 8 &&
            (bytes[0].toInt() and 0xFF) == 0x89 &&
            bytes[1].toInt() == 0x50 &&
            bytes[2].toInt() == 0x4E &&
            bytes[3].toInt() == 0x47
        ) {
            return "image/png" to "png"
        }
        return "image/jpeg" to "jpg"
    }

    fun isVideoUri(context: Context, uri: Uri): Boolean {
        val mime = runCatching {
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                context.contentResolver.getType(uri)
            } else {
                null
            }
        }.getOrNull()

        if (!mime.isNullOrBlank()) {
            if (mime.startsWith("video/", ignoreCase = true)) return true
            if (mime.startsWith("image/", ignoreCase = true)) return false
        }

        val pathLower = (uri.lastPathSegment ?: uri.toString()).lowercase()
        val videoExts = listOf(".mp4", ".mov", ".webm", ".mkv", ".avi", ".3gp", ".m4v")
        if (videoExts.any { pathLower.contains(it) }) return true

        // Fallback magic bytes ISO BMFF (ftyp) atau EBML (webm/mkv)
        return runCatching {
            val header = ByteArray(16)
            val count = openInputStream(context, uri)?.use { it.read(header) } ?: 0
            if (count >= 12 && String(header, 4, 4, Charsets.US_ASCII) == "ftyp") {
                true
            } else if (count >= 4 &&
                (header[0].toInt() and 0xFF) == 0x1A &&
                (header[1].toInt() and 0xFF) == 0x45 &&
                (header[2].toInt() and 0xFF) == 0xDF &&
                (header[3].toInt() and 0xFF) == 0xA3
            ) {
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    /**
     * Mengonversi video pendek (maks 5 detik pertama) menjadi Animated GIF loop tak terbatas.
     */
    fun convertVideoToGif(context: Context, uri: Uri, isBanner: Boolean): File? {
        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }

            val rawDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 3_000L
            val clipMs = rawDuration.coerceIn(100L, MAX_CLIP_DURATION_MS)

            var srcW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: if (isBanner) 420 else 224
            var srcH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: if (isBanner) 236 else 224
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                val tmp = srcW
                srcW = srcH
                srcH = tmp
            }

            val maxDim = if (isBanner) 420 else 224
            val scale = min(maxDim.toFloat() / max(1, srcW), maxDim.toFloat() / max(1, srcH))
                .coerceAtMost(1f)
            val targetW = max(64, (srcW * scale).roundToInt())
            val targetH = max(64, (srcH * scale).roundToInt())

            val targetFps = 10
            val frameCount = ((clipMs * targetFps) / 1000L).toInt().coerceIn(2, MAX_FRAME_COUNT)
            val delayCs = ((clipMs.toFloat() / frameCount) / 10f).roundToInt()
                .coerceIn(MIN_DELAY_CS, MAX_DELAY_CS)
            val stepUs = (clipMs * 1000L) / frameCount

            val prefix = if (isBanner) "tg_banner" else "tg_avatar"
            val outFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.gif")

            FileOutputStream(outFile).use { fos ->
                val encoder = Gif89Encoder(fos, targetW, targetH, delayCs)
                encoder.start()

                var encodedFrames = 0
                for (i in 0 until frameCount) {
                    val timeUs = (i * stepUs).coerceAtLeast(0L)
                    val frame = extractScaledFrame(retriever, timeUs, targetW, targetH)
                    if (frame != null) {
                        val normalized = if (frame.width != targetW || frame.height != targetH) {
                            Bitmap.createScaledBitmap(frame, targetW, targetH, true).also {
                                if (it !== frame) frame.recycle()
                            }
                        } else {
                            frame
                        }
                        encoder.addFrame(normalized)
                        normalized.recycle()
                        encodedFrames++
                    }
                }
                encoder.finish()
                if (encodedFrames == 0) {
                    outFile.delete()
                    return null
                }
            }
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengonversi video ke GIF: ${e.message}", e)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extractScaledFrame(
        retriever: MediaMetadataRetriever,
        timeUs: Long,
        targetW: Int,
        targetH: Int
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            runCatching {
                retriever.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                    targetW,
                    targetH
                )
            }.getOrNull()?.let { return it }
        }
        return runCatching {
            val raw = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return null
            if (raw.width == targetW && raw.height == targetH) {
                raw
            } else {
                Bitmap.createScaledBitmap(raw, targetW, targetH, true).also {
                    if (it !== raw) raw.recycle()
                }
            }
        }.getOrNull()
    }

    private fun openInputStream(context: Context, uri: Uri) = when {
        uri.scheme == "file" && !uri.path.isNullOrBlank() -> FileInputStream(File(uri.path!!))
        else -> context.contentResolver.openInputStream(uri)
    }

    /**
     * Encoder GIF89a (NETSCAPE2.0 infinite loop) dengan kuantisasi NeuQuant 256 warna dan LZW.
     */
    private class Gif89Encoder(
        private val out: OutputStream,
        private val width: Int,
        private val height: Int,
        private val delayCs: Int
    ) {
        private var firstFrame = true
        private val argbBuffer = IntArray(width * height)
        private val rgbBuffer = ByteArray(width * height * 3)
        private val indexedPixels = ByteArray(width * height)

        fun start() {
            writeAscii("GIF89a")
            // Logical Screen Descriptor (header penuh ditulis saat frame pertama)
        }

        fun addFrame(bitmap: Bitmap) {
            bitmap.getPixels(argbBuffer, 0, width, 0, 0, width, height)
            var pIdx = 0
            for (i in argbBuffer.indices) {
                val px = argbBuffer[i]
                rgbBuffer[pIdx++] = ((px shr 16) and 0xFF).toByte()
                rgbBuffer[pIdx++] = ((px shr 8) and 0xFF).toByte()
                rgbBuffer[pIdx++] = (px and 0xFF).toByte()
            }

            val nq = NeuQuant(rgbBuffer, sampleFac = 10)
            val colorTable = nq.process()
            for (i in indexedPixels.indices) {
                val base = i * 3
                val r = rgbBuffer[base].toInt() and 0xFF
                val g = rgbBuffer[base + 1].toInt() and 0xFF
                val b = rgbBuffer[base + 2].toInt() and 0xFF
                indexedPixels[i] = nq.map(r, g, b).toByte()
            }

            if (firstFrame) {
                writeLsd(colorTable)
                writeNetscapeLoopExt()
                firstFrame = false
                writeGraphicControlExt()
                writeImageDesc(hasLocalPalette = false)
            } else {
                writeGraphicControlExt()
                writeImageDesc(hasLocalPalette = true)
                out.write(colorTable, 0, 768)
            }

            LzwEncoder(width, height, indexedPixels, colorDepth = 8).encode(out)
        }

        fun finish() {
            if (firstFrame) {
                // Fallback jika tidak ada frame
                writeShort(width)
                writeShort(height)
                out.write(0x70)
                out.write(0)
                out.write(0)
            }
            out.write(0x3B) // GIF Trailer
            out.flush()
        }

        private fun writeLsd(globalColorTable: ByteArray) {
            writeShort(width)
            writeShort(height)
            // Global Color Table Flag (1), Color Res (7), Sort (0), Size of GCT (7 -> 256 warna)
            out.write(0xF7)
            out.write(0) // Background color index
            out.write(0) // Pixel aspect ratio
            out.write(globalColorTable, 0, 768)
        }

        private fun writeNetscapeLoopExt() {
            out.write(0x21)
            out.write(0xFF)
            out.write(11)
            writeAscii("NETSCAPE2.0")
            out.write(3)
            out.write(1)
            writeShort(0) // Infinite loop
            out.write(0)
        }

        private fun writeGraphicControlExt() {
            out.write(0x21)
            out.write(0xF9)
            out.write(4)
            out.write(0) // Dispose = 0, tidak transparan
            writeShort(delayCs)
            out.write(0)
            out.write(0)
        }

        private fun writeImageDesc(hasLocalPalette: Boolean) {
            out.write(0x2C)
            writeShort(0)
            writeShort(0)
            writeShort(width)
            writeShort(height)
            out.write(if (hasLocalPalette) 0x87 else 0x00)
        }

        private fun writeShort(value: Int) {
            out.write(value and 0xFF)
            out.write((value shr 8) and 0xFF)
        }

        private fun writeAscii(text: String) {
            for (ch in text) out.write(ch.code)
        }
    }

    /**
     * Kuantisasi warna NeuQuant 256-entry (urutan RGB konsisten dengan palet GIF).
     */
    private class NeuQuant(private val thepic: ByteArray, private val sampleFac: Int) {
        companion object {
            private const val NETSIZE = 256
            private const val PRIME1 = 499
            private const val PRIME2 = 491
            private const val PRIME3 = 487
            private const val PRIME4 = 503
            private const val MIN_PICTURE_BYTES = 3 * PRIME4
            private const val MAXNETPOS = NETSIZE - 1
            private const val NETBIASSHIFT = 4
            private const val NCYCLES = 100
            private const val INTBIASSHIFT = 16
            private const val INTBIAS = 1 shl INTBIASSHIFT
            private const val GAMMASHIFT = 10
            private const val BETASHIFT = 10
            private const val BETA = INTBIAS shr BETASHIFT
            private const val BETAGAMMA = INTBIAS shl (GAMMASHIFT - BETASHIFT)
            private const val INITRAD = NETSIZE shr 3
            private const val RADIUSBIASSHIFT = 6
            private const val RADIUSBIAS = 1 shl RADIUSBIASSHIFT
            private const val INITRADIUS = INITRAD * RADIUSBIAS
            private const val RADIUSDEC = 30
            private const val ALPHABIASSHIFT = 10
            private const val INITALPHA = 1 shl ALPHABIASSHIFT
            private const val RADBIASSHIFT = 8
            private const val RADBIAS = 1 shl RADBIASSHIFT
            private const val ALPHARADBSHIFT = ALPHABIASSHIFT + RADBIASSHIFT
            private const val ALPHARADBIAS = 1 shl ALPHARADBSHIFT
        }

        private val network = Array(NETSIZE) { IntArray(4) }
        private val netindex = IntArray(256)
        private val bias = IntArray(NETSIZE)
        private val freq = IntArray(NETSIZE)
        private val radpower = IntArray(INITRAD)

        init {
            for (i in 0 until NETSIZE) {
                val p = network[i]
                val v = (i shl (NETBIASSHIFT + 8)) / NETSIZE
                p[0] = v
                p[1] = v
                p[2] = v
                freq[i] = INTBIAS / NETSIZE
                bias[i] = 0
            }
        }

        fun process(): ByteArray {
            learn()
            unbiasnet()
            inxbuild()
            val map = ByteArray(3 * NETSIZE)
            val index = IntArray(NETSIZE)
            for (i in 0 until NETSIZE) index[network[i][3]] = i
            var k = 0
            for (i in 0 until NETSIZE) {
                val j = index[i]
                map[k++] = network[j][0].toByte()
                map[k++] = network[j][1].toByte()
                map[k++] = network[j][2].toByte()
            }
            return map
        }

        fun map(c0: Int, c1: Int, c2: Int): Int {
            var bestd = 1000
            var best = -1
            var i = netindex[c1]
            var j = i - 1

            while (i < NETSIZE || j >= 0) {
                if (i < NETSIZE) {
                    val p = network[i]
                    var dist = p[1] - c1
                    if (dist >= bestd) {
                        i = NETSIZE
                    } else {
                        i++
                        if (dist < 0) dist = -dist
                        var a = p[0] - c0
                        if (a < 0) a = -a
                        dist += a
                        if (dist < bestd) {
                            a = p[2] - c2
                            if (a < 0) a = -a
                            dist += a
                            if (dist < bestd) {
                                bestd = dist
                                best = p[3]
                            }
                        }
                    }
                }
                if (j >= 0) {
                    val p = network[j]
                    var dist = c1 - p[1]
                    if (dist >= bestd) {
                        j = -1
                    } else {
                        j--
                        if (dist < 0) dist = -dist
                        var a = p[0] - c0
                        if (a < 0) a = -a
                        dist += a
                        if (dist < bestd) {
                            a = p[2] - c2
                            if (a < 0) a = -a
                            dist += a
                            if (dist < bestd) {
                                bestd = dist
                                best = p[3]
                            }
                        }
                    }
                }
            }
            return best
        }

        private fun unbiasnet() {
            for (i in 0 until NETSIZE) {
                network[i][0] = network[i][0] shr NETBIASSHIFT
                network[i][1] = network[i][1] shr NETBIASSHIFT
                network[i][2] = network[i][2] shr NETBIASSHIFT
                network[i][3] = i
            }
        }

        private fun inxbuild() {
            var previouscol = 0
            var startpos = 0
            for (i in 0 until NETSIZE) {
                val p = network[i]
                var smallpos = i
                var smallval = p[1]
                for (j in i + 1 until NETSIZE) {
                    val q = network[j]
                    if (q[1] < smallval) {
                        smallpos = j
                        smallval = q[1]
                    }
                }
                val q = network[smallpos]
                if (i != smallpos) {
                    var j = q[0]; q[0] = p[0]; p[0] = j
                    j = q[1]; q[1] = p[1]; p[1] = j
                    j = q[2]; q[2] = p[2]; p[2] = j
                    j = q[3]; q[3] = p[3]; p[3] = j
                }
                if (smallval != previouscol) {
                    netindex[previouscol] = (startpos + i) shr 1
                    for (j in previouscol + 1 until smallval) netindex[j] = i
                    previouscol = smallval
                    startpos = i
                }
            }
            netindex[previouscol] = (startpos + MAXNETPOS) shr 1
            for (j in previouscol + 1 until 256) netindex[j] = MAXNETPOS
        }

        private fun learn() {
            val lengthcount = thepic.size
            val fac = if (lengthcount < MIN_PICTURE_BYTES) 1 else sampleFac
            val alphadec = 30 + ((fac - 1) / 3)
            val samplepixels = lengthcount / (3 * fac)
            var delta = max(1, samplepixels / NCYCLES)
            var alpha = INITALPHA
            var radius = INITRADIUS
            var rad = radius shr RADIUSBIASSHIFT
            if (rad <= 1) rad = 0
            for (i in 0 until rad) {
                radpower[i] = alpha * (((rad * rad - i * i) * RADBIAS) / (rad * rad))
            }

            val step = when {
                lengthcount < MIN_PICTURE_BYTES -> 3
                lengthcount % PRIME1 != 0 -> 3 * PRIME1
                lengthcount % PRIME2 != 0 -> 3 * PRIME2
                lengthcount % PRIME3 != 0 -> 3 * PRIME3
                else -> 3 * PRIME4
            }

            var pix = 0
            var i = 0
            while (i < samplepixels) {
                val c0 = (thepic[pix].toInt() and 0xFF) shl NETBIASSHIFT
                val c1 = (thepic[pix + 1].toInt() and 0xFF) shl NETBIASSHIFT
                val c2 = (thepic[pix + 2].toInt() and 0xFF) shl NETBIASSHIFT
                val j = contest(c0, c1, c2)
                altersingle(alpha, j, c0, c1, c2)
                if (rad != 0) alterneigh(rad, j, c0, c1, c2)

                pix += step
                if (pix >= lengthcount) pix -= lengthcount
                i++
                if (i % delta == 0) {
                    alpha -= alpha / alphadec
                    radius -= radius / RADIUSDEC
                    rad = radius shr RADIUSBIASSHIFT
                    if (rad <= 1) rad = 0
                    for (k in 0 until rad) {
                        radpower[k] = alpha * (((rad * rad - k * k) * RADBIAS) / (rad * rad))
                    }
                }
            }
        }

        private fun contest(c0: Int, c1: Int, c2: Int): Int {
            var bestd = Int.MAX_VALUE
            var bestbiasd = bestd
            var bestpos = -1
            var bestbiaspos = bestpos

            for (i in 0 until NETSIZE) {
                val n = network[i]
                val dist = abs(n[0] - c0) + abs(n[1] - c1) + abs(n[2] - c2)
                if (dist < bestd) {
                    bestd = dist
                    bestpos = i
                }
                val biasdist = dist - (bias[i] shr (INTBIASSHIFT - NETBIASSHIFT))
                if (biasdist < bestbiasd) {
                    bestbiasd = biasdist
                    bestbiaspos = i
                }
                val betafreq = freq[i] shr BETASHIFT
                freq[i] -= betafreq
                bias[i] += betafreq shl GAMMASHIFT
            }
            freq[bestpos] += BETA
            bias[bestpos] -= BETAGAMMA
            return bestbiaspos
        }

        private fun altersingle(alpha: Int, i: Int, c0: Int, c1: Int, c2: Int) {
            val n = network[i]
            n[0] -= (alpha * (n[0] - c0)) / INITALPHA
            n[1] -= (alpha * (n[1] - c1)) / INITALPHA
            n[2] -= (alpha * (n[2] - c2)) / INITALPHA
        }

        private fun alterneigh(rad: Int, i: Int, c0: Int, c1: Int, c2: Int) {
            val lo = max(i - rad, -1)
            val hi = min(i + rad, NETSIZE)
            var j = i + 1
            var k = i - 1
            var m = 1
            while (j < hi || k > lo) {
                val a = radpower[m++]
                if (j < hi) {
                    val p = network[j++]
                    p[0] -= (a * (p[0] - c0)) / ALPHARADBIAS
                    p[1] -= (a * (p[1] - c1)) / ALPHARADBIAS
                    p[2] -= (a * (p[2] - c2)) / ALPHARADBIAS
                }
                if (k > lo) {
                    val p = network[k--]
                    p[0] -= (a * (p[0] - c0)) / ALPHARADBIAS
                    p[1] -= (a * (p[1] - c1)) / ALPHARADBIAS
                    p[2] -= (a * (p[2] - c2)) / ALPHARADBIAS
                }
            }
        }
    }

    /**
     * Kompresi bitstream LZW standar GIF (8-bit color depth, kode 9..12 bit).
     */
    private class LzwEncoder(
        private val imgW: Int,
        private val imgH: Int,
        private val pixAry: ByteArray,
        colorDepth: Int
    ) {
        companion object {
            private const val EOF = -1
            private const val BITS = 12
            private const val HSIZE = 5003
        }

        private val initCodeSize = max(2, colorDepth)
        private var remaining = 0
        private var curPixel = 0

        private var nBits = 0
        private val maxBits = BITS
        private var maxCode = 0
        private val maxMaxCode = 1 shl BITS

        private val htab = IntArray(HSIZE)
        private val codetab = IntArray(HSIZE)
        private var freeEnt = 0
        private var clearFlg = false
        private var gInitBits = 0
        private var clearCode = 0
        private var eofCode = 0

        private var curAccum = 0
        private var curBits = 0
        private val masks = intArrayOf(
            0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F, 0x00FF,
            0x01FF, 0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF
        )
        private var aCount = 0
        private val accum = ByteArray(256)

        fun encode(os: OutputStream) {
            os.write(initCodeSize)
            remaining = imgW * imgH
            curPixel = 0
            compress(initCodeSize + 1, os)
            os.write(0)
        }

        private fun nextPixel(): Int {
            if (remaining == 0) return EOF
            remaining--
            return pixAry[curPixel++].toInt() and 0xFF
        }

        private fun compress(initBits: Int, outs: OutputStream) {
            gInitBits = initBits
            clearFlg = false
            nBits = gInitBits
            maxCode = (1 shl nBits) - 1
            clearCode = 1 shl (initBits - 1)
            eofCode = clearCode + 1
            freeEnt = clearCode + 2
            aCount = 0

            var ent = nextPixel()
            var hshift = 0
            var fcode = HSIZE
            while (fcode < 65536) {
                hshift++
                fcode *= 2
            }
            hshift = 8 - hshift
            clHash(HSIZE)
            output(clearCode, outs)

            var c: Int
            outer@ while (nextPixel().also { c = it } != EOF) {
                fcode = (c shl maxBits) + ent
                var i = (c shl hshift) xor ent
                if (htab[i] == fcode) {
                    ent = codetab[i]
                    continue
                } else if (htab[i] >= 0) {
                    var disp = HSIZE - i
                    if (i == 0) disp = 1
                    do {
                        i -= disp
                        if (i < 0) i += HSIZE
                        if (htab[i] == fcode) {
                            ent = codetab[i]
                            continue@outer
                        }
                    } while (htab[i] >= 0)
                }
                output(ent, outs)
                ent = c
                if (freeEnt < maxMaxCode) {
                    codetab[i] = freeEnt++
                    htab[i] = fcode
                } else {
                    clHash(HSIZE)
                    freeEnt = clearCode + 2
                    clearFlg = true
                    output(clearCode, outs)
                }
            }
            output(ent, outs)
            output(eofCode, outs)
        }

        private fun output(code: Int, outs: OutputStream) {
            curAccum = curAccum and masks[curBits]
            if (curBits > 0) {
                curAccum = curAccum or (code shl curBits)
            } else {
                curAccum = code
            }
            curBits += nBits
            while (curBits >= 8) {
                charOut((curAccum and 0xFF).toByte(), outs)
                curAccum = curAccum shr 8
                curBits -= 8
            }
            if (freeEnt > maxCode || clearFlg) {
                if (clearFlg) {
                    nBits = gInitBits
                    maxCode = (1 shl nBits) - 1
                    clearFlg = false
                } else {
                    nBits++
                    maxCode = if (nBits == maxBits) maxMaxCode else (1 shl nBits) - 1
                }
            }
            if (code == eofCode) {
                while (curBits > 0) {
                    charOut((curAccum and 0xFF).toByte(), outs)
                    curAccum = curAccum shr 8
                    curBits -= 8
                }
                flushChar(outs)
            }
        }

        private fun clHash(hsize: Int) {
            for (i in 0 until hsize) htab[i] = -1
        }

        private fun charOut(c: Byte, outs: OutputStream) {
            accum[aCount++] = c
            if (aCount >= 254) flushChar(outs)
        }

        private fun flushChar(outs: OutputStream) {
            if (aCount > 0) {
                outs.write(aCount)
                outs.write(accum, 0, aCount)
                aCount = 0
            }
        }
    }
}
