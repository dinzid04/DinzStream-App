package com.yuranime.dinz.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.BuildConfig
import com.yuranime.dinz.R
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.ui.components.BottomNavBar
import com.yuranime.dinz.ui.components.NavTab
import com.yuranime.dinz.ui.leaderboard.LeaderboardScreen
import com.yuranime.dinz.ui.profile.subscreens.DownloadsScreen
import com.yuranime.dinz.ui.profile.subscreens.EditProfileScreen
import com.yuranime.dinz.ui.profile.subscreens.PointsRewardsScreen
import com.yuranime.dinz.ui.profile.subscreens.SettingsScreen
import com.yuranime.dinz.ui.theme.SfProFontFamily
import com.yuranime.dinz.ui.theme.YuranimeLogoFontFamily

enum class ProfileSubScreen {
    MAIN,
    EDIT_PROFILE,
    LEADERBOARD,
    POINTS_REWARDS,
    DOWNLOADS,
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
    onCheckUpdateClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var currentSubScreen by remember { mutableStateOf(ProfileSubScreen.MAIN) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showEmailLoginDialog by remember { mutableStateOf(false) }

    // Real-Time Firebase Auth State (100% Reactive)
    val currentUser by FirebaseManager.currentUser.collectAsState()
    val isFirebaseLoggedIn = currentUser != null
    val userProfile by FirebaseManager.userProfile.collectAsState()

    // Google Sign-In Client (Menampilkan Dialog Pemilih Akun Google Lengkap dengan Avatar, Nama & Email)
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("400464992596-68fde66c88d351699610c1.apps.googleusercontent.com")
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
            val idToken = account?.idToken
            val email = account?.email.orEmpty().trim().lowercase()
            val displayName = account?.displayName?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").split(".", "_", "-")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    .ifBlank { "Pengguna Google" }
            val photoUrl = account?.photoUrl?.toString()

            if (!idToken.isNullOrBlank()) {
                isLoggingIn = true
                FirebaseManager.signInWithGoogleIdToken(
                    idToken = idToken,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    activity = activity,
                    onSuccess = {
                        isLoggingIn = false
                        Toast.makeText(context, "Selamat datang, $displayName!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        isLoggingIn = false
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        showEmailLoginDialog = true
                    }
                )
            } else if (email.isNotBlank()) {
                isLoggingIn = true
                FirebaseManager.signInWithGoogleAccount(
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    activity = activity,
                    onSuccess = {
                        isLoggingIn = false
                        Toast.makeText(context, "Selamat datang, $displayName!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        isLoggingIn = false
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        showEmailLoginDialog = true
                    }
                )
            } else {
                isLoggingIn = false
            }
        } catch (e: ApiException) {
            // Jika Play Services error (misalnya kode 10 karena SHA-1 mismatch debug),
            // fallback langsung ke OAuthProvider yang membuka Google Sign-In web autentikasi
            if (activity != null) {
                isLoggingIn = true
                FirebaseManager.signInWithGoogleProvider(
                    activity = activity,
                    onSuccess = {
                        isLoggingIn = false
                        Toast.makeText(context, "Selamat datang di Yuranime!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        isLoggingIn = false
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        showEmailLoginDialog = true
                    }
                )
            } else {
                isLoggingIn = false
                showEmailLoginDialog = true
            }
        } catch (e: Exception) {
            isLoggingIn = false
            if (activity != null) {
                FirebaseManager.signInWithGoogleProvider(
                    activity = activity,
                    onSuccess = {
                        Toast.makeText(context, "Selamat datang di Yuranime!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        showEmailLoginDialog = true
                    }
                )
            } else {
                showEmailLoginDialog = true
            }
        }
    }

    LaunchedEffect(isFirebaseLoggedIn) {
        if (isFirebaseLoggedIn) {
            FirebaseManager.startListeningCurrentUser(context)
        }
    }

    // Tangani Tombol Back untuk Sub-screen
    BackHandler(enabled = currentSubScreen != ProfileSubScreen.MAIN) {
        currentSubScreen = ProfileSubScreen.MAIN
    }

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
        label = "ProfileSubScreenAnim"
    ) { subScreen ->
        when (subScreen) {
            ProfileSubScreen.EDIT_PROFILE -> {
                EditProfileScreen(
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
                    onBackClick = { currentSubScreen = ProfileSubScreen.MAIN }
                )
            }
            ProfileSubScreen.DOWNLOADS -> {
                DownloadsScreen(
                    onBackClick = { currentSubScreen = ProfileSubScreen.MAIN }
                )
            }
            ProfileSubScreen.SETTINGS -> {
                SettingsScreen(
                    onBackClick = { currentSubScreen = ProfileSubScreen.MAIN },
                    onCheckUpdateClick = onCheckUpdateClick
                )
            }
            ProfileSubScreen.MAIN -> {
                if (!isFirebaseLoggedIn) {
                    // LAYAR LOGIN (BottomNavBar 100% Tersembunyi)
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
                                if (activity != null) {
                                    FirebaseManager.signInWithGoogleProvider(
                                        activity = activity,
                                        onSuccess = {
                                            isLoggingIn = false
                                            Toast.makeText(context, "Selamat datang di Yuranime!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            isLoggingIn = false
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            showEmailLoginDialog = true
                                        }
                                    )
                                } else {
                                    isLoggingIn = false
                                    showEmailLoginDialog = true
                                }
                            }
                        },
                        onEmailLoginClick = {
                            showEmailLoginDialog = true
                        },
                        onGuestLoginClick = {
                            onTabSelected(NavTab.HOME)
                        }
                    )
                } else {
                    // LAYAR PROFIL (Setelah Login Real-Time)
                    Scaffold(
                        modifier = modifier.fillMaxSize(),
                        containerColor = Color(0xFF0E0F12),
                        bottomBar = {
                            BottomNavBar(
                                selectedTab = selectedTab,
                                onTabSelected = onTabSelected
                            )
                        }
                    ) { innerPadding ->
                        UserProfileView(
                            profile = userProfile,
                            currentUser = currentUser,
                            modifier = Modifier.padding(innerPadding),
                            onEditProfileClick = { currentSubScreen = ProfileSubScreen.EDIT_PROFILE },
                            onDownloadMenuClick = { currentSubScreen = ProfileSubScreen.DOWNLOADS },
                            onLeaderboardMenuClick = { currentSubScreen = ProfileSubScreen.LEADERBOARD },
                            onRewardsMenuClick = { currentSubScreen = ProfileSubScreen.POINTS_REWARDS },
                            onSettingsMenuClick = { currentSubScreen = ProfileSubScreen.SETTINGS },
                            onInfoMenuClick = { showInfoDialog = true },
                            onLogout = {
                                FirebaseManager.signOut()
                                Toast.makeText(context, "Akun berhasil keluar", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEmailLoginDialog) {
        EmailLoginDialog(
            onDismiss = { showEmailLoginDialog = false },
            onSubmit = { email, displayName, password ->
                isLoggingIn = true
                FirebaseManager.signInWithGoogleAccount(
                    email = email,
                    displayName = displayName,
                    photoUrl = null,
                    googleId = null,
                    passwordOverride = password,
                    activity = activity,
                    onSuccess = {
                        isLoggingIn = false
                        showEmailLoginDialog = false
                        Toast.makeText(context, "Selamat datang, $displayName!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        isLoggingIn = false
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            },
            onForgotPassword = { emailToReset ->
                FirebaseManager.sendPasswordReset(
                    email = emailToReset,
                    onSuccess = {
                        Toast.makeText(context, "Link reset kata sandi telah dikirim ke $emailToReset", Toast.LENGTH_LONG).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (showInfoDialog) {
        AppInfoModal(onDismiss = { showInfoDialog = false })
    }
}

/**
 * TAHAP 1 & 2: Halaman Profile Real-Time Setelah Login
 * - HAPUS sepenuhnya baris tombol media sosial!
 * - Rapatkan tata letak header & menu tanpa celah besar (Arrangement.spacedBy(8.dp))
 */
@Composable
private fun UserProfileView(
    profile: com.yuranime.dinz.data.firebase.UserProfile?,
    currentUser: com.google.firebase.auth.FirebaseUser? = null,
    modifier: Modifier = Modifier,
    onEditProfileClick: () -> Unit,
    onDownloadMenuClick: () -> Unit,
    onLeaderboardMenuClick: () -> Unit,
    onRewardsMenuClick: () -> Unit,
    onSettingsMenuClick: () -> Unit,
    onInfoMenuClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val name = profile?.name?.takeIf { it.isNotBlank() }
        ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.email?.substringBefore("@")
        ?: "Otaku Member"
    val avatarUrl = profile?.avatar?.takeIf { it.isNotBlank() }
        ?: currentUser?.photoUrl?.toString()
        ?: ""
    val bannerUrl = profile?.banner ?: ""
    val level = profile?.level ?: 1
    val exp = profile?.exp ?: 0L
    val points = profile?.points ?: 100L
    val expProgress = profile?.expProgress ?: 0.1f
    val expToNext = profile?.expToNextLevel ?: 1000L

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Header Banner Landscape Anime dengan Avatar, Nama, & Bar XP
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                // Background Cover / Banner
                if (bannerUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(bannerUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover Profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.poster_login_01),
                        contentDescription = "Cover Profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Gradasi Gelap Halus ke Warna Latar Utama (#0E0F12)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x330E0F12),
                                    Color(0x880E0F12),
                                    Color(0xFF0E0F12)
                                )
                            )
                        )
                )

                // Avatar, Nama & Level di Kiri Bawah Banner
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 12.dp, end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Foto Profil Bulat dengan Border Oranye & Badge Kamera Biru
                    Box(modifier = Modifier.size(76.dp)) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2024))
                                .border(2.dp, Color(0xFFFF5722), CircleShape)
                                .clickable { onEditProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(avatarUrl)
                                        .crossfade(true)
                                        .placeholder(R.drawable.ic_yuranime_monochrome)
                                        .error(R.drawable.ic_yuranime_monochrome)
                                        .build(),
                                    contentDescription = "Foto Profil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                                    contentDescription = "Foto Profil",
                                    tint = Color(0xFFFF5722),
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        // Badge Ikon Kamera Biru Kecil di Pojok Kanan Bawah
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6))
                                .border(1.5.dp, Color(0xFF0E0F12), CircleShape)
                                .clickable { onEditProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Ubah Foto",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Sisi Kanan: Nama Pengguna + Ikon Pensil Edit + Lv + Bar XP
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profil",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onEditProfileClick() }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFF5722).copy(alpha = 0.2f))
                                    .border(0.7.dp, Color(0xFFFF5722), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Lv. $level",
                                    color = Color(0xFFFF5722),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "$exp XP",
                                color = Color(0xFFFDE68A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SfProFontFamily
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress Bar XP Tipis Elegan
                        LinearProgressIndicator(
                            progress = { expProgress },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFFFF5722),
                            trackColor = Color(0xFF2A2D34)
                        )
                    }
                }
            }
        }

        // 2. Daftar Menu Profile (Tepat Menempel Rapat di Bawah Header, Bebas Celah Renggang!)
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Edit Profil - Sub-teks: "Ganti Nama, Foto atau Cover"
                ProfileMenuItem(
                    icon = Icons.Default.Edit,
                    iconTint = Color(0xFF94A3B8),
                    title = "Edit Profil",
                    subtitle = "Ganti Nama, Foto atau Cover",
                    trailingType = TrailingType.CHEVRON,
                    onClick = onEditProfileClick
                )

                // 2. Unduh - Sub-teks: "Kelola konten yang diunduh"
                ProfileMenuItem(
                    icon = Icons.Default.FileDownload,
                    iconTint = Color(0xFF94A3B8),
                    title = "Unduh",
                    subtitle = "Kelola konten yang diunduh",
                    trailingType = TrailingType.CHEVRON,
                    onClick = onDownloadMenuClick
                )

                // 3. Papan Peringkat - Sub-teks: "Lihat peringkat XP bulanan" (badge: TOP 100)
                ProfileMenuItem(
                    icon = Icons.Default.EmojiEvents,
                    iconTint = Color(0xFFFBBF24),
                    title = "Papan Peringkat",
                    subtitle = "Lihat peringkat XP bulanan",
                    trailingType = TrailingType.BADGE_TOP_100,
                    onClick = onLeaderboardMenuClick
                )

                // 4. Points & Rewards - Sub-teks: "Check-in harian & tukar poin" (badge: ★ [Jumlah Poin Real-time])
                ProfileMenuItem(
                    icon = Icons.Default.Star,
                    iconTint = Color(0xFFFBBF24),
                    title = "Points & Rewards",
                    subtitle = "Check-in harian & tukar poin",
                    trailingType = TrailingType.BADGE_COINS,
                    coinsAmount = points.toInt(),
                    onClick = onRewardsMenuClick
                )

                // 5. Pengaturan - Sub-teks: "Atur tampilan slider & preferensi aplikasi"
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    iconTint = Color(0xFF94A3B8),
                    title = "Pengaturan",
                    subtitle = "Atur tampilan slider & preferensi aplikasi",
                    trailingType = TrailingType.CHEVRON,
                    onClick = onSettingsMenuClick
                )

                // 6. Info - Sub-teks: "Simak Cerita Kami"
                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    iconTint = Color(0xFF94A3B8),
                    title = "Info",
                    subtitle = "Simak Cerita Kami",
                    trailingType = TrailingType.CHEVRON,
                    onClick = onInfoMenuClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 6. Keluar Akun (Logout Bersih)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161B26))
                        .border(0.7.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { onLogout() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Keluar Akun",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Keluar Akun",
                                color = Color(0xFFEF4444),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = "Keluar dari sesi Firebase & Google saat ini",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Satu Baris Menu Profile Bersih & Rapat
 */
@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingType: TrailingType,
    coinsAmount: Int = 0,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161B26))
            .border(0.7.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2024)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily
                )
            }

            when (trailingType) {
                TrailingType.BADGE_TOP_100 -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF281C3F))
                            .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "TOP 100",
                            color = Color(0xFFFDE68A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = SfProFontFamily
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
                TrailingType.BADGE_COINS -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2E2211))
                            .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$coinsAmount",
                            color = Color(0xFFFDE68A),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
                TrailingType.CHEVRON -> {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
                TrailingType.NONE -> {}
            }
        }
    }
}

