package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.i18n.AppLanguage
import com.example.ui.components.LedgerCard
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientStart

const val DEVELOPER_FACEBOOK_URL = "https://www.facebook.com/share/1FeowFMRsp/"

fun openDeveloperFacebookProfile(context: Context, uriHandler: androidx.compose.ui.platform.UriHandler) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, DEVELOPER_FACEBOOK_URL.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            uriHandler.openUri(DEVELOPER_FACEBOOK_URL)
        } catch (_: Exception) {
        }
    }
}

@Composable
fun LoginDeveloperCreditRow(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val isBangla = language == AppLanguage.BANGLA

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable { openDeveloperFacebookProfile(context, uriHandler) }
            .testTag("login_developer_md_jim_link")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = buildAnnotatedString {
                    append(if (isBangla) "ডেভেলপার: " else "Developer: ")
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("MD Jim")
                    }
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(5.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "MD Jim Facebook Profile",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperProfileSettingsCard(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val isBangla = language == AppLanguage.BANGLA
    var previewDrawableRes by remember { mutableStateOf<Int?>(null) }

    val sectionTitle = if (isBangla) "ডেভেলপার ও ক্রিয়েটর প্রোফাইল" else "Developer & Creator Profile"
    val roleSubtitle = if (isBangla) {
        "সফটওয়্যার ও মোবাইল অ্যাপ ডেভেলপার • ডিজিটাল ক্রিয়েটর"
    } else {
        "Software & Mobile App Developer • Digital Creator"
    }
    val bioText = if (isBangla) {
        "আধুনিক ও নিরাপদ অ্যান্ড্রয়েড অ্যাপ, ফিনটেক সিস্টেম, বিজনেস ম্যানেজমেন্ট সফটওয়্যার এবং প্রিমিয়াম UI/UX ডিজাইনে দক্ষ একজন প্রফেশনাল ডেভেলপার। আপনার যেকোনো ব্যবসা, প্রতিষ্ঠান বা ব্যক্তিগত আইডিয়াকে আধুনিক অ্যাপ, ওয়েবসাইট কিংবা কাস্টম সফটওয়্যারে রূপান্তর করতে নিখুঁত, দ্রুত এবং বিশ্বস্ত সেবা প্রদান করা হয়।"
    } else {
        "Professional Full-Stack & Android App Developer specializing in modern mobile apps, FinTech systems, business automation software, and high-converting UI/UX design. Dedicated to turning your business vision or personal idea into fast, secure, and world-class digital products."
    }
    val promoHeadline = if (isBangla) {
        "আপনার কি কোনো অ্যাপ, ওয়েবসাইট বা সফটওয়্যার তৈরি করাতে হবে?"
    } else {
        "Need a Custom App, Website, or Software Developed?"
    }
    val promoBody = if (isBangla) {
        "যেকোনো ধরনের অ্যান্ড্রয়েড অ্যাপ, ওয়েবসাইট, ই-কমার্স বা কাস্টম সফটওয়্যার ডেভেলপমেন্ট করাতে এবং প্রজেক্ট নিয়ে সরাসরি কথা বলতে নিচে ক্লিক করে MD Jim-এর অফিসিয়াল ফেসবুক আইডিতে যোগাযোগ করুন।"
    } else {
        "Whether you need an Android application, website, e-commerce platform, or custom software built from scratch—click below to contact MD Jim directly on Facebook."
    }
    val skillsList = if (isBangla) {
        listOf(
            "অ্যান্ড্রয়েড অ্যাপ ডেভেলপমেন্ট",
            "কাস্টম সফটওয়্যার",
            "ওয়েব অ্যাপ্লিকেশন",
            "ফিনটেক ও ই-কমার্স",
            "প্রিমিয়াম UI/UX ডিজাইন"
        )
    } else {
        listOf(
            "Android App Development",
            "Custom Software",
            "Web Applications",
            "FinTech & E-Commerce",
            "Premium UI/UX Design"
        )
    }
    val galleryLabel = if (isBangla) "ক্রিয়েটর গ্যালারি (বড় করে দেখতে ছবিতে ট্যাপ করুন)" else "Creator Gallery (Tap any photo to enlarge)"
    val ctaButtonLabel = if (isBangla) {
        "ডেভেলপমেন্ট করাতে ক্লিক করুন (Facebook - MD Jim)"
    } else {
        "Hire / Contact MD Jim on Facebook"
    }

    LedgerCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("developer_profile_section_card")
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MD Jim",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cover Banner + Overlapping Portrait Avatar
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_dev_md_jim_cover),
                contentDescription = "MD Jim Cover Banner - Focus On Your Goals",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(158.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { previewDrawableRes = R.drawable.img_dev_md_jim_cover }
                    .testTag("developer_cover_image")
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp)
                    .offset(y = 34.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_dev_md_jim_portrait),
                    contentDescription = "Developer MD Jim Portrait",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .clickable { previewDrawableRes = R.drawable.img_dev_md_jim_portrait }
                        .testTag("developer_portrait_avatar")
                )
            }
        }

        Spacer(modifier = Modifier.height(42.dp))

        // Name & Verified Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { openDeveloperFacebookProfile(context, uriHandler) }
        ) {
            Text(
                text = "MD Jim",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified Developer",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = roleSubtitle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Professional Bio
        Text(
            text = bioText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Expertise Pills
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            skillsList.forEach { skill ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = skill,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dual Photo Showcase Gallery (Both uploaded pictures side-by-side)
        Text(
            text = galleryLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_dev_md_jim_portrait),
                contentDescription = "MD Jim Photo 1",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .height(155.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { previewDrawableRes = R.drawable.img_dev_md_jim_portrait }
                    .testTag("developer_gallery_photo_1")
            )
            Image(
                painter = painterResource(id = R.drawable.img_dev_md_jim_cover),
                contentDescription = "MD Jim Photo 2",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .height(155.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { previewDrawableRes = R.drawable.img_dev_md_jim_cover }
                    .testTag("developer_gallery_photo_2")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Promotional Call-To-Action Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd)))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = Color(0xFFFDE68A),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = promoHeadline,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = promoBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE6F4EA)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { openDeveloperFacebookProfile(context, uriHandler) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("developer_facebook_contact_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ctaButtonLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = DEVELOPER_FACEBOOK_URL,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA7F3D0),
                    textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openDeveloperFacebookProfile(context, uriHandler) }
                        .padding(top = 2.dp)
                )
            }
        }
    }

    // Full-screen photo lightbox dialog when user taps either developer photo
    previewDrawableRes?.let { resId ->
        Dialog(
            onDismissRequest = { previewDrawableRes = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { previewDrawableRes = null }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { previewDrawableRes = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = "MD Jim Full Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                    )
                    OutlinedButton(
                        onClick = { openDeveloperFacebookProfile(context, uriHandler) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("MD Jim — Facebook Profile")
                    }
                }
            }
        }
    }
}
