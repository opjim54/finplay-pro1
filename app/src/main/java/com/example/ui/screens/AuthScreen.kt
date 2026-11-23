package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.i18n.AppLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.LedgerCard
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientStart

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AuthScreen(
    currentLanguage: AppLanguage,
    authError: String?,
    authBusy: Boolean = false,
    driveBusy: Boolean,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogin: (username: String, password: String, remember: Boolean) -> Unit,
    onRegister: (username: String, password: String, displayName: String, question: String, answer: String) -> Unit,
    onForgotPassword: (username: String, answer: String, newPassword: String) -> Unit,
    onAdminLogin: (adminKey: String) -> Unit,
    onRestoreFromDrive: () -> Unit,
    onClearError: () -> Unit
) {
    val strings = LocalStrings.current
    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var secQuestion by remember { mutableStateOf("Favorite city?") }
    var secAnswer by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Instant Language Switcher Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                onLanguageChange(
                                    if (currentLanguage == AppLanguage.BANGLA) AppLanguage.ENGLISH
                                    else AppLanguage.BANGLA
                                )
                            }
                            .testTag("auth_language_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = strings.languageLabel,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentLanguage.flagLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Hero Header with Long-Press Hidden Admin Entry on Logo
                var heroLaunched by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { heroLaunched = true }
                val logoScale by animateFloatAsState(
                    targetValue = if (heroLaunched) 1f else 0.90f,
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                    label = "authLogoScale"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd)))
                        .padding(vertical = 24.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.smart_finance_life_logo_1794994103358),
                            contentDescription = strings.appTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(82.dp)
                                .scale(logoScale)
                                .clip(RoundedCornerShape(22.dp))
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        onClearError()
                                        showAdminDialog = true
                                    }
                                )
                                .testTag("app_logo_admin_trigger")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.appTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD1FAE5)
                        )
                    }
                }

                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isRegisterMode) strings.registerAccount else strings.loginTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (!isRegisterMode) {
                        Text(
                            text = strings.defaultCredentialsHint,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedVisibility(visible = isRegisterMode) {
                        Column {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text(strings.displayName) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_display_name")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            onClearError()
                        },
                        label = { Text(strings.username) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_username_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            onClearError()
                        },
                        label = { Text(strings.password) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    AnimatedVisibility(visible = isRegisterMode) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = secQuestion,
                                onValueChange = { secQuestion = it },
                                label = { Text(strings.securityQuestion) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = secAnswer,
                                onValueChange = { secAnswer = it },
                                label = { Text(strings.securityAnswer) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (!isRegisterMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { rememberMe = !rememberMe }
                            ) {
                                Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                                Text(strings.rememberMe, style = MaterialTheme.typography.bodyMedium)
                            }
                            TextButton(onClick = { showForgotDialog = true }) {
                                Text(strings.forgotPassword)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (!authError.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = authError,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                if (username.isNotBlank() && password.isNotBlank()) {
                                    onRegister(username, password, displayName, secQuestion, secAnswer)
                                }
                            } else {
                                onLogin(username, password, rememberMe)
                            }
                        },
                        enabled = !authBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button")
                    ) {
                        if (authBusy && !showAdminDialog) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isRegisterMode) strings.createAccount else strings.signIn,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            onClearError()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(if (isRegisterMode) strings.signIn else strings.registerAccount)
                    }
                }

                // Reinstall Google Drive Recovery directly on Login screen
                OutlinedButton(
                    onClick = onRestoreFromDrive,
                    enabled = !driveBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_drive_restore_button")
                ) {
                    if (driveBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.restoreFromDriveLogin)
                }
                Text(
                    text = strings.newPhoneRestoreHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Subtle clickable Developer credit redirecting to MD Jim's Facebook profile
                LoginDeveloperCreditRow(
                    language = currentLanguage,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    if (showForgotDialog) {
        var fpUser by remember { mutableStateOf(username) }
        var fpAns by remember { mutableStateOf("") }
        var fpNewPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text(strings.forgotPasswordTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fpUser,
                        onValueChange = { fpUser = it },
                        label = { Text(strings.username) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fpAns,
                        onValueChange = { fpAns = it },
                        label = { Text("${strings.securityAnswer} (e.g. dhaka)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fpNewPass,
                        onValueChange = { fpNewPass = it },
                        label = { Text(strings.newPassword) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (fpUser.isNotBlank() && fpNewPass.isNotBlank()) {
                        onForgotPassword(fpUser, fpAns, fpNewPass)
                        showForgotDialog = false
                    }
                }) {
                    Text(strings.resetPassword)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showAdminDialog) {
        var adminKey by remember { mutableStateOf("") }
        var adminKeyVisible by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!authBusy) showAdminDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                }
            },
            title = { Text(strings.adminPortalTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = adminKey,
                        onValueChange = {
                            adminKey = it
                            onClearError()
                        },
                        label = { Text(strings.adminKeyLabel) },
                        enabled = !authBusy,
                        trailingIcon = {
                            IconButton(onClick = { adminKeyVisible = !adminKeyVisible }) {
                                Icon(
                                    imageVector = if (adminKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (adminKeyVisible) "Hide key" else "Show key"
                                )
                            }
                        },
                        visualTransformation = if (adminKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!authBusy && adminKey.isNotBlank()) {
                                    onAdminLogin(adminKey)
                                }
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_key_input")
                    )
                    if (authBusy) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Verifying encrypted Admin key...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (!authError.isNullOrBlank()) {
                        Text(
                            text = authError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("admin_login_error_text")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onAdminLogin(adminKey) },
                    enabled = !authBusy && adminKey.isNotBlank(),
                    modifier = Modifier.testTag("admin_login_submit")
                ) {
                    if (authBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(strings.enterAdminPortal)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAdminDialog = false },
                    enabled = !authBusy
                ) { Text(strings.cancel) }
            }
        )
    }
}
