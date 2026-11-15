package com.yuranime.dinz.ui.profile.subscreens

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yuranime.dinz.BuildConfig
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.local.AppPreferences
import com.yuranime.dinz.data.local.AppStorageStats
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.measureTimeMillis

private val SettingsCanvasBg = Color(0xFF0B0C10)
private val SettingsCardBg = Color(0xFF11131A)
private val SettingsCardBorder = Color(0xFF1E222E)
private val SettingsDividerColor = Color(0xFF1C202B)
private val SettingsSectionLabel = Color(0xFF64748B)
private val SettingsMutedText = Color(0xFF94A3B8)
private val SettingsTitleText = Color(0xFFF8FAFC)

private val AnimeQualityOptions = listOf("Auto", "360p", "480p", "720p", "1080p")
private val PlaybackSpeedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onCheckUpdateClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val currentProfile by FirebaseManager.userProfile.collectAsState()
    val isSignedIn = remember(currentProfile) {
        currentProfile != null || !FirebaseManager.getActiveUid().isNullOrBlank()
    }

    // Pemutaran states
    val defaultQuality by AppPreferences.defaultAnimeQuality.collectAsState()
    val defaultSpeed by AppPreferences.defaultPlaybackSpeed.collectAsState()
    val autoPlayEnabled by AppPreferences.autoPlayEnabled.collectAsState()
    val autoSkipIntro by AppPreferences.autoSkipIntro.collectAsState()
    val preloadNextEpisode by AppPreferences.preloadNextEpisode.collectAsState()

    // Tampilan states
    val showMiniTitlePref by AppPreferences.showMiniTitle.collectAsState()
    val showCustomTitlePref by AppPreferences.showCustomTitle.collectAsState()
    val showRolePref by AppPreferences.showRole.collectAsState()
    val liquidGlassEnabled by AppPreferences.liquidGlassEnabled.collectAsState()
    val cosmeticAnimatePref by AppPreferences.cosmeticAnimate.collectAsState()

    val showMiniTitle = currentProfile?.showMiniTitle ?: showMiniTitlePref
    val showCustomTitle = currentProfile?.showCustomTitle ?: showCustomTitlePref
    val showRole = currentProfile?.showRole ?: showRolePref
    val cosmeticAnimate = currentProfile?.cosmeticAnimate ?: cosmeticAnimatePref

    // Storage stats (real StatFs + downloads + app cache)
    var storageStats by remember {
        mutableStateOf(
            AppStorageStats(
                totalDeviceBytes = 0L,
                availableDeviceBytes = 0L,
                downloadBytes = 0L,
                downloadItemCount = 0,
                cacheBytes = 0L
            )
        )
    }
    var isClearingCache by remember { mutableStateOf(false) }

    fun refreshStorageStats() {
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                AppPreferences.readAppStorageStats(context)
            }
            storageStats = loaded
        }
    }

    LaunchedEffect(Unit) {
        refreshStorageStats()
    }

    // Real-time server ping latency measurement
    var serverPingMs by remember { mutableStateOf<Long?>(null) }
    var isMeasuringPing by remember { mutableStateOf(true) }
    var pingRefreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(pingRefreshTrigger) {
        while (isActive) {
            val latency = withContext(Dispatchers.IO) {
                measureServerPingMs("https://xyz-api.animein.net/")
            }
            serverPingMs = latency
            isMeasuringPing = false
            delay(5000L)
        }
    }

    // Sheets & Dialogs
    var showQualitySheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPrivacyFaqDialog by remember { mutableStateOf(false) }

    val showCompactHeaderTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 96
        }
    }

    val deviceDisplayName = remember {
        val brand = Build.MANUFACTURER.orEmpty()
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val model = Build.MODEL.orEmpty().trim()
        if (brand.isNotBlank() && model.startsWith(brand, ignoreCase = true)) {
            model
        } else {
            "$brand $model".trim().ifBlank { "Perangkat Android" }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsCanvasBg)
    ) {
        // Soft top ambient radial glow matching reference depth without clipping text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x1E6366F1),
                            Color(0x0EE11D48),
                            Color.Transparent
                        ),
                        center = Offset(720f, 60f),
                        radius = 780f
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Solid non-colliding top bar containing ONLY back button (+ collapsed nav title when scrolled)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SettingsCanvasBg.copy(alpha = if (showCompactHeaderTitle) 0.98f else 0f))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF171A24))
                            .border(1.dp, Color(0xFF232734), CircleShape)
                            .clickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = showCompactHeaderTitle,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Pengaturan",
                                color = SettingsTitleText,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }

                if (showCompactHeaderTitle) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = SettingsDividerColor
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header item inside LazyColumn so PENGATURAN scrolls cleanly and never collides with items
                item(key = "settings_hero_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = "PENGATURAN",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = SfProFontFamily,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "Sesuaikan pengalaman menonton dan aplikasi.",
                            color = SettingsMutedText,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                // SEKSI 1: PEMUTARAN
                item(key = "section_pemutaran") {
                    SettingsSectionContainer(
                        icon = Icons.Outlined.PlayCircle,
                        title = "PEMUTARAN"
                    ) {
                        SettingsValueRow(
                            icon = Icons.Rounded.Hd,
                            iconTint = Color(0xFFA855F7),
                            title = "Kualitas Anime",
                            value = defaultQuality,
                            showChevron = true,
                            onClick = { showQualitySheet = true }
                        )
                        SettingsGroupDivider()
                        SettingsValueRow(
                            icon = Icons.Rounded.Speed,
                            iconTint = Color(0xFFF59E0B),
                            title = "Kecepatan Putar",
                            value = "${formatSpeedValue(defaultSpeed)}x",
                            showChevron = true,
                            onClick = { showSpeedSheet = true }
                        )
                        SettingsGroupDivider()
                        SettingsToggleRow(
                            icon = Icons.Rounded.PlayArrow,
                            iconTint = Color(0xFF22C55E),
                            title = "Putar Otomatis",
                            checked = autoPlayEnabled,
                            onCheckedChange = { AppPreferences.setAutoPlayEnabled(context, it) }
                        )
                        SettingsGroupDivider()
                        SettingsToggleRow(
                            icon = Icons.Rounded.FastForward,
                            iconTint = Color(0xFF8B5CF6),
                            title = "Lewati Intro Otomatis",
                            checked = autoSkipIntro,
                            onCheckedChange = { AppPreferences.setAutoSkipIntro(context, it) }
                        )
                        SettingsGroupDivider()
                        SettingsToggleRow(
                            icon = Icons.Rounded.FileDownload,
                            iconTint = Color(0xFF38BDF8),
                            title = "Pramuat Episode Berikutnya",
                            checked = preloadNextEpisode,
                            onCheckedChange = { AppPreferences.setPreloadNextEpisode(context, it) }
                        )
                    }
                }

                // SEKSI 2: PENYIMPANAN
                item(key = "section_penyimpanan") {
                    val downloadSummary = remember(storageStats) {
                        val sizeStr = AppPreferences.formatByteSize(storageStats.downloadBytes)
                        "${storageStats.downloadItemCount} Item • $sizeStr"
                    }
                    val freeSpaceLabel = remember(storageStats) {
                        if (storageStats.totalDeviceBytes > 0L) {
                            val free = AppPreferences.formatByteSize(storageStats.availableDeviceBytes)
                            val total = AppPreferences.formatByteSize(storageStats.totalDeviceBytes)
                            "Sisa $free dari $total"
                        } else {
                            "Memeriksa penyimpanan..."
                        }
                    }
                    val cacheSizeText = remember(storageStats.cacheBytes, isClearingCache) {
                        if (isClearingCache) {
                            "Membersihkan..."
                        } else {
                            AppPreferences.formatByteSize(storageStats.cacheBytes)
                        }
                    }

                    SettingsSectionContainer(
                        icon = Icons.Rounded.Storage,
                        title = "PENYIMPANAN"
                    ) {
                        SettingsValueRow(
                            icon = Icons.Rounded.FileDownload,
                            iconTint = Color(0xFF8B5CF6),
                            title = "Unduhan",
                            subtitle = freeSpaceLabel,
                            value = downloadSummary,
                            showChevron = true,
                            onClick = onNavigateToDownloads
                        )
                        SettingsGroupDivider()
                        SettingsValueRow(
                            icon = Icons.Rounded.Cached,
                            iconTint = Color(0xFFA855F7),
                            title = "Bersihkan Cache",
                            value = cacheSizeText,
                            valueColor = Color(0xFF818CF8),
                            showChevron = false,
                            onClick = {
                                if (!isClearingCache) {
                                    isClearingCache = true
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            AppPreferences.clearImageCache(context)
                                        }
                                        val updated = withContext(Dispatchers.IO) {
                                            AppPreferences.readAppStorageStats(context)
                                        }
                                        storageStats = updated
                                        isClearingCache = false
                                        Toast.makeText(
                                            context,
                                            "Cache dibersihkan (${AppPreferences.formatByteSize(updated.cacheBytes)})",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                }

                // SEKSI 3: TAMPILAN
                item(key = "section_tampilan") {
                    SettingsSectionContainer(
                        icon = Icons.Rounded.Settings,
                        title = "TAMPILAN"
                    ) {
                        SettingsToggleRow(
                            icon = Icons.Outlined.Diamond,
                            iconTint = Color(0xFF22C55E),
                            title = "Tampilkan Mini-Title",
                            checked = showMiniTitle,
                            onCheckedChange = { enabled ->
                                FirebaseManager.setDisplayVisibility(
                                    context = context,
                                    showMiniTitle = enabled
                                )
                            }
                        )
                        SettingsGroupDivider()
                        SettingsToggleRow(
                            icon = Icons.Outlined.EmojiEvents,
                            iconTint = Color(0xFFF59E0B),
                            title = "Tampilkan Custom Title",
                            checked = showCustomTitle,
                            onCheckedChange = { enabled ->
                                FirebaseManager.setDisplayVisibility(
                                    context = context,
                                    showCustomTitle = enabled
                                )
                            }
                        )
                        SettingsGroupDivider()
                        SettingsToggleRow(
                            icon = Icons.Rounded.Shield,
                            iconTint = Color(0xFF38BDF8),
                            title = "Tampilkan Role & Lencana",
                            checked = showRole,
                            onCheckedChange = { enabled ->
                                FirebaseManager.setDisplayVisibility(
                                    context = context,
                                    showRole = enabled
                                )
                            }
                        )
                        SettingsGroupDivider()
                        SettingsToggleRow(
                            icon = Icons.Rounded.AutoAwesome,
                            iconTint = Color(0xFF22D3EE),
                            title = "Efek Liquid Glass Navbar",
                            checked = liquidGlassEnabled,
                            onCheckedChange = { AppPreferences.setLiquidGlassEnabled(context, it) }
                        )
                        SettingsGroupDivider()
                        SettingsToggleRow(
                            icon = Icons.Rounded.Star,
                            iconTint = Color(0xFFF43F5E),
                            title = "Animasi Kosmetik & Efek",
                            checked = cosmeticAnimate,
                            onCheckedChange = { enabled ->
                                FirebaseManager.setCosmeticAnimate(context, enabled)
                            }
                        )
                    }
                }

                // SEKSI 4: TENTANG
                item(key = "section_tentang") {
                    val pingText = when {
                        isMeasuringPing -> "Mengukur..."
                        serverPingMs == null -> "Offline"
                        else -> "$serverPingMs ms"
                    }
                    val pingColor = when {
                        isMeasuringPing -> SettingsMutedText
                        serverPingMs == null -> Color(0xFFF43F5E)
                        serverPingMs!! < 300L -> Color(0xFF22C55E)
                        serverPingMs!! <= 800L -> Color(0xFFFACC15)
                        else -> Color(0xFFF43F5E)
                    }

                    SettingsSectionContainer(
                        icon = Icons.Outlined.Info,
                        title = "TENTANG"
                    ) {
                        SettingsValueRow(
                            icon = Icons.Rounded.Info,
                            iconTint = Color(0xFFA855F7),
                            title = "Versi Aplikasi",
                            value = "${BuildConfig.VERSION_NAME} • ${BuildConfig.VERSION_CODE}",
                            showChevron = false,
                            onClick = onCheckUpdateClick
                        )
                        SettingsGroupDivider()
                        SettingsValueRow(
                            icon = Icons.Rounded.Code,
                            iconTint = Color(0xFF38BDF8),
                            title = "ID Paket",
                            value = BuildConfig.APPLICATION_ID,
                            showChevron = false
                        )
                        SettingsGroupDivider()
                        SettingsValueRow(
                            icon = Icons.Rounded.Android,
                            iconTint = Color(0xFF22C55E),
                            title = "Sistem",
                            value = "Android ${Build.VERSION.RELEASE} • API ${Build.VERSION.SDK_INT}",
                            showChevron = false
                        )
                        SettingsGroupDivider()
                        SettingsValueRow(
                            icon = Icons.Rounded.Smartphone,
                            iconTint = Color(0xFFF59E0B),
                            title = "Perangkat",
                            value = deviceDisplayName,
                            showChevron = false
                        )
                        SettingsGroupDivider()
                        SettingsPingRow(
                            icon = Icons.Rounded.Dns,
                            iconTint = Color(0xFF22C55E),
                            title = "Status Server",
                            pingText = pingText,
                            pingColor = pingColor,
                            onClick = {
                                isMeasuringPing = true
                                pingRefreshTrigger++
                            }
                        )
                        SettingsGroupDivider()
                        SettingsValueRow(
                            icon = Icons.Rounded.Security,
                            iconTint = Color(0xFFFACC15),
                            title = "Privasi & FAQ",
                            value = "Buka",
                            showChevron = true,
                            onClick = { showPrivacyFaqDialog = true }
                        )
                    }
                }

                // SEKSI 5: SESI
                item(key = "section_sesi") {
                    SettingsSectionContainer(
                        icon = Icons.Rounded.Person,
                        title = "SESI"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSignedIn) {
                                        onSignOutClick()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Belum ada akun yang masuk",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBadge(
                                icon = Icons.AutoMirrored.Rounded.Logout,
                                tint = Color(0xFFF43F5E)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Keluar dari Akun",
                                color = SettingsTitleText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = SfProFontFamily,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (isSignedIn) "Keluar" else "Tamu",
                                color = Color(0xFFF43F5E),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }

                // FOOTER BRANDING
                item(key = "settings_footer_brand") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 6.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Y U R A N I M E",
                            color = SettingsSectionLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dibuat dengan presisi",
                            color = Color(0xFF475569),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheet Pilih Kualitas Anime Default
    if (showQualitySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
            sheetState = sheetState,
            containerColor = SettingsCardBg,
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Kualitas Anime Default",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Player otomatis memilih resolusi ini saat membuka episode.",
                    color = SettingsMutedText,
                    fontSize = 12.5.sp,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF161923))
                        .border(1.dp, SettingsCardBorder, RoundedCornerShape(16.dp))
                ) {
                    AnimeQualityOptions.forEachIndexed { index, option ->
                        val isSelected = defaultQuality.equals(option, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppPreferences.setDefaultAnimeQuality(context, option)
                                    showQualitySheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                color = if (isSelected) Color(0xFF22C55E) else SettingsTitleText,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                fontFamily = SfProFontFamily,
                                modifier = Modifier.weight(1f)
                            )
                            if (option == "Auto") {
                                Text(
                                    text = "Disarankan",
                                    color = SettingsSectionLabel,
                                    fontSize = 12.sp,
                                    fontFamily = SfProFontFamily
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Terpilih",
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < AnimeQualityOptions.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = SettingsDividerColor,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Bottom Sheet Pilih Kecepatan Putar Default
    if (showSpeedSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            sheetState = sheetState,
            containerColor = SettingsCardBg,
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Kecepatan Putar Default",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Kecepatan playback awal saat episode anime dimulai.",
                    color = SettingsMutedText,
                    fontSize = 12.5.sp,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF161923))
                        .border(1.dp, SettingsCardBorder, RoundedCornerShape(16.dp))
                ) {
                    PlaybackSpeedOptions.forEachIndexed { index, speed ->
                        val isSelected = kotlin.math.abs(defaultSpeed - speed) < 0.01f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppPreferences.setDefaultPlaybackSpeed(context, speed)
                                    showSpeedSheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatSpeedValue(speed)}x",
                                color = if (isSelected) Color(0xFFF59E0B) else SettingsTitleText,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                fontFamily = SfProFontFamily,
                                modifier = Modifier.weight(1f)
                            )
                            if (speed == 1.0f) {
                                Text(
                                    text = "Normal",
                                    color = SettingsSectionLabel,
                                    fontSize = 12.sp,
                                    fontFamily = SfProFontFamily
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Terpilih",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < PlaybackSpeedOptions.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = SettingsDividerColor,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Dialog Privasi & FAQ
    if (showPrivacyFaqDialog) {
        PrivacyFaqDialog(
            onDismiss = { showPrivacyFaqDialog = false },
            onCheckUpdateClick = {
                showPrivacyFaqDialog = false
                onCheckUpdateClick()
            }
        )
    }
}

@Composable
private fun SettingsSectionContainer(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 6.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SettingsSectionLabel,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = SettingsSectionLabel,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                fontFamily = SfProFontFamily
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SettingsCardBg)
                .border(1.dp, SettingsCardBorder, RoundedCornerShape(20.dp))
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsGroupDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = SettingsDividerColor,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsValueRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String? = null,
    valueColor: Color = SettingsMutedText,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBadge(icon = icon, tint = iconTint)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SettingsTitleText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = SettingsSectionLabel,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = SfProFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (showChevron) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = SettingsSectionLabel,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun SettingsPingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    pingText: String,
    pingColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBadge(icon = icon, tint = iconTint)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = SettingsTitleText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = SfProFontFamily,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(pingColor)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = pingText,
                color = pingColor,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SfProFontFamily
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBadge(icon = icon, tint = iconTint)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = SettingsTitleText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = SfProFontFamily,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(12.dp))
        IosToggleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun IosToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF22C55E) else Color(0xFF262A36),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "switch_track"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "switch_thumb"
    )

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun PrivacyFaqDialog(
    onDismiss: () -> Unit,
    onCheckUpdateClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SettingsCardBg)
                .border(1.dp, SettingsCardBorder, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIconBadge(
                    icon = Icons.Rounded.Security,
                    tint = Color(0xFFFACC15)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Privasi & FAQ Yuranime",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1D27))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup",
                        tint = SettingsMutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 0.5.dp, color = SettingsDividerColor)
            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FaqItem(
                    question = "Data apa saja yang disimpan Yuranime?",
                    answer = "Profil, progres EXP, gaya nama, lencana, dan preferensi visibilitas disimpan aman melalui Firebase Firestore agar tersinkronisasi lintas sesi. Unduhan komik offline dan cache media disimpan hanya secara lokal di penyimpanan internal perangkat."
                )
                FaqItem(
                    question = "Bagaimana cara menyembunyikan Title atau Role?",
                    answer = "Aktifkan atau matikan toggle Tampilkan Mini-Title, Tampilkan Custom Title, dan Tampilkan Role & Lencana pada seksi Tampilan. Perubahan langsung tersinkronisasi ke Profil, Preview Profil, dan Global Chat."
                )
                FaqItem(
                    question = "Apa kegunaan Pramuat Episode & Lewati Intro?",
                    answer = "Pramuat Episode Berikutnya menyiapkan stream episode selanjutnya di latar belakang sehingga perpindahan episode instan. Lewati Intro Otomatis melompati segmen opening saat memutar episode penuh."
                )
                FaqItem(
                    question = "Apakah Bersihkan Cache menghapus unduhan offline?",
                    answer = "Tidak. Bersihkan Cache hanya menghapus berkas sementara gambar Coil, stiker, dan buffer sementara di cacheDir tanpa menyentuh chapter komik offline maupun riwayat akun."
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1D27))
                        .border(1.dp, SettingsCardBorder, RoundedCornerShape(12.dp))
                        .clickable(onClick = onCheckUpdateClick)
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cek Pembaruan",
                        color = SettingsTitleText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily,
                        textAlign = TextAlign.Center
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE11D48))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mengerti",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqItem(
    question: String,
    answer: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161923))
            .border(0.7.dp, SettingsCardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            text = question,
            color = SettingsTitleText,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = answer,
            color = SettingsMutedText,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontFamily = SfProFontFamily
        )
    }
}

private fun formatSpeedValue(speed: Float): String {
    return when {
        kotlin.math.abs(speed - 1.0f) < 0.01f -> "1.0"
        kotlin.math.abs(speed - 0.5f) < 0.01f -> "0.5"
        kotlin.math.abs(speed - 0.75f) < 0.01f -> "0.75"
        kotlin.math.abs(speed - 1.25f) < 0.01f -> "1.25"
        kotlin.math.abs(speed - 1.5f) < 0.01f -> "1.5"
        kotlin.math.abs(speed - 2.0f) < 0.01f -> "2.0"
        else -> speed.toString()
    }
}

private fun measureServerPingMs(targetUrl: String): Long? {
    return try {
        var elapsed = -1L
        val connection = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            connectTimeout = 3500
            readTimeout = 3500
            instanceFollowRedirects = true
        }
        elapsed = measureTimeMillis {
            try {
                connection.connect()
                connection.responseCode
            } finally {
                connection.disconnect()
            }
        }
        elapsed.coerceAtLeast(1L)
    } catch (_: Exception) {
        null
    }
}
