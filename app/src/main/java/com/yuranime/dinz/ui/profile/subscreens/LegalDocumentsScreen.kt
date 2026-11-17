package com.yuranime.dinz.ui.profile.subscreens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.R
import com.yuranime.dinz.ui.theme.SfProFontFamily

enum class LegalDocumentType {
    TERMS_OF_SERVICE,
    PRIVACY_POLICY
}

private val LegalCanvasBg = Color(0xFF0B0B0B)
private val LegalDividerColor = Color(0xFF181A20)
private val LegalBodyTextColor = Color(0xFFD1D5DB)
private val LegalMutedDateColor = Color(0xFF9CA3AF)
private val LegalProgressTrackColor = Color(0xFF1B1F24)
private val LegalProgressFillColor = Color(0xFFBED62C)

private data class LegalSectionContent(
    val heading: String,
    val paragraphs: List<String> = emptyList(),
    val bullets: List<String> = emptyList(),
    val definitions: List<Pair<String, String>> = emptyList(),
    val postParagraphs: List<String> = emptyList()
)

@Composable
fun LegalDocumentScreen(
    documentType: LegalDocumentType,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var heroHeightPx by remember { mutableIntStateOf(0) }

    val showStickyBar by remember {
        derivedStateOf {
            val threshold = (heroHeightPx * 0.56f).toInt().coerceAtLeast(130)
            scrollState.value > threshold
        }
    }

    val rawReadingProgress by remember {
        derivedStateOf {
            val max = scrollState.maxValue
            if (max > 0) {
                (scrollState.value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    val readingProgress by animateFloatAsState(
        targetValue = rawReadingProgress,
        animationSpec = tween(durationMillis = 60),
        label = "legalReadingProgress"
    )

    val pageTitle = when (documentType) {
        LegalDocumentType.TERMS_OF_SERVICE -> "Terms of Service"
        LegalDocumentType.PRIVACY_POLICY -> "Privacy Policy"
    }

    val lastUpdatedDate = "November 17, 2026"

    val sections = remember(documentType) {
        when (documentType) {
            LegalDocumentType.TERMS_OF_SERVICE -> buildTermsOfServiceSections()
            LegalDocumentType.PRIVACY_POLICY -> buildPrivacyPolicySections()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LegalCanvasBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
        ) {
            // Hero Header dengan gradasi lembut, tombol back bulat kecil, judul besar & watermark logo pojok kanan atas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .onGloballyPositioned { heroHeightPx = it.size.height }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1F2A14),
                                Color(0xFF12180E),
                                LegalCanvasBg
                            ),
                            center = Offset(860f, 40f),
                            radius = 860f
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                LegalCanvasBg
                            )
                        )
                    )
            ) {
                // Watermark logo aplikasi monochrome tanpa background kotak, miring & terpotong estetis di pojok kanan atas
                Image(
                    painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.065f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(230.dp)
                        .offset(x = 52.dp, y = (-18).dp)
                        .graphicsLayer { rotationZ = -18f }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x5522271E))
                            .border(0.8.dp, Color(0x28FFFFFF), CircleShape)
                            .clickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = pageTitle,
                        color = Color.White,
                        fontSize = 31.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        letterSpacing = (-0.4).sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = Color(0xFFE5E7EB),
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) {
                                append("Last updated ")
                            }
                            withStyle(
                                SpanStyle(
                                    color = LegalMutedDateColor,
                                    fontWeight = FontWeight.Normal
                                )
                            ) {
                                append(lastUpdatedDate)
                            }
                        },
                        fontSize = 13.5.sp,
                        fontFamily = SfProFontFamily
                    )

                    Spacer(modifier = Modifier.height(22.dp))
                }
            }

            HorizontalDivider(thickness = 0.8.dp, color = LegalDividerColor)

            // Bab-bab dokumen legal
            sections.forEachIndexed { index, section ->
                LegalSectionBlock(section = section)
                if (index < sections.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.7.dp,
                        color = LegalDividerColor,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(44.dp))
        }

        // Sticky Top App Bar saat pengguna scroll melewati header judul + garis Reading Progress Bar
        AnimatedVisibility(
            visible = showStickyBar,
            enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { -it / 4 },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 4 },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LegalCanvasBg)
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1A1D22))
                            .border(0.8.dp, Color(0xFF282D35), RoundedCornerShape(10.dp))
                            .clickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = pageTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(LegalProgressTrackColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(readingProgress)
                            .fillMaxHeight()
                            .background(LegalProgressFillColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegalSectionBlock(section: LegalSectionContent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Text(
            text = section.heading,
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily
        )

        if (section.paragraphs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                section.paragraphs.forEach { paragraph ->
                    Text(
                        text = paragraph,
                        color = LegalBodyTextColor,
                        fontSize = 14.5.sp,
                        lineHeight = 22.5.sp,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }

        if (section.definitions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                section.definitions.forEach { (term, explanation) ->
                    Text(
                        text = buildDefinitionLine(term = term, explanation = explanation),
                        fontSize = 14.5.sp,
                        lineHeight = 22.5.sp,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }

        if (section.bullets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                section.bullets.forEach { bullet ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "-",
                            color = LegalBodyTextColor,
                            fontSize = 14.5.sp,
                            lineHeight = 22.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = bullet,
                            color = LegalBodyTextColor,
                            fontSize = 14.5.sp,
                            lineHeight = 22.5.sp,
                            fontFamily = SfProFontFamily,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (section.postParagraphs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                section.postParagraphs.forEach { paragraph ->
                    Text(
                        text = paragraph,
                        color = LegalBodyTextColor,
                        fontSize = 14.5.sp,
                        lineHeight = 22.5.sp,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}

private fun buildDefinitionLine(term: String, explanation: String): AnnotatedString {
    return buildAnnotatedString {
        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) {
            append(term)
            append(": ")
        }
        withStyle(SpanStyle(color = LegalBodyTextColor, fontWeight = FontWeight.Normal)) {
            append(explanation)
        }
    }
}

private fun buildTermsOfServiceSections(): List<LegalSectionContent> {
    return listOf(
        LegalSectionContent(
            heading = "Acceptance of Terms",
            paragraphs = listOf(
                "These Terms of Service (\"Terms\") constitute a legally binding agreement between you and Yuranime (\"Yuranime\", \"we\", \"us\", or \"our\") governing your access to and use of the Yuranime Android mobile application, companion configuration pages, community features, and related utilities (collectively, the \"Service\").",
                "By downloading, installing, browsing, or using any part of the Service, you acknowledge that you have read, understood, and agreed to be bound by these Terms as well as our Privacy Policy. If you do not agree with any provision of these Terms, you must immediately discontinue use of the Service and uninstall the application."
            )
        ),
        LegalSectionContent(
            heading = "Eligibility",
            paragraphs = listOf(
                "You must be at least 13 years old (or the minimum age of digital consent applicable in your jurisdiction, whichever is higher) to access or use the Service. By using Yuranime, you represent and warrant that you meet this age requirement and possess the legal capacity to enter into these Terms.",
                "If you are under 18 years old, you confirm that you are using the Service under the supervision and permission of a parent or legal guardian who agrees to be bound by these Terms on your behalf."
            )
        ),
        LegalSectionContent(
            heading = "Nature of the Service and Disclaimer",
            paragraphs = listOf(
                "Yuranime operates solely as an unofficial, fan-maintained client, index, and aggregator interface that displays metadata, release schedules, comic chapters, and streaming links made publicly accessible by independent third-party providers.",
                "We do not host, upload, encode, mirror, or store any anime video files, donghua streams, manga/manhwa images, or external copyrighted media on servers owned or operated by Yuranime. All trademarks, series titles, character artwork, posters, soundtracks, and copyrighted works remain the sole property of their respective studios, publishers, and rights holders.",
                "Yuranime makes no representations regarding the uptime, legality, accuracy, or quality of content hosted on external third-party servers linked through the Service. If you are a copyright owner or authorized agent and believe that any link or index entry accessible via the Service infringes your intellectual property rights, please submit a written notice (including DMCA takedown requests) to admin@yuranime.com containing: (a) a clear identification of the copyrighted work claimed to be infringed, (b) the specific title, episode, or endpoint reference within Yuranime where the material appears, (c) your full name, valid email address, and authority declaration, and (d) a good-faith statement under penalty of perjury that the disputed reference is not authorized. Upon verification, we will promptly review and remove or disable access to the reported index entry."
            )
        ),
        LegalSectionContent(
            heading = "User Account & Security",
            paragraphs = listOf(
                "Certain interactive features, including cloud watch history synchronization, profile customization, cosmetic item ownership, point progression, global chat, and profile badges, require signing in with a valid Google account. You may also browse public anime catalogs in limited guest mode without signing in.",
                "You are solely responsible for safeguarding access to the Google account and Android device used with the Service, and for all actions, comments, and chat messages transmitted under your profile. You agree to provide accurate profile details and to notify us immediately at admin@yuranime.com if you suspect unauthorized access, account compromise, or abnormal activity involving your Yuranime profile."
            )
        ),
        LegalSectionContent(
            heading = "Acceptable Use Policy",
            paragraphs = listOf(
                "When participating in episode comment threads, replies, sticker reactions, global chat rooms, leaderboard profiles, or any interactive area of the Service, you agree to conduct yourself respectfully and lawfully. Specifically, you agree NOT to:"
            ),
            bullets = listOf(
                "Post, upload, or share any content that is unlawful, defamatory, harassing, threatening, abusive, obscene, sexually explicit, or promotes violence and self-harm.",
                "Spread hate speech, discrimination, or harassment based on ethnicity, religion, race, inter-group relations (SARA), gender, sexual orientation, disability, or nationality.",
                "Send repetitive spam, flooding, unsolicited advertisements, phishing links, referral scams, or malware in comments or global chat.",
                "Post unflagged major spoilers with the deliberate intent to disrupt other viewers, or impersonate Yuranime administrators, moderators, or other community members.",
                "Attempt to bypass rate limits, tamper with client-side requests, exploit EXP/coin progression bugs, inject malicious payloads into comments or chat, or reverse-engineer protected remote service configurations."
            ),
            postParagraphs = listOf(
                "Yuranime reserves the right to remove offending comments, revoke virtual rewards earned through exploitation, or restrict interactive privileges at any time."
            )
        ),
        LegalSectionContent(
            heading = "Intellectual Property",
            paragraphs = listOf(
                "All original user interface layouts, visual components, Jetpack Compose modules, application source code, database schemas, and the Yuranime name and monochrome wolf emblem are the intellectual property of Yuranime and its contributors, protected under applicable copyright and software laws.",
                "You are granted a limited, non-exclusive, non-transferable, revocable license to use the Yuranime application on a personal Android device solely for non-commercial personal entertainment. User-generated content such as comments and public profile bios remains yours, while you grant Yuranime a non-exclusive license to display and cache that content within the Service."
            )
        ),
        LegalSectionContent(
            heading = "Termination of Access",
            paragraphs = listOf(
                "We may temporarily suspend or permanently terminate your access to the Service, interactive features, or associated cloud profile at any time, with or without prior notice, if we reasonably determine that you have violated these Terms, abused community channels, or posed a security risk to other users.",
                "You may stop using the Service at any time by clearing application storage and uninstalling Yuranime from your device. If you wish to request complete removal of your synchronized account profile and cloud activity records, you may follow the deletion instructions described in our Privacy Policy."
            )
        ),
        LegalSectionContent(
            heading = "Disclaimer of Warranties & Limitation of Liability",
            paragraphs = listOf(
                "The Service is provided strictly on an \"AS IS\" and \"AS AVAILABLE\" basis without warranties of any kind, whether express, implied, or statutory, including implied warranties of merchantability, fitness for a particular purpose, continuous stream availability, or non-infringement.",
                "To the maximum extent permitted by applicable law, Yuranime and its maintainers shall not be liable for any indirect, incidental, special, consequential, or punitive damages, loss of data, watch progress interruption, or temporary third-party server downtime arising from your use of or inability to use the Service."
            )
        ),
        LegalSectionContent(
            heading = "Changes to These Terms",
            paragraphs = listOf(
                "We may revise and update these Terms from time to time to reflect new application capabilities, upstream API changes, community safety policies, or legal requirements. Any updates will be published directly within this page together with an updated \"Last updated\" date at the top.",
                "Your continued access to or use of Yuranime after updated Terms are posted constitutes your binding acceptance of the revised Terms."
            )
        ),
        LegalSectionContent(
            heading = "Governing Law",
            paragraphs = listOf(
                "These Terms of Service and any dispute, claim, or controversy arising out of or relating to the Service shall be governed by and construed in accordance with the laws of the Republic of Indonesia (Hukum Republik Indonesia), without regard to conflict-of-law principles.",
                "Any dispute shall first be addressed through good-faith informal negotiation by contacting admin@yuranime.com. If the matter cannot be resolved amicably within a reasonable timeframe, it shall be submitted to the exclusive jurisdiction of the competent courts in Indonesia."
            )
        ),
        LegalSectionContent(
            heading = "Contact Us",
            paragraphs = listOf(
                "If you have any questions, feedback, copyright inquiries, or legal notices regarding these Terms of Service, please contact the Yuranime team directly at admin@yuranime.com."
            )
        )
    )
}

private fun buildPrivacyPolicySections(): List<LegalSectionContent> {
    return listOf(
        LegalSectionContent(
            heading = "Introduction",
            paragraphs = listOf(
                "Yuranime (\"we\", \"our\", or \"us\") respects and is committed to protecting your privacy. This Privacy Policy explains what information is collected when you use the Yuranime Android mobile application and related cloud services (collectively, the \"Service\"), how that information is used and protected, how long it is retained, and the choices you control.",
                "By accessing or using the Service, you consent to the collection, storage, and processing of information as described in this Privacy Policy and in our Terms of Service."
            )
        ),
        LegalSectionContent(
            heading = "Definitions",
            definitions = listOf(
                "Account" to "Your Yuranime user profile created when you sign in using Google Sign-In.",
                "Device" to "Any Android smartphone, tablet, or compatible hardware running the Yuranime application.",
                "Personal Data" to "Any information relating to an identified or identifiable individual, including your Google name, email address, and unique user identifier.",
                "Service Providers" to "Third-party infrastructure providers used to operate authentication, real-time messaging, media delivery, and crash diagnostics.",
                "We / Us / Our / Yuranime" to "The Yuranime application project and maintainers based in Indonesia.",
                "You" to "The individual accessing or using the Service."
            )
        ),
        LegalSectionContent(
            heading = "Information We Collect",
            paragraphs = listOf(
                "Depending on how you interact with Yuranime, we collect the following categories of data:"
            ),
            bullets = listOf(
                "Google Account Data: When you sign in with Google, we receive your basic public account details provided by Google Identity, specifically your display name, email address, Google account identifier, and profile photo URL.",
                "Profile & Community Data: Information you voluntarily customize or submit inside the app, including your chosen username, bio, profile banner, equipped cosmetic borders and titles, episode comments, replies, and global chat messages.",
                "Watch History & Library Activity: Episode playback timestamps, watched episodes, favorites, bookmark lists, comic reading history, and search keywords so you can resume watching across sessions.",
                "Device & Technical Logs: Basic device brand, Android OS version, API level, application build version, server latency telemetry, notification schedule state, and anonymized diagnostic error logs used to resolve crashes."
            )
        ),
        LegalSectionContent(
            heading = "How We Use Your Information",
            paragraphs = listOf(
                "We use collected information strictly to deliver, maintain, and improve the core functionality of Yuranime:"
            ),
            bullets = listOf(
                "Authenticate your Google session, synchronize your user profile across restarts, and display your public identity in comments, chat rooms, and leaderboards.",
                "Persist episode resume positions, compute watch EXP/level progression, and store favorites and comic reading progress.",
                "Deliver opt-in local Android system notifications when anime titles listed on your release schedule air new episodes.",
                "Moderate spam, abuse, or policy violations in community spaces and protect accounts against automated tampering.",
                "Measure upstream stream latency, troubleshoot video playback bugs, and deliver critical compatibility updates."
            )
        ),
        LegalSectionContent(
            heading = "Data Storage & Security",
            paragraphs = listOf(
                "Yuranime employs a hybrid local and cloud storage architecture designed for speed and resilience:",
                "Local Storage: Preferences, playback quality defaults, local watch history mirrors, offline comic chapters, and temporary image/seekbar frame caches (stored in volatile RAM or private cacheDir) remain stored directly on your device's internal sandboxed storage and are not transmitted unnecessarily.",
                "Cloud Database (Google Firebase): Synchronized account profiles, virtual cosmetic inventories, global chat messages, real-time presence, remote sticker packs, and dynamic remote asset configurations are hosted securely on Google Firebase Authentication, Cloud Firestore, and Firebase Realtime Database over HTTPS/TLS encrypted transport.",
                "While we implement reasonable technical safeguards including role restrictions, null-safe deserialization, and removal of sensitive client-side configuration assets, no internet transmission or cloud database is completely immune from risk. You are encouraged to safeguard your Google credentials and use official builds from trusted Yuranime releases."
            )
        ),
        LegalSectionContent(
            heading = "Third-Party Services & Links",
            paragraphs = listOf(
                "Yuranime integrates trusted third-party infrastructure such as Google Sign-In and Google Firebase, and resolves anime metadata, soundtrack previews, comic chapters, and streaming links hosted on external third-party sources not controlled by Yuranime.",
                "When the player loads a remote media stream or when you open an external announcement link, your IP address and standard HTTP request headers are received directly by those external servers according to their own independent privacy policies. We recommend reviewing the privacy practices of any external site or service you interact with."
            )
        ),
        LegalSectionContent(
            heading = "Data Retention & Account Deletion",
            paragraphs = listOf(
                "We retain your synchronized cloud profile, comments, and watch history for as long as your Account remains active or as needed to provide the Service. Temporary seekbar preview frames are stored only in RAM and are purged automatically when changing episodes or closing the video player, while image and sticker caches can be cleared anytime from Settings.",
                "You can request full deletion of your cloud Account and personal data by emailing admin@yuranime.com from the Google email address associated with your profile. Once verified, we will permanently erase your profile record and watch history documents within 30 calendar days, except where minimal anonymized moderation logs must be retained to enforce community bans."
            )
        ),
        LegalSectionContent(
            heading = "Children's Privacy",
            paragraphs = listOf(
                "The Service is not directed to children under the age of 13, and we do not knowingly collect Personal Data from anyone under 13 years old. If you are a parent or legal guardian and believe your child under 13 has signed in and provided Personal Data to Yuranime, please contact us immediately at admin@yuranime.com so we can promptly locate and delete the associated account data."
            )
        ),
        LegalSectionContent(
            heading = "Changes to This Privacy Policy",
            paragraphs = listOf(
                "We may update this Privacy Policy periodically to reflect improvements to data protection practices, new features such as notification permissions or remote asset loading, or regulatory requirements. All updates take effect when posted inside this screen with an updated \"Last updated\" timestamp.",
                "We encourage you to review this page from time to time. Continued use of Yuranime after revisions become effective indicates your acknowledgment of the updated Privacy Policy."
            )
        ),
        LegalSectionContent(
            heading = "Contact Us",
            paragraphs = listOf(
                "If you have any questions about this Privacy Policy, wish to exercise your data access or deletion rights, or want to report a privacy concern, please reach out to us at admin@yuranime.com."
            )
        )
    )
}
