package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.GoogleSignInButton
import com.example.ui.components.DorbarLogoBadge
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NotoBengaliFontFamily
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.WarmGold

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDeep, Color(0xFF1E1B4B), MaterialTheme.colorScheme.background)
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(26.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_welcome_hero),
                            contentDescription = "Dorbar Learning Sanctuary",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, NavyDeep.copy(alpha = 0.85f))
                                    )
                                )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(18.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_dorbar_emblem),
                                contentDescription = "দরবার logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "দরবার",
                                    fontFamily = NotoBengaliFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 34.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Your Digital Learning & Knowledge Hub",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = WarmGold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Sign in to continue",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Connect securely to access all six subject libraries, offline study downloads, AI GK Challenge, and multi-admin sync.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            AuthFeatureRow(
                                icon = Icons.Default.LibraryBooks,
                                title = "6 Subject Libraries",
                                desc = "Bengali, English, Physics, Chemistry, Maths & Biology"
                            )
                            AuthFeatureRow(
                                icon = Icons.Default.AutoAwesome,
                                title = "দরবার AI GK Challenge",
                                desc = "12 GK categories powered by verified Gemini AI questions"
                            )
                            AuthFeatureRow(
                                icon = Icons.Default.Shield,
                                title = "Protected Student & Admin Sync",
                                desc = "Role-verified access & private student activity history"
                            )

                            if (!errorMsg.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = errorMsg!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            GoogleSignInButton(
                                onAuthSuccess = {
                                    errorMsg = null
                                    onAuthSuccess()
                                },
                                onAuthError = { msg -> errorMsg = msg }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(9.dp)
                    .size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StudentNameGateScreen(
    cachedName: String,
    isAdminAuthorized: Boolean,
    onSubmitName: (String) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember(cachedName) { mutableStateOf(cachedName) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DorbarLogoBadge(subtitle = "Student Entry • Name Only")
                    if (isAdminAuthorized) {
                        Surface(
                            color = RoyalPurple.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Verified",
                                    tint = RoyalPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Admin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoyalPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_welcome_hero),
                        contentDescription = "Student Welcome",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NavyDeep.copy(alpha = 0.88f), IndigoPrimary.copy(alpha = 0.45f))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(modifier = Modifier.align(Alignment.CenterStart)) {
                            Text(
                                text = "স্বাগতম! Welcome to দরবার",
                                fontFamily = NotoBengaliFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Simple Student Entry — just enter your full name below. No separate email, phone number, or password needed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your Unique Student ID is created automatically",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                validationError = null
                            },
                            label = { Text("Student Full Name") },
                            placeholder = { Text("e.g., Ananya Chatterjee") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            isError = validationError != null,
                            supportingText = validationError?.let { err -> { Text(err) } },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_name_input")
                        )

                        Button(
                            onClick = {
                                val trimmed = fullName.trim()
                                if (trimmed.length < 2) {
                                    validationError = "Please enter your full name (at least 2 characters)."
                                } else {
                                    onSubmitName(trimmed)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("student_continue_button")
                        ) {
                            Text(
                                text = "Continue to Dashboard",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        if (isAdminAuthorized) {
                            OutlinedButton(
                                onClick = onOpenAdminPanel,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("open_admin_direct_button")
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Admin Control Center")
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Switch Google Account")
                        }
                    }
                }
            }
        }
    }
}
