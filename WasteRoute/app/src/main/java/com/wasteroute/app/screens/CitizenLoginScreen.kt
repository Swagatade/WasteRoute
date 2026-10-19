package com.wasteroute.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.wasteroute.app.auth.FirebaseAuthManager
import com.wasteroute.app.auth.NoGoogleAccountException
import com.wasteroute.app.auth.UserCancelledException
import com.wasteroute.app.auth.UserSessionManager
import com.wasteroute.app.components.GlassCard
import com.wasteroute.app.components.PasswordField
import com.wasteroute.app.components.PrimaryButton
import com.wasteroute.app.components.SocialAuthButton
import com.wasteroute.app.components.SocialProvider
import com.wasteroute.app.components.WasteRouteLogo
import com.wasteroute.app.components.WasteRouteTextField
import com.wasteroute.app.model.CitizenAuthSession
import com.wasteroute.app.theme.Cyan400
import com.wasteroute.app.theme.Emerald400
import com.wasteroute.app.theme.Emerald500
import com.wasteroute.app.theme.NeutralGray400
import com.wasteroute.app.theme.NeutralGray500
import com.wasteroute.app.theme.NeutralGray600
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Suppress("FunctionName")
@Composable
fun CitizenLoginScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToRegister: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onSignInSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var authErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isNoGoogleAccountError by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(true) }

    var isVisible by remember { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isGoogleLoading by rememberSaveable { mutableStateOf(false) }
    var isNavigating by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val emailRegex = remember { Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$") }

    fun safeNavigate(action: () -> Unit, delayMs: Long = 260L) {
        if (isNavigating) return
        isNavigating = true
        scope.launch {
            delay(delayMs.milliseconds)
            action()
            isNavigating = false
        }
    }

    fun validateInputs(): Boolean {
        var isValid = true
        val cleanEmail = email.trim()

        if (cleanEmail.isEmpty()) {
            emailError = "Email address is required"
            isValid = false
        } else if (!emailRegex.matches(cleanEmail)) {
            emailError = "Please enter a valid email (e.g. name@example.com)"
            isValid = false
        } else {
            emailError = null
        }

        if (password.isEmpty()) {
            passwordError = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordError = null
        }

        return isValid
    }

    fun handleGoogleSignIn() {
        keyboardController?.hide()
        authErrorMessage = null
        isNoGoogleAccountError = false
        if (isLoading || isGoogleLoading) return

        isGoogleLoading = true
        scope.launch {
            val result = FirebaseAuthManager.signInWithGoogle(context)
            isGoogleLoading = false
            result.onSuccess { citizenUser ->
                UserSessionManager.saveCitizenSession(context, citizenUser)
                onSignInSuccess()
            }.onFailure { error ->
                if (error !is UserCancelledException) {
                    isNoGoogleAccountError = (error is NoGoogleAccountException)
                    authErrorMessage = error.message ?: "Google Sign-In failed. Please try again."
                }
            }
        }
    }

    fun handleEmailPasswordSignIn() {
        keyboardController?.hide()
        authErrorMessage = null
        isNoGoogleAccountError = false

        if (!validateInputs()) return
        if (isLoading || isGoogleLoading) return

        isLoading = true
        scope.launch {
            val result = FirebaseAuthManager.signInWithEmail(email.trim(), password, context)
            isLoading = false
            result.onSuccess { citizenUser ->
                UserSessionManager.saveCitizenSession(context, citizenUser)
                onSignInSuccess()
            }.onFailure { error ->
                authErrorMessage = error.message ?: "Authentication failed"
            }
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
        // A signed-in worker (@worker.in) is never auto-signed in as a citizen
        val live = FirebaseAuthManager.getCurrentUser()
        val existing = if (live?.email?.endsWith(FirebaseAuthManager.REQUIRED_WORKER_DOMAIN, ignoreCase = true) == true) null
            else live ?: UserSessionManager.restoreSessionIfAvailable(context)
        if (existing != null) {
            CitizenAuthSession.setSession(existing)
            UserSessionManager.saveCitizenSession(context, existing)
            onSignInSuccess()
        }
    }

    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        // Dedicated Top Bar with Guaranteed Touch Target and High Z-Index
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .zIndex(100f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x4D1E293B) else Color(0xF2FFFFFF))
                    .border(
                        width = 1.2.dp,
                        color = if (isDark) Color(0x4D94A3B8) else Color(0x4DCBD5E1),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 24.dp),
                        role = Role.Button,
                        onClick = { safeNavigate(onNavigateBack) }
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back to role selection",
                    tint = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Login Card with animated entry
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = tween(300)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 64.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                GlassCard(
                    maxWidth = 420.dp,
                    cornerRadius = 28.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header: Mini Logo + Citizen Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            WasteRouteLogo(
                                size = 36.dp,
                                showGlow = false
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                Emerald400.copy(alpha = if (isDark) 0.22f else 0.15f),
                                                Cyan400.copy(alpha = if (isDark) 0.15f else 0.10f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                Emerald400.copy(alpha = 0.5f),
                                                Cyan400.copy(alpha = 0.3f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Citizen Access",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Emerald400 else Color(0xFF047857)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Headings
                        Text(
                            text = "Citizen Login",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            ),
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Sign in with Google or your email & password.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )


                        // Error Banner if present
                        AnimatedVisibility(
                            visible = authErrorMessage != null,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x26EF4444))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isNoGoogleAccountError) "Google Account Notice" else "Sign-in Notice",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = Color(0xFFEF4444),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = authErrorMessage ?: "",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 12.5.sp,
                                                    lineHeight = 17.sp
                                                )
                                            )
                                        }
                                    }

                                    if (isNoGoogleAccountError) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        FirebaseAuthManager.openAddAccountSettings(context)
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "Open Account Settings",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFFEF4444),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // SECTION 1: GOOGLE AUTHENTICATION
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SocialAuthButton(
                                provider = SocialProvider.GOOGLE,
                                text = "Continue with Google",
                                isLoading = isGoogleLoading,
                                enabled = !isLoading && !isGoogleLoading,
                                onClick = {
                                    handleGoogleSignIn()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Divider between Google Auth and Email Auth
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = if (isDark) Color(0x3394A3B8) else Color(0x33CBD5E1)
                            )
                            Text(
                                text = "  or sign in with email  ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) NeutralGray500 else NeutralGray400,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = if (isDark) Color(0x3394A3B8) else Color(0x33CBD5E1)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // SECTION 2: EMAIL & PASSWORD AUTHENTICATION
                        // Input 1: Email Address
                        WasteRouteTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = null
                                authErrorMessage = null
                            },
                            placeholder = "Enter your email address",
                            leadingIcon = Icons.Outlined.Email,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            isError = emailError != null,
                            errorMessage = emailError
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Input 2: Password
                        PasswordField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                                authErrorMessage = null
                            },
                            placeholder = "Enter your password",
                            keyboardActions = KeyboardActions(
                                onDone = { handleEmailPasswordSignIn() }
                            ),
                            isError = passwordError != null,
                            errorMessage = passwordError
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Remember Me & Forgot Password Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Remember Me custom toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        role = Role.Checkbox,
                                        onClick = { rememberMe = !rememberMe }
                                    )
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(19.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            if (rememberMe) Emerald400 else (if (isDark) Color(0x331E293B) else Color(0x33CBD5E1))
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (rememberMe) Emerald400 else (if (isDark) Color(0x6694A3B8) else Color(0x6664748B)),
                                            shape = RoundedCornerShape(5.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (rememberMe) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF090D16),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Remember me",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFCBD5E1) else NeutralGray600
                                    )
                                )
                            }

                            // Forgot Password Link
                            Text(
                                text = "Forgot password?",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Cyan400 else Color(0xFF0891B2)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        role = Role.Button,
                                        onClick = { safeNavigate(onNavigateToForgotPassword) }
                                    )
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Primary Action: Sign in with Email
                        PrimaryButton(
                            text = "Sign in with Email",
                            isLoading = isLoading,
                            enabled = !isLoading && !isGoogleLoading,
                            onClick = { handleEmailPasswordSignIn() }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Create Account Link
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Don't have an account? ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFCBD5E1) else NeutralGray500
                                )
                            )
                            Text(
                                text = "Create account",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Emerald400 else Emerald500
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        role = Role.Button,
                                        onClick = { safeNavigate(onNavigateToRegister) }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
