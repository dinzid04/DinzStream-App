package com.yuranime.dinz.ui.profile

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.yuranime.dinz.BuildConfig
import com.yuranime.dinz.R
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.TitleUnlockType
import com.yuranime.dinz.data.firebase.UserProfile
import com.yuranime.dinz.data.local.AppPreferences
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.ui.components.BottomNavBar
import com.yuranime.dinz.data.repository.DiscordCosmeticsRepository
import com.yuranime.dinz.ui.components.AnimatedCosmeticImage
import com.yuranime.dinz.ui.components.EmptyTitleSlotBadge
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.components.NavTab
import com.yuranime.dinz.ui.components.PrimoPointIcon
import com.yuranime.dinz.ui.components.StatusMicroTitleTagRow
import com.yuranime.dinz.ui.components.liquidGlassBackdrop
import com.yuranime.dinz.ui.leaderboard.LeaderboardScreen
import com.yuranime.dinz.ui.profile.subscreens.DownloadsScreen
import com.yuranime.dinz.ui.profile.subscreens.EditProfileScreen
import com.yuranime.dinz.ui.profile.subscreens.PointsRewardsScreen
import com.yuranime.dinz.ui.profile.subscreens.SettingsScreen
import com.yuranime.dinz.ui.profile.subscreens.ShopScreen
import com.yuranime.dinz.ui.theme.JakartaSansFontFamily
import com.yuranime.dinz.ui.theme.JetBrainsMonoFontFamily
import com.yuranime.dinz.ui.theme.OutfitDisplayFontFamily
import com.yuranime.dinz.ui.theme.SfProFontFamily
import com.yuranime.dinz.ui.theme.YuranimeLogoFontFamily

enum class ProfileSubScreen {
    MAIN,
    EDIT_PROFILE,
    LEADERBOARD,
    POINTS_REWARDS,
    SHOP,
    DOWNLOADS,
    COLLECTION_HISTORY,
    SETTINGS
}

enum class TrailingType {
    NONE,
    CHEVRON,
    BADGE_TOP_100,
    BADGE_COINS
}