/**
 * TAHAP 1: Layar Login Sinematik (Menggunakan Google Provider Real-Time Firebase)
 */
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
        // 1. Kolase Poster Bergerak Lembut Miring (-12°)
        SeamlessTiltedPosterBackground()

        // 2. Lapisan Overlay Hitam Transparan
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

        // 3. Tombol Aksi di Pojok Kanan Atas (Pengaturan & Kembali)
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

        // 4. Grup Branding Tengah & Tombol Aksi Bawah
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.55f))

            // Branding YURANIME
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

            // Tombol "Masuk dengan Google"
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

            // Tombol Alternatif "Masuk dengan Email / Akun Dinz"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1B2E))
                    .border(1.dp, Color(0x338B5CF6), CircleShape)
                    .clickable(enabled = !isLoggingIn) { onEmailLoginClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Masuk dengan Email / Akun Dinz",
                    color = Color(0xFFE2E8F0),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tombol "Login Tanpa Akun"
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

            // Teks Kebijakan
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
                .background(Color(0xFF161B26))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
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
                    text = "Yuranime adalah aplikasi streaming anime subtitle Indonesia gratis, bebas iklan mengganggu, dan didukung oleh komunitas anime Indonesia dengan database Firebase terintegrasi.",
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
                        .background(Color(0xFFFF5722))
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

/**
 * Dialog Masuk Akun / Fallback jika Google Play Services gagal
 */
@Composable
private fun EmailLoginDialog(
    onDismiss: () -> Unit,
    onSubmit: (email: String, displayName: String, password: String?) -> Unit,
    onForgotPassword: (email: String) -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF14131D))
                .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Masuk Akun Yuranime",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Gunakan email Google atau akun web DinzStream kamu",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.5.sp,
                    fontFamily = SfProFontFamily,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Alamat Email", fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = Color(0xFFC084FC),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        cursorColor = Color(0xFF8B5CF6)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nama Tampilan (Opsional)", fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = Color(0xFFC084FC),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        cursorColor = Color(0xFF8B5CF6)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Kata Sandi (Opsional jika sudah punya)", fontSize = 13.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = Color(0xFFC084FC),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        cursorColor = Color(0xFF8B5CF6)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Lupa kata sandi?",
                        color = Color(0xFFC084FC),
                        fontSize = 12.sp,
                        fontFamily = SfProFontFamily,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable {
                                if (email.isBlank() || !email.contains("@")) {
                                    errorMessage = "Isi email di atas terlebih dahulu untuk meminta link reset"
                                    infoMessage = null
                                } else {
                                    errorMessage = null
                                    infoMessage = "Tautan reset dikirim ke ${email.trim()}"
                                    onForgotPassword(email.trim())
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontFamily = SfProFontFamily,
                        textAlign = TextAlign.Center
                    )
                }

                if (infoMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = infoMessage ?: "",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontFamily = SfProFontFamily,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Batal",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            fontFamily = SfProFontFamily
                        )
                    }

                    Button(
                        onClick = {
                            if (email.isBlank() || !email.contains("@")) {
                                errorMessage = "Harap masukkan alamat email yang valid"
                                return@Button
                            }
                            isSubmitting = true
                            errorMessage = null
                            val effectiveName = displayName.ifBlank { email.substringBefore("@") }
                            val effectivePass = password.ifBlank { null }
                            onSubmit(email.trim(), effectiveName, effectivePass)
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B5CF6)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Masuk Sekarang",
                                color = Color.White,
                                fontSize = 14.sp,
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
