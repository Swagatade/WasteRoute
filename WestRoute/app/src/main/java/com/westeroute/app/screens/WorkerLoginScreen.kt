package com.westeroute.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Shield
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.westeroute.app.auth.FirebaseAuthManager
import com.westeroute.app.auth.UserSessionManager
import com.westeroute.app.components.GlassCard
import com.westeroute.app.components.PasswordField
import com.westeroute.app.components.PrimaryButton
import com.westeroute.app.components.WesteRouteLogo
import com.westeroute.app.components.WesteRouteTextField
import com.westeroute.app.model.UserRole
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.ElectricBlue400
import com.westeroute.app.theme.ElectricBlue500
import com.westeroute.app.theme.Emerald400
import com.westeroute.app.theme.NeutralGray400
import com.westeroute.app.theme.NeutralGray500
import com.westeroute.app.theme.NeutralGray600
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Suppress("FunctionName")
@Composable
fun WorkerLoginScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToForgotPassword: () -> Unit = {},
    onSignInSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var workerId by rememberSaveable { mutableStateOf("") }
    var workerEmail by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(true) }

    var workerIdError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var authErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    var isVisible by remember { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isNavigating by remember { mutableStateOf(false) }

    val domain = FirebaseAuthManager.REQUIRED_WORKER_DOMAIN // "@worker.in"

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
        val cleanId = workerId.trim()
        val cleanEmail = workerEmail.trim().lowercase()

        if (cleanId.isEmpty()) {
            workerIdError = "Worker ID is required (e.g. WRK-106 or WR-W-204)"
            isValid = false
        } else {
            workerIdError = null
        }

        if (cleanEmail.isEmpty()) {
            emailError = "Registered worker email is required"
            isValid = false
        } else if (!cleanEmail.endsWith(domain)) {
            emailError = "Worker email must end with special domain '$domain' (e.g. name$domain)"
            isValid = false
        } else if (cleanEmail.length <= domain.length) {
            emailError = "Please enter your full email address (e.g. ${cleanId.lowercase().ifBlank { "worker" }}$domain)"
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

    fun handleSignIn() {
        keyboardController?.hide()
        authErrorMessage = null

        if (!validateInputs()) return
        if (isLoading) return

        isLoading = true
        scope.launch {
            val result = FirebaseAuthManager.signInWorker(
                workerId = workerId.trim(),
                email = workerEmail.trim().lowercase(),
                password = password,
                context = context
            )

            isLoading = false
            result.onSuccess {
                onSignInSuccess()
            }.onFailure { err ->
                authErrorMessage = err.message ?: "Worker authentication failed."
            }
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
        if (UserSessionManager.isLoggedIn(context) && UserSessionManager.getUserRole(context) == UserRole.WORKER) {
            onSignInSuccess()
        }
    }

    val isDark = isSystemInDarkTheme()
    val isDomainMatched = workerEmail.trim().lowercase().endsWith(domain) && workerEmail.trim().length > domain.length

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        // Dedicated Top Bar with Guaranteed 48dp Touch Target and High Z-Index
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
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(400)
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
                    maxWidth = 440.dp,
                    cornerRadius = 28.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header: Mini Logo + Worker Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            WesteRouteLogo(
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
                                                ElectricBlue400.copy(alpha = if (isDark) 0.22f else 0.15f),
                                                Cyan400.copy(alpha = if (isDark) 0.15f else 0.10f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                ElectricBlue400.copy(alpha = 0.5f),
                                                Cyan400.copy(alpha = 0.3f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Worker Portal",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) ElectricBlue400 else Color(0xFF1D4ED8)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Headings
                        Text(
                            text = "Worker Login",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            ),
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Only registered workers provisioned by supervisors can log in.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )

                        // Error Banner
                        AnimatedVisibility(
                            visible = authErrorMessage != null,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut()
                        ) {
                            authErrorMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x26EF4444))
                                        .border(1.dp, Color(0x66EF4444), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                            tint = Color(0xFFF87171),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFF87171),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.5.sp,
                                                lineHeight = 16.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Field 1: Worker ID
                        WesteRouteTextField(
                            value = workerId,
                            onValueChange = {
                                workerId = it.uppercase()
                                workerIdError = null
                                authErrorMessage = null
                            },
                            placeholder = "Enter Worker ID (e.g. WRK-106)",
                            leadingIcon = Icons.Outlined.Badge,
                            errorMessage = workerIdError
                        )

                        // Quick-fill chip if workerId is typed and email is empty
                        if (workerId.isNotBlank() && workerEmail.isBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = "Tap to use: ${workerId.lowercase()}$domain",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isDark) Cyan400 else Color(0xFF0284C7),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDark) Color(0x2606B6D4) else Color(0x1A0284C7))
                                        .clickable {
                                            workerEmail = "${workerId.lowercase()}$domain"
                                            emailError = null
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Field 2: Special Worker Email (@worker.in)
                        WesteRouteTextField(
                            value = workerEmail,
                            onValueChange = {
                                workerEmail = it
                                emailError = null
                                authErrorMessage = null
                            },
                            placeholder = "Worker email (e.g. rahul$domain)",
                            leadingIcon = Icons.Outlined.Email,
                            errorMessage = emailError
                        )

                        // Domain Status Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Special domain: $domain",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) NeutralGray400 else NeutralGray500
                                )
                            )

                            if (isDomainMatched) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Emerald400,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Valid $domain",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald400
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Field 3: Password
                        PasswordField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                                authErrorMessage = null
                            },
                            placeholder = "Enter worker password",
                            errorMessage = passwordError
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Remember Me & Forgot Password Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Remember Me Checkbox
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
                                            if (rememberMe) ElectricBlue400 else (if (isDark) Color(0x331E293B) else Color(0x33CBD5E1))
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (rememberMe) ElectricBlue400 else (if (isDark) Color(0x6694A3B8) else Color(0x6664748B)),
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
                                    color = if (isDark) ElectricBlue400 else Color(0xFF2563EB)
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

                        Spacer(modifier = Modifier.height(22.dp))

                        // Primary Action: Sign In
                        PrimaryButton(
                            text = "Sign in with Firebase",
                            isLoading = isLoading,
                            brush = Brush.linearGradient(
                                listOf(
                                    ElectricBlue500,
                                    Cyan400
                                )
                            ),
                            onClick = { handleSignIn() }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Authorized Personnel Notice
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = if (isDark) ElectricBlue400.copy(alpha = 0.8f) else Color(0xFF3B82F6),
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "Only registered workers with $domain accounts can log in",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFCBD5E1) else NeutralGray500
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