@Composable
fun LoginProfileScreen(
    modifier: Modifier = Modifier,
    selectedTab: NavTab = NavTab.PROFILE,
    onTabSelected: (NavTab) -> Unit = {},
    onAnimeClick: (id: String) -> Unit = {},
    onEpisodeClick: (episodeId: String, movieId: String) -> Unit = { _, _ -> },
    onCheckUpdateClick: () -> Unit = {},
    onComicChapterClick: ((chapterId: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val authDb = remember { LocalAuthDatabase(context) }

    var currentSubScreen by remember { mutableStateOf(ProfileSubScreen.MAIN) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showTitlePickerDialog by remember { mutableStateOf(false) }
    val isLiquidGlass by AppPreferences.liquidGlassEnabled.collectAsState()

    LaunchedEffect(Unit) {
        FirebaseManager.initSession(context)
    }

    val currentUser by FirebaseManager.currentUser.collectAsState()
    val googleSession by FirebaseManager.googleSession.collectAsState()
    val isUserLoggedIn = currentUser != null || googleSession != null
    val userProfile by FirebaseManager.userProfile.collectAsState()
    val leaderboardUsers by FirebaseManager.leaderboard.collectAsState()

    val userRankNumber = remember(leaderboardUsers, userProfile?.uid) {
        val activeUid = userProfile?.uid ?: FirebaseManager.getCurrentUid()
        val foundIndex = leaderboardUsers.indexOfFirst { it.uid == activeUid }
        if (foundIndex >= 0) foundIndex + 1 else null
    }

    // Google Sign-In Client standar Android
    val webClientId = remember(context) {
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else FirebaseManager.WEB_CLIENT_ID
        } catch (e: Exception) {
            FirebaseManager.WEB_CLIENT_ID
        }
    }
    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember(context) {
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                isLoggingIn = true
                val googleId = account.id ?: account.email.orEmpty().hashCode().toString().replace("-", "x")
                val email = account.email.orEmpty().trim().lowercase()
                val displayName = account.displayName?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore("@").split(".", "_", "-")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                        .ifBlank { "Pengguna Google" }
                val photoUrl = account.photoUrl?.toString()

                AppPreferences.saveGoogleSession(context, googleId, displayName, email, photoUrl)
                authDb.setLoggedIn(true, displayName)
                if (!photoUrl.isNullOrBlank()) {
                    authDb.setAvatarUrl(photoUrl)
                }

                FirebaseManager.onGoogleAccountSelected(
                    uid = googleId,
                    name = displayName,
                    email = email,
                    avatar = photoUrl,
                    idToken = account.idToken,
                    context = context
                )

                isLoggingIn = false
                Toast.makeText(context, "Selamat datang, $displayName!", Toast.LENGTH_SHORT).show()
            } else {
                isLoggingIn = false
            }
        } catch (e: Exception) {
            isLoggingIn = false
        }
    }

    LaunchedEffect(isUserLoggedIn) {
        if (isUserLoggedIn) {
            FirebaseManager.startListeningCurrentUser(context)
        }
    }

    BackHandler(enabled = currentSubScreen != ProfileSubScreen.MAIN) {
        currentSubScreen = ProfileSubScreen.MAIN
    }

    if (showInfoDialog) {
        AppInfoModal(onDismiss = { showInfoDialog = false })
    }

    if (showTitlePickerDialog) {
        QuickTitlePickerDialog(
            profile = userProfile,
            onDismiss = { showTitlePickerDialog = false },
            onOpenPointsRewards = {
                showTitlePickerDialog = false
                currentSubScreen = ProfileSubScreen.POINTS_REWARDS
            }
        )
    }

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
        label = "ProfileSubScreenAnim"
    ) { subScreen ->
        when (subScreen) {
            ProfileSubScreen.EDIT_PROFILE -> {
                EditProfileScreen(
                    authDb = authDb,
                    onBackClick = { currentSubScreen = ProfileSubScreen.MAIN }
                )
            }
            ProfileSubScreen.LEADERBOARD -> {
                LeaderboardScreen(
                    onBackClick = { currentSubScreen = ProfileSubScreen.MAIN }
                )
            }
            ProfileSubScreen.POINTS_REWARDS -> {
                PointsRewardsScreen(
                    authDb = authDb,
                    onBackClick = { currentSubScreen = ProfileSubScreen.MAIN }
                )
            }
            ProfileSubScreen.SHOP -> {
                ShopScreen(
                    onBack = { currentSubScreen = ProfileSubScreen.MAIN },
                    onNavigateToRewards = { currentSubScreen = ProfileSubScreen.POINTS_REWARDS }
                )
            }
            ProfileSubScreen.DOWNLOADS -> {
                DownloadsScreen(
                    onBackClick = { currentSubScreen = ProfileSubScreen.MAIN },
                    onReadComicOffline = { offlineComic ->
                        onComicChapterClick?.invoke(offlineComic.chapterId)
                    }
                )
            }
            ProfileSubScreen.COLLECTION_HISTORY -> {
                CollectionAndHistorySubScreen(
                    authDb = authDb,
                    onBackClick = { currentSubScreen = ProfileSubScreen.MAIN },
                    onEpisodeClick = onEpisodeClick,
                    onComicChapterClick = onComicChapterClick
                )
            }
            ProfileSubScreen.SETTINGS -> {
                SettingsScreen(
                    onBackClick = { currentSubScreen = ProfileSubScreen.MAIN },
                    onCheckUpdateClick = onCheckUpdateClick
                )
            }
            ProfileSubScreen.MAIN -> {
                if (!isUserLoggedIn) {
                    LoginView(
                        isLoggingIn = isLoggingIn,
                        onBackToHomeClick = { onTabSelected(NavTab.HOME) },
                        onSettingsClick = { currentSubScreen = ProfileSubScreen.SETTINGS },
                        onGoogleLoginClick = {
                            isLoggingIn = true
                            try {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    val signInIntent = googleSignInClient.signInIntent
                                    googleSignInLauncher.launch(signInIntent)
                                }
                            } catch (e: Exception) {
                                isLoggingIn = false
                            }
                        },
                        onEmailLoginClick = {
                            isLoggingIn = true
                            try {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    val signInIntent = googleSignInClient.signInIntent
                                    googleSignInLauncher.launch(signInIntent)
                                }
                            } catch (e: Exception) {
                                isLoggingIn = false
                            }
                        },
                        onGuestLoginClick = {
                            onTabSelected(NavTab.HOME)
                        }
                    )
                } else {
                    val backdropLayer = rememberGraphicsLayer()
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .background(Color(0xFF0B0B0B))
                    ) {
                        UserProfileView(
                            profile = userProfile,
                            authDb = authDb,
                            userRankNumber = userRankNumber,
                            modifier = Modifier
                                .fillMaxSize()
                                .liquidGlassBackdrop(backdropLayer, enabled = isLiquidGlass),
                            onEditProfileClick = { currentSubScreen = ProfileSubScreen.EDIT_PROFILE },
                            onOpenLeaderboardClick = { currentSubScreen = ProfileSubScreen.LEADERBOARD },
                            onOpenPointsRewardsClick = { currentSubScreen = ProfileSubScreen.POINTS_REWARDS },
                            onOpenShopClick = { currentSubScreen = ProfileSubScreen.SHOP },
                            onOpenTitleSlotClick = { showTitlePickerDialog = true },
                            onOpenDownloadsClick = { currentSubScreen = ProfileSubScreen.DOWNLOADS },
                            onOpenCollectionHistoryClick = { currentSubScreen = ProfileSubScreen.COLLECTION_HISTORY },
                            onOpenSettingsClick = { currentSubScreen = ProfileSubScreen.SETTINGS },
                            onOpenInfoClick = { showInfoDialog = true },
                            onSignOutClick = {
                                FirebaseManager.signOut(context)
                                authDb.setLoggedIn(false)
                                Toast.makeText(
                                    context,
                                    "Berhasil keluar dari akun",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        BottomNavBar(
                            selectedTab = selectedTab,
                            onTabSelected = onTabSelected,
                            backdropLayer = backdropLayer,
                            isLiquidGlassEnabled = isLiquidGlass,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}

/**
 * TAHAP 1: Redesign Halaman Profile (`ProfileScreen`)
 * Mengikuti struktur tata letak referensi `1002213252.jpg` (`WA_1794546770520.jpg`)
 * dengan palet Yuranime (Deep Obsidian `#0B0B0B` + Crimson `#E11D48` & Vibrant Orange `#FF5722`).
 */
@Composable
private fun UserProfileView(
    profile: UserProfile?,
    authDb: LocalAuthDatabase,
    userRankNumber: Int?,
    modifier: Modifier = Modifier,
    onEditProfileClick: () -> Unit,
    onOpenLeaderboardClick: () -> Unit,
    onOpenPointsRewardsClick: () -> Unit,
    onOpenShopClick: () -> Unit,
    onOpenTitleSlotClick: () -> Unit,
    onOpenDownloadsClick: () -> Unit,
    onOpenCollectionHistoryClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onOpenInfoClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val context = LocalContext.current
    var isPublicPreviewMode by remember { mutableStateOf(false) }

    val displayName = profile?.name?.takeIf { it.isNotBlank() } ?: authDb.getUsername()
    val userTag = profile?.userTag?.takeIf { it.isNotBlank() } ?: "1001"
    val email = profile?.email?.takeIf { it.isNotBlank() } ?: "Akun Yuranime Terverifikasi"
    val joinDate = profile?.joinDate?.takeIf { it.isNotBlank() } ?: "November 2026"
    val bio = profile?.bio?.takeIf { it.isNotBlank() }
        ?: "Penikmat anime & komik di Yuranime. Ketuk ikon pensil di kanan atas untuk menulis bio."
    val role = profile?.role?.takeIf { it.isNotBlank() } ?: "Member"
    val avatarUrl = profile?.avatar?.takeIf { it.isNotBlank() } ?: authDb.getAvatarUrl()
    val bannerUrl = profile?.banner?.takeIf { it.isNotBlank() } ?: authDb.getCoverUrl()
    val exp = profile?.exp ?: 0L
    val points = profile?.points ?: 0L
    val level = profile?.level ?: 1
    val rankTitle = profile?.rankTitle ?: FirebaseManager.computeRankTitle(level)
    val nextTargetExp = profile?.nextLevelTargetExp ?: FirebaseManager.computeNextLevelTargetExp(level)
    val expProgress = profile?.expProgress ?: FirebaseManager.computeExpProgress(exp, nextTargetExp)
    val equippedTitle = profile?.equippedTitle.orEmpty()
    val equippedTitleTier = profile?.equippedTitleTier ?: level
    val equippedTitleVariant = profile?.equippedTitleVariant ?: 0
    val equippedBorderUrl = profile?.equippedBorderUrl.orEmpty()
    val equippedProfileEffectUrl = profile?.equippedProfileEffectUrl.orEmpty()
    val cosmeticAnimate = profile?.cosmeticAnimate ?: true
    val resolvedBorderUrl = remember(equippedBorderUrl, cosmeticAnimate) {
        if (!cosmeticAnimate && equippedBorderUrl.contains("passthrough=true")) {
            equippedBorderUrl.replace("passthrough=true", "passthrough=false")
        } else {
            equippedBorderUrl
        }
    }

    val animatedExpProgress by animateFloatAsState(
        targetValue = expProgress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "profileExpSpring"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 125.dp)
    ) {
        // 1. HEADER BANNER + PROFILE EFFECT LOOPING 380.dp (TANPA CLIPPING) + AVATAR 84.dp & BORDER 102.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            // Layer Background Banner: 270.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .align(Alignment.TopCenter)
            ) {
                if (bannerUrl.isNotBlank()) {
                    AsyncImage(
                        model = bannerUrl,
                        contentDescription = "Profile Cover Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF2B0E16),
                                        Color(0xFF190E16),
                                        Color(0xFF0F111A)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(900f, 500f)
                                )
                            )
                    )
                }

                // Gradasi Bawah memudar lembut ke #0B0B0B
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0x220B0B0B),
                                    0.55f to Color(0x660B0B0B),
                                    0.88f to Color(0xD90B0B0B),
                                    1.0f to Color(0xFF0B0B0B)
                                )
                            )
                        )
                )

                // 3 Tombol Ikon Bulat Minimalis di Pojok Kanan Atas
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 12.dp, end = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopHeaderCircleIconButton(
                        icon = Icons.Outlined.Visibility,
                        contentDescription = "Preview Profil Publik",
                        isActive = isPublicPreviewMode,
                        onClick = {
                            isPublicPreviewMode = !isPublicPreviewMode
                            val msg = if (isPublicPreviewMode) {
                                "Menampilkan pratinjau profil publik"
                            } else {
                                "Kembali ke mode profil pribadi"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    )

                    TopHeaderCircleIconButton(
                        icon = Icons.Outlined.Edit,
                        contentDescription = "Edit Profil",
                        onClick = onEditProfileClick
                    )

                    TopHeaderCircleIconButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = "Pengaturan",
                        onClick = onOpenSettingsClick
                    )
                }
            }

            // Profile Effect Looping APNG/WebP 60 FPS (Fills 380.dp Box TANPA Clipping)
            if (equippedProfileEffectUrl.isNotBlank()) {
                AnimatedCosmeticImage(
                    url = equippedProfileEffectUrl,
                    isAnimated = cosmeticAnimate,
                    contentDescription = "Profile Effect",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Layer Avatar: Bulat 84.dp + Border 102.dp (TANPA Cincin EXP!)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 8.dp)
                    .size(102.dp),
                contentAlignment = Alignment.Center
            ) {
                // Layer Foto Profil Bulat: 84.dp
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161820))
                        .clickable { if (!isPublicPreviewMode) onEditProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Foto Profil $displayName",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = displayName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                // Layer Border Avatar Asli (discord_assets): 102.dp tepat konsentris di Alignment.Center
                if (resolvedBorderUrl.isNotBlank()) {
                    AnimatedCosmeticImage(
                        url = DiscordCosmeticsRepository.getOptimizedUrl(resolvedBorderUrl, size = 240, animated = cosmeticAnimate),
                        isAnimated = cosmeticAnimate,
                        contentDescription = "Avatar Border",
                        modifier = Modifier.size(102.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = isPublicPreviewMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161820))
                    .clickable { isPublicPreviewMode = false }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mode Pratinjau Profil Publik",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                }
                Text(
                    text = "Tutup",
                    color = Color(0xFFE11D48),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }

        // 2. IDENTITAS PENGGUNA BERSIH (Rata Kiri, Ikon Outline Mail & Calendar, Tanpa Kotak Overlay)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp)
        ) {
            // Baris 1: Username (SF Pro Bold 24.sp) + #ID unik (SF Pro 15.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "#$userTag",
                    color = Color(0xFF64748B),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily,
                    modifier = Modifier.alignByBaseline()
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Baris 2: Email dengan Ikon Outline Envelope
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = email,
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Baris 3: Tanggal Bergabung dengan Ikon Outline Calendar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bergabung sejak $joinDate",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Baris 4: Bio Pengguna
            Text(
                text = bio,
                color = Color(0xFFE2E8F0),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = SfProFontFamily,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Baris 5: Status Row (MEMBER | LV. X | RANK TITLE) - Bersih Tanpa Polygon/Card Box Overlay
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = role.uppercase(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
                VerticalDivider(
                    modifier = Modifier.height(12.dp),
                    thickness = 1.dp,
                    color = Color(0xFF262934)
                )
                Spacer(modifier = Modifier.width(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardDoubleArrowUp,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "LV. $level",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
                VerticalDivider(
                    modifier = Modifier.height(12.dp),
                    thickness = 1.dp,
                    color = Color(0xFF262934)
                )
                Spacer(modifier = Modifier.width(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Diamond,
                        contentDescription = null,
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = rankTitle.uppercase(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Baris 6: Slot Custom Title Badge
            if (equippedTitle.isNotBlank()) {
                GeometricTitleBadge(
                    titleName = equippedTitle,
                    tierLevel = equippedTitleTier,
                    styleVariant = equippedTitleVariant,
                    onClick = { if (!isPublicPreviewMode) onOpenTitleSlotClick() }
                )
            } else {
                EmptyTitleSlotBadge(
                    onClick = { if (!isPublicPreviewMode) onOpenTitleSlotClick() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Baris 7: Full-Width EXP Bar Sans-Serif Bold (NO slashed zero!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXP",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Text(
                    text = "${FirebaseManager.formatNumber(exp)} / ${FirebaseManager.formatNumber(nextTargetExp)}",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1C1F2A))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedExpProgress.coerceAtLeast(if (exp > 0L) 0.03f else 0f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF5722), Color(0xFFE11D48))
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. 2-KOLOM DIVIDER PAPAN PERINGKAT & POINTS & REWARDS (Dibatasi Divider 0.5.dp #1E222B, Tanpa Card Box)
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            thickness = 0.5.dp,
            color = Color(0xFF1E222B)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolom Kiri: Papan Peringkat
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenLeaderboardClick() }
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = "Papan Peringkat",
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Papan Peringkat",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (userRankNumber != null) "Rank #$userRankNumber • TOP 100" else "TOP 100 Global",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 0.5.dp,
                color = Color(0xFF1E222B)
            )

            // Kolom Kanan: Points & Rewards (dengan PrimoPointIcon)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenPointsRewardsClick() }
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimoPointIcon(size = 22.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Points & Rewards",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${FirebaseManager.formatNumber(points)} Poin",
                        color = Color(0xFFE11D48),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            thickness = 0.5.dp,
            color = Color(0xFF1E222B)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 4. CLEAN BOTTOM MENU LIST (Termasuk "Shop Kosmetik" Baru)
        if (isPublicPreviewMode) {
            PublicProfileShowcaseSection(
                authDb = authDb,
                userRankNumber = userRankNumber,
                points = points
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                CleanEditorialNavRow(
                    icon = Icons.Outlined.Storefront,
                    title = "Shop Kosmetik",
                    subtitle = "Koleksi 985 border, 396 efek & custom title",
                    onClick = onOpenShopClick
                )

                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF1A1D26))

                CleanEditorialNavRow(
                    icon = Icons.Outlined.DownloadForOffline,
                    title = "Unduh",
                    subtitle = "Kelola episode anime & chapter komik offline",
                    onClick = onOpenDownloadsClick
                )

                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF1A1D26))

                CleanEditorialNavRow(
                    icon = Icons.Outlined.Bookmarks,
                    title = "Koleksi & Riwayat",
                    subtitle = "Daftar favorit, riwayat tontonan & bacaan",
                    onClick = onOpenCollectionHistoryClick
                )

                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF1A1D26))

                CleanEditorialNavRow(
                    icon = Icons.Outlined.Info,
                    title = "Info Aplikasi",
                    subtitle = "Tentang Yuranime & versi ${BuildConfig.VERSION_NAME}",
                    onClick = onOpenInfoClick
                )

                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF1A1D26))

                CleanEditorialNavRow(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    title = "Keluar Akun",
                    subtitle = "Akhiri sesi login pada perangkat ini",
                    titleColor = Color(0xFFF43F5E),
                    iconTint = Color(0xFFF43F5E),
                    showChevron = false,
                    onClick = onSignOutClick
                )

                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF1A1D26))

                // Spacer ekstra agar tombol "Keluar Akun" tidak tertutup LiquidGlassNavbar
                Spacer(modifier = Modifier.height(130.dp))
            }
        }
    }
}

