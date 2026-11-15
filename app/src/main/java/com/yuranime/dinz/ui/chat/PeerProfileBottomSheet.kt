package com.yuranime.dinz.ui.chat

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.GlobalChatMessage
import com.yuranime.dinz.data.firebase.UserProfile
import com.yuranime.dinz.data.repository.DiscordCosmeticsRepository
import com.yuranime.dinz.ui.components.AnimatedCosmeticImage
import com.yuranime.dinz.ui.components.ApngStackedEffectView
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.components.MultiLayerProfileEffectView
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private val SheetSurfaceColor = Color(0xFF0E1015)

/**
 * Bottom Sheet Preview Profil teman saat avatar atau nama diklik di Chat Global.
 * Banner 140.dp di atas, avatar 78.dp tumpang tindih di sisi kanan dengan kosmetik animasi penuh,
 * serta identitas kiri berurutan tanpa tombol Tambah Teman maupun Kunjungi Profil.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PeerProfileBottomSheet(
    peerMessage: GlobalChatMessage?,
    onDismiss: () -> Unit
) {
    if (peerMessage == null) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Muat data profil Firestore terbaru agar bio, banner, dan tanggal bergabung selalu akurat
    var latestProfile by remember(peerMessage.uid) { mutableStateOf<UserProfile?>(null) }
    var cosmeticsLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(peerMessage.uid) {
        withContext(Dispatchers.IO) {
            DiscordCosmeticsRepository.getAllBorders(context)
            DiscordCosmeticsRepository.getAllProfileEffects(context)
        }
        cosmeticsLoaded = true
        latestProfile = FirebaseManager.fetchUserProfileByUid(peerMessage.uid)
    }

    val displayName = latestProfile?.name?.ifBlank { null } ?: peerMessage.name
    val userTag = latestProfile?.userTag?.ifBlank { null } ?: peerMessage.userTag
    val avatarUrl = latestProfile?.avatar?.ifBlank { null } ?: peerMessage.avatar
    val bannerUrl = latestProfile?.banner?.ifBlank { null } ?: peerMessage.banner
    val bio = latestProfile?.bio?.ifBlank { null }
        ?: peerMessage.bio.ifBlank { "Penikmat anime & komik di Yuranime." }
    val joinDate = latestProfile?.joinDate?.ifBlank { null }
        ?: peerMessage.joinDate.ifBlank { "Baru bergabung" }
    val role = latestProfile?.role?.ifBlank { null } ?: peerMessage.role.ifBlank { "Member" }
    val level = latestProfile?.level ?: peerMessage.level
    val rankTitle = latestProfile?.rankTitle?.ifBlank { null }
        ?: peerMessage.rankTitle.ifBlank { FirebaseManager.computeRankTitle(level) }
    val equippedTitle = latestProfile?.equippedTitle ?: peerMessage.equippedTitle
    val equippedTitleTier = latestProfile?.equippedTitleTier ?: peerMessage.equippedTitleTier
    val equippedTitleVariant = latestProfile?.equippedTitleVariant ?: peerMessage.equippedTitleVariant
    val borderUrl = latestProfile?.equippedBorderUrl ?: peerMessage.equippedBorderUrl
    val effectUrl = latestProfile?.equippedProfileEffectUrl ?: peerMessage.equippedProfileEffectUrl

    val animatedBorderUrl = remember(borderUrl) {
        if (borderUrl.isNotBlank()) {
            DiscordCosmeticsRepository.getOptimizedUrl(borderUrl, size = 240, animated = true)
        } else ""
    }
    val effectDef = remember(effectUrl, cosmeticsLoaded) {
        if (effectUrl.isNotBlank()) {
            DiscordCosmeticsRepository.findEffect(context, effectUrl)
        } else null
    }
    val effectLayers = remember(effectDef, effectUrl) {
        effectDef?.layerUrls?.ifEmpty { listOf(effectUrl) }
            ?: if (effectUrl.isNotBlank()) listOf(effectUrl) else emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetSurfaceColor,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SheetSurfaceColor)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp)
            ) {
                // Header: Banner 140.dp + Avatar 78.dp overlap di sisi kanan
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(178.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        if (bannerUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(bannerUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Banner $displayName",
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
                                            start = Offset.Zero,
                                            end = Offset(820f, 380f)
                                        )
                                    )
                            )
                        }

                        // Gradasi bawah memudar halus ke latar sheet #0E1015
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color(0x140E1015),
                                            0.55f to Color(0x6E0E1015),
                                            0.88f to Color(0xD60E1015),
                                            1.0f to SheetSurfaceColor
                                        )
                                    )
                                )
                        )
                    }

                    // Avatar 78.dp di SISI KANAN menumpuk di batas bawah banner + Avatar Decoration animasi penuh
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp)
                            .size(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF161820))
                                .border(3.dp, SheetSurfaceColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(avatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto Profil $displayName",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = displayName.take(1).uppercase(Locale.ROOT),
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }

                        if (animatedBorderUrl.isNotBlank()) {
                            AnimatedCosmeticImage(
                                url = animatedBorderUrl,
                                isAnimated = true,
                                contentDescription = "Avatar Decoration",
                                modifier = Modifier.size(96.dp)
                            )
                        }
                    }
                }

                // Identitas di sisi kiri dengan urutan persis sesuai spesifikasi
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    // 1. Username (SF Pro Bold 22.sp)
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // 2. #ID • Bergabung sejak [Tanggal]
                    Text(
                        text = "#$userTag • Bergabung sejak $joinDate",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = SfProFontFamily
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Baris Status & Mini-Title (Tanpa kotak/kapsul shadow overlay)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isPriv = role.equals("Admin", ignoreCase = true) ||
                            role.equals("Owner", ignoreCase = true)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.WorkspacePremium,
                                contentDescription = null,
                                tint = if (isPriv) Color(0xFFF43F5E) else Color(0xFFFF5722),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = role.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                                },
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardDoubleArrowUp,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LV. $level",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Diamond,
                                contentDescription = null,
                                tint = if (level >= 35) Color(0xFFF59E0B) else Color(0xFFE11D48),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = rankTitle,
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }

                    // Custom Title Besar jika sedang dipasang akun tersebut
                    if (equippedTitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        GeometricTitleBadge(
                            titleName = equippedTitle,
                            tierLevel = equippedTitleTier,
                            styleVariant = equippedTitleVariant,
                            isAnimated = true,
                            compact = false
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Bio Pengguna
                    Text(
                        text = bio,
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            // Profile Effect bergerak penuh di atas header sheet jika akun memasang effect
            if (effectUrl.isNotBlank() && effectLayers.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(20f)
                ) {
                    if (effectDef != null) {
                        MultiLayerProfileEffectView(
                            effect = effectDef,
                            scaleType = ImageView.ScaleType.FIT_START,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ApngStackedEffectView(
                            layerUrls = effectLayers,
                            scaleType = ImageView.ScaleType.FIT_START,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