@Composable
private fun TopHeaderCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isActive) Color(0xCCFF5722) else Color(0x730B0B0B))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ProfileStatusPill(
    text: String,
    textColor: Color,
    leadingSymbol: String? = null,
    leadingColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF161820))
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingSymbol != null) {
            Text(
                text = leadingSymbol,
                color = leadingColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SfProFontFamily
        )
    }
}

@Composable
private fun SleekHighlightFeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E1015))
            .border(0.5.dp, Color(0xFF1E2028), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(9.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = OutfitDisplayFontFamily,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = JakartaSansFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CleanEditorialNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = Color.White,
    iconTint: Color = Color(0xFFCBD5E1),
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(15.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SfProFontFamily
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF475569),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PublicProfileShowcaseSection(
    authDb: LocalAuthDatabase,
    userRankNumber: Int?,
    points: Long
) {
    val watchCount = remember { authDb.getWatchHistory().size }
    val comicCount = remember { authDb.getComicReadHistory().size }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Statistik Publik Komunitas",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PublicStatMetricBox(
                modifier = Modifier.weight(1f),
                label = "Peringkat",
                value = if (userRankNumber != null) "#$userRankNumber" else "TOP 100"
            )
            PublicStatMetricBox(
                modifier = Modifier.weight(1f),
                label = "Anime Ditonton",
                value = "$watchCount"
            )
            PublicStatMetricBox(
                modifier = Modifier.weight(1f),
                label = "Komik Dibaca",
                value = "$comicCount"
            )
            PublicStatMetricBox(
                modifier = Modifier.weight(1f),
                label = "Crystal Poin",
                value = FirebaseManager.formatNumber(points)
            )
        }
    }
}

@Composable
private fun PublicStatMetricBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF13151D))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 10.5.sp,
            fontFamily = SfProFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickTitlePickerDialog(
    profile: UserProfile?,
    onDismiss: () -> Unit,
    onOpenPointsRewards: () -> Unit
) {
    val context = LocalContext.current
    val level = profile?.level ?: 1
    val unlockedTitles = profile?.unlockedTitles ?: listOf("WARGA YURANIME")
    val equippedTitle = profile?.equippedTitle.orEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF12141E))
                .border(1.dp, Color(0xFF1E2230), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            // iOS Drag Handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2A2E40))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pilih Custom Title",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitDisplayFontFamily
                    )
                    Text(
                        text = "${unlockedTitles.size} Title Terbuka",
                        color = Color(0xFFFF5722),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Ketuk Title yang sudah Anda miliki untuk memasangnya ke profil.",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontFamily = JakartaSansFontFamily
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.height(290.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(FirebaseManager.ALL_TITLES, key = { it.id }) { titleDef ->
                    val isUnlocked = (titleDef.unlockType == TitleUnlockType.MILESTONE && level >= titleDef.requiredLevel) ||
                        unlockedTitles.any { it.equals(titleDef.titleName, ignoreCase = true) }
                    val isEquipped = equippedTitle.equals(titleDef.titleName, ignoreCase = true)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isEquipped) Color(0x26E11D48) else Color(0xFF0C0E15))
                            .border(
                                width = if (isEquipped) 1.dp else 0.5.dp,
                                color = if (isEquipped) Color(0xFFE11D48) else Color(0xFF1B1E2B),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (isUnlocked) {
                                    val target = if (isEquipped) null else titleDef
                                    FirebaseManager.equipTitle(context, target) { _, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                    onDismiss()
                                } else {
                                    onOpenPointsRewards()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GeometricTitleBadge(
                            titleName = titleDef.titleName,
                            tierLevel = titleDef.tierLevel,
                            styleVariant = titleDef.styleVariant,
                            compact = true,
                            scaleFactor = 0.82f
                        )

                        if (isUnlocked) {
                            Text(
                                text = if (isEquipped) "Terpasang" else "Pasang",
                                color = if (isEquipped) Color(0xFFFF5722) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = JakartaSansFontFamily
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (titleDef.pointCost > 0) "${titleDef.pointCost} Poin" else "Lv.${titleDef.requiredLevel}",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF5722), Color(0xFFE11D48))
                        )
                    )
                    .clickable(onClick = onOpenPointsRewards),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Buka Misi & Katalog Title",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JakartaSansFontFamily
                )
            }
        }
    }
}

@Composable
private fun CollectionAndHistorySubScreen(
    authDb: LocalAuthDatabase,
    onBackClick: () -> Unit,
    onEpisodeClick: (episodeId: String, movieId: String) -> Unit,
    onComicChapterClick: ((chapterId: String) -> Unit)?
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val watchHistory = remember { authDb.getWatchHistory() }
    val comicHistory = remember { authDb.getComicReadHistory() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14161E))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Koleksi & Riwayat",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Text(
                    text = "Riwayat tontonan anime & bacaan komik tersimpan",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF14161E))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("Riwayat Anime (${watchHistory.size})", "Riwayat Komik (${comicHistory.size})")
            tabs.forEachIndexed { index, title ->
                val active = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) Color(0xFFE11D48) else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (active) Color.White else Color(0xFF94A3B8),
                        fontSize = 12.5.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedTab == 0) {
                if (watchHistory.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada riwayat tontonan anime.\nMulai tonton episode di Beranda untuk mencatat progresmu.",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp,
                                fontFamily = SfProFontFamily,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(watchHistory, key = { it.movieId }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF14161E))
                                .clickable { onEpisodeClick(item.episodeId, item.movieId) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.posterUrl,
                                contentDescription = item.animeTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(76.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1C1F2B))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.animeTitle,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.episodeTitle,
                                    color = Color(0xFFFF5722),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                if (comicHistory.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada riwayat bacaan komik.\nBuka tab Komik untuk mulai membaca chapter favoritmu.",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp,
                                fontFamily = SfProFontFamily,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(comicHistory, key = { it.comicId }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF14161E))
                                .clickable { onComicChapterClick?.invoke(item.chapterId) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.coverUrl,
                                contentDescription = item.comicTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(76.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1C1F2B))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.comicTitle,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Chapter ${item.chapterNumber}",
                                    color = Color(0xFFE11D48),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginView(
    isLoggingIn: Boolean,
    onBackToHomeClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onGoogleLoginClick: () -> Unit,
    onEmailLoginClick: () -> Unit,
    onGuestLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
    ) {
        SeamlessTiltedPosterBackground()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99000000),
                            Color(0xD90B0B0B),
                            Color(0xFA0B0B0B),
                            Color(0xFF0B0B0B)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x55000000))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pengaturan",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x55000000))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { onBackToHomeClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Kembali ke Beranda",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.55f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                    contentDescription = "Yuranime Wolf",
                    tint = Color.White,
                    modifier = Modifier.size(84.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "YURANIME",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontFamily = YuranimeLogoFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Tempat Nonton Anime Subtitle Indonesia",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.5.sp,
                    fontFamily = SfProFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1.0f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(enabled = !isLoggingIn) { onGoogleLoginClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoggingIn) {
                    CircularProgressIndicator(
                        color = Color(0xFF111827),
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Masuk dengan Google",
                            color = Color(0xFF111827),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161820))
                    .border(1.dp, Color(0x33E11D48), CircleShape)
                    .clickable(enabled = !isLoggingIn) { onEmailLoginClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Pilih Akun Google Lainnya",
                    color = Color(0xFFE2E8F0),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Login Tanpa Akun",
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SfProFontFamily,
                modifier = Modifier
                    .clickable { onGuestLoginClick() }
                    .padding(vertical = 4.dp, horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val agreementText = remember {
                buildAnnotatedString {
                    append("Dengan melanjutkan, kamu menyetujui ")
                    withStyle(
                        style = SpanStyle(
                            color = Color.White,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append("Ketentuan Layanan")
                    }
                    append(" dan ")
                    withStyle(
                        style = SpanStyle(
                            color = Color.White,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append("Kebijakan Privasi")
                    }
                }
            }

            Text(
                text = agreementText,
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp,
                fontFamily = SfProFontFamily,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Versi ${BuildConfig.VERSION_NAME}",
                color = Color(0xFF6B7280),
                fontSize = 12.sp,
                fontFamily = SfProFontFamily
            )

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SeamlessTiltedPosterBackground() {
    val col1Posters = listOf(
        R.drawable.poster_login_01,
        R.drawable.poster_login_04,
        R.drawable.poster_login_07,
        R.drawable.poster_login_10
    )
    val col2Posters = listOf(
        R.drawable.poster_login_02,
        R.drawable.poster_login_05,
        R.drawable.poster_login_08,
        R.drawable.poster_login_11
    )
    val col3Posters = listOf(
        R.drawable.poster_login_03,
        R.drawable.poster_login_06,
        R.drawable.poster_login_09,
        R.drawable.poster_login_12
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationZ = -12f
                scaleX = 1.3f
                scaleY = 1.3f
            }
    ) {
        val density = LocalDensity.current
        val colWidth = (maxWidth - 16.dp) / 3
        val posterHeight = colWidth * 1.5f
        val itemHeightWithSpacing = posterHeight + 8.dp
        val cycleHeightPx = with(density) { (itemHeightWithSpacing * 4).toPx() }

        val transition = rememberInfiniteTransition(label = "seamlessPosterTransition")

        val progressCol1 by transition.animateFloat(
            initialValue = 0f,
            targetValue = -1f,
            animationSpec = infiniteRepeatable(
                animation = tween(42000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progressCol1"
        )

        val progressCol2 by transition.animateFloat(
            initialValue = -1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(38000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progressCol2"
        )

        val progressCol3 by transition.animateFloat(
            initialValue = 0f,
            targetValue = -1f,
            animationSpec = infiniteRepeatable(
                animation = tween(45000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progressCol3"
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SeamlessMarqueeColumn(
                posters = col1Posters,
                offsetY = progressCol1 * cycleHeightPx,
                modifier = Modifier.weight(1f)
            )
            SeamlessMarqueeColumn(
                posters = col2Posters,
                offsetY = progressCol2 * cycleHeightPx,
                modifier = Modifier.weight(1f)
            )
            SeamlessMarqueeColumn(
                posters = col3Posters,
                offsetY = progressCol3 * cycleHeightPx,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SeamlessMarqueeColumn(
    posters: List<Int>,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    val repeatedPosters = remember(posters) { posters + posters + posters }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .offset { IntOffset(0, offsetY.toInt()) },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeatedPosters.forEach { resId ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun AppInfoModal(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF14161E))
                .border(1.dp, Color(0xFF1F2433), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tentang Yuranime",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Icon(
                    painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "YURANIME",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = YuranimeLogoFontFamily,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Versi ${BuildConfig.VERSION_NAME}",
                    color = Color(0xFFFF5722),
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Yuranime adalah aplikasi streaming anime dan baca komik subtitle Indonesia gratis, bebas iklan mengganggu, dengan sistem profil, title, dan papan peringkat real-time.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.5.sp,
                    fontFamily = SfProFontFamily,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF5722), Color(0xFFE11D48))
                            )
                        )
                        .clickable { onDismiss() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tutup",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}
