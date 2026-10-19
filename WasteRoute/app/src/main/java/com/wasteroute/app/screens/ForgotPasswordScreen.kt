@file:Suppress("FunctionName")

package com.westeroute.app.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import com.westeroute.app.theme.Emerald500
import com.westeroute.app.theme.NeutralGray400
import com.westeroute.app.theme.NeutralGray500
import com.westeroute.app.theme.NeutralGray600
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private enum class ForgotPasswordStep {
    REQUEST_EMAIL,
    VERIFY_OTP,
    RESET_PASSWORD,
    SUCCESS
}

@Suppress("FunctionName")
@Composable
fun ForgotPasswordScreen(
    userRole: UserRole,
    onNavigateBack: () -> Unit,
    onResetSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    var currentStep by rememberSaveable { mutableStateOf(ForgotPasswordStep.REQUEST_EMAIL) }
    var email by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }

    var otpCode by rememberSaveable { mutableStateOf("") }
    var otpError by rememberSaveable { mutableStateOf<String?>(null) }
    var resendTimer by rememberSaveable { mutableIntStateOf(45) }
    var isTimerRunning by rememberSaveable { mutableStateOf(false) }

    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // OTP Countdown Timer (Email resend cooldown)
    LaunchedEffect(isTimerRunning, resendTimer) {
        if (isTimerRunning && resendTimer > 0) {
            delay(1000.milliseconds)
            resendTimer -= 1
        } else if (resendTimer == 0) {
            isTimerRunning = false
        }
    }

    // Role-based gradient accents
    val isCitizen = userRole == UserRole.CITIZEN
    val primaryAccent = if (isCitizen) Emerald400 else ElectricBlue400
    val primaryGradient = if (isCitizen) {
        Brush.linearGradient(listOf(Emerald500, Cyan400))
    } else {
        Brush.linearGradient(listOf(ElectricBlue500, Cyan400))
    }
    val badgeBorderColor = if (isCitizen) Emerald400.copy(alpha = 0.5f) else ElectricBlue400.copy(alpha = 0.5f)
    val badgeBgColor = if (isCitizen) {
        Brush.horizontalGradient(listOf(Emerald400.copy(alpha = 0.18f), Cyan400.copy(alpha = 0.10f)))
    } else {
        Brush.horizontalGradient(listOf(ElectricBlue400.copy(alpha = 0.18f), Cyan400.copy(alpha = 0.10f)))
    }
    val badgeText = if (isCitizen) "Citizen Password Recovery" else "Worker Password Recovery"

    // Email validation helper
    fun validateEmail(input: String): Boolean {
        val trimmed = input.trim()
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return trimmed.isNotEmpty() && emailRegex.matches(trimmed)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        // Dedicated Top Bar
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
                        onClick = {
                            when (currentStep) {
                                ForgotPasswordStep.REQUEST_EMAIL -> onNavigateBack()
                                ForgotPasswordStep.VERIFY_OTP -> currentStep = ForgotPasswordStep.REQUEST_EMAIL
                                ForgotPasswordStep.RESET_PASSWORD -> currentStep = ForgotPasswordStep.VERIFY_OTP
                                ForgotPasswordStep.SUCCESS -> onResetSuccess()
                            }
                        }
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Main Animated Card
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
                        // Header: Logo & Badge
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
                                    .background(badgeBgColor)
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(badgeBorderColor, Cyan400.copy(alpha = 0.3f))
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = primaryAccent
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Step Indicator Progress Bar (Steps 1 to 3)
                        if (currentStep != ForgotPasswordStep.SUCCESS) {
                            val activeStepIndex = when (currentStep) {
                                ForgotPasswordStep.REQUEST_EMAIL -> 0
                                ForgotPasswordStep.VERIFY_OTP -> 1
                                ForgotPasswordStep.RESET_PASSWORD -> 2
                                ForgotPasswordStep.SUCCESS -> 3
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(3) { stepIdx ->
                                    val isCompletedOrActive = stepIdx <= activeStepIndex
                                    val barColor = if (isCompletedOrActive) primaryAccent else (if (isDark) Color(0x3394A3B8) else Color(0x33CBD5E1))

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(barColor)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Animated Step Transition
                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = {
                                if (targetState.ordinal > initialState.ordinal) {
                                    (slideInHorizontally { width -> width / 2 } + fadeIn(tween(250))) togetherWith
                                            (slideOutHorizontally { width -> -width / 2 } + fadeOut(tween(250)))
                                } else {
                                    (slideInHorizontally { width -> -width / 2 } + fadeIn(tween(250))) togetherWith
                                            (slideOutHorizontally { width -> width / 2 } + fadeOut(tween(250)))
                                }
                            },
                            label = "forgot_password_step_transition"
                        ) { step ->
                            when (step) {
                                ForgotPasswordStep.REQUEST_EMAIL -> {
                                    RequestEmailStep(
                                        email = email,
                                        onEmailChange = {
                                            email = it
                                            if (emailError != null) emailError = null
                                        },
                                        errorMessage = emailError,
                                        isLoading = isLoading,
                                        isDark = isDark,
                                        primaryGradient = primaryGradient,
                                        onSubmit = {
                                            if (!validateEmail(email)) {
                                                emailError = "Please enter a valid email address (e.g. name@example.com)"
                                                return@RequestEmailStep
                                            }
                                            emailError = null
                                            isLoading = true
                                            scope.launch {
                                                delay(1000.milliseconds) // Simulate email dispatch
                                                isLoading = false
                                                resendTimer = 45
                                                isTimerRunning = true
                                                currentStep = ForgotPasswordStep.VERIFY_OTP
                                            }
                                        },
                                        onNavigateBack = onNavigateBack
                                    )
                                }

                                ForgotPasswordStep.VERIFY_OTP -> {
                                    VerifyOtpStep(
                                        email = email,
                                        otpCode = otpCode,
                                        onOtpChange = {
                                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                                otpCode = it
                                                if (otpError != null) otpError = null
                                            }
                                        },
                                        errorMessage = otpError,
                                        resendTimer = resendTimer,
                                        isTimerRunning = isTimerRunning,
                                        isLoading = isLoading,
                                        isDark = isDark,
                                        primaryAccent = primaryAccent,
                                        primaryGradient = primaryGradient,
                                        onResend = {
                                            if (!isTimerRunning) {
                                                isLoading = true
                                                scope.launch {
                                                    delay(800.milliseconds)
                                                    isLoading = false
                                                    resendTimer = 45
                                                    isTimerRunning = true
                                                    otpCode = ""
                                                }
                                            }
                                        },
                                        onChangeEmail = {
                                            currentStep = ForgotPasswordStep.REQUEST_EMAIL
                                        },
                                        onSubmit = {
                                            if (otpCode.length < 6) {
                                                otpError = "Please enter the complete 6-digit code"
                                                return@VerifyOtpStep
                                            }
                                            otpError = null
                                            isLoading = true
                                            scope.launch {
                                                delay(900.milliseconds) // Simulate verification
                                                isLoading = false
                                                currentStep = ForgotPasswordStep.RESET_PASSWORD
                                            }
                                        }
                                    )
                                }

                                ForgotPasswordStep.RESET_PASSWORD -> {
                                    ResetPasswordStep(
                                        newPassword = newPassword,
                                        onNewPasswordChange = {
                                            newPassword = it
                                            if (passwordError != null) passwordError = null
                                        },
                                        confirmPassword = confirmPassword,
                                        onConfirmPasswordChange = {
                                            confirmPassword = it
                                            if (passwordError != null) passwordError = null
                                        },
                                        errorMessage = passwordError,
                                        isLoading = isLoading,
                                        isDark = isDark,
                                        primaryAccent = primaryAccent,
                                        primaryGradient = primaryGradient,
                                        onSubmit = {
                                            when {
                                                newPassword.length < 6 -> {
                                                    passwordError = "Password must be at least 6 characters"
                                                }
                                                newPassword != confirmPassword -> {
                                                    passwordError = "Passwords do not match"
                                                }
                                                else -> {
                                                    passwordError = null
                                                    isLoading = true
                                                    scope.launch {
                                                        delay(1100.milliseconds) // Simulate saving
                                                        isLoading = false
                                                        currentStep = ForgotPasswordStep.SUCCESS
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }

                                ForgotPasswordStep.SUCCESS -> {
                                    SuccessStep(
                                        isDark = isDark,
                                        primaryAccent = primaryAccent,
                                        primaryGradient = primaryGradient,
                                        onSignIn = onResetSuccess
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 1: User enters registered Email address
 */
@Suppress("FunctionName")
@Composable
private fun RequestEmailStep(
    email: String,
    onEmailChange: (String) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    isDark: Boolean,
    primaryGradient: Brush,
    onSubmit: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Forgot Password?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            ),
            color = if (isDark) Color.White else Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Enter your registered email address to receive a secure password reset code.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Email Text Field
        WesteRouteTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "Enter registered email",
            leadingIcon = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444)
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Submit button
        PrimaryButton(
            text = "Send Verification Code",
            onClick = onSubmit,
            isLoading = isLoading,
            brush = primaryGradient
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Return to login link
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onNavigateBack
                )
                .padding(vertical = 6.dp, horizontal = 12.dp)
        ) {
            Text(
                text = "Back to Sign In",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFCBD5E1) else NeutralGray600
                )
            )
        }
    }
}

/**
 * Step 2: 6-Digit Email OTP Verification Code
 */
@Suppress("FunctionName")
@Composable
private fun VerifyOtpStep(
    email: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    errorMessage: String?,
    resendTimer: Int,
    isTimerRunning: Boolean,
    isLoading: Boolean,
    isDark: Boolean,
    primaryAccent: Color,
    primaryGradient: Brush,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
    onSubmit: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mini Email sent icon badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(primaryAccent.copy(alpha = 0.15f))
                .border(1.dp, primaryAccent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MarkEmailRead,
                contentDescription = null,
                tint = primaryAccent,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Verify Email",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            ),
            color = if (isDark) Color.White else Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "We sent a 6-digit code to:",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Normal
            ),
            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
            textAlign = TextAlign.Center
        )

        Text(
            text = email,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = primaryAccent
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Interactive 6-Digit OTP Box Grid
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Hidden real text field for keyboard capture
            BasicTextField(
                value = otpCode,
                onValueChange = onOtpChange,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester),
                cursorBrush = SolidColor(Color.Transparent)
            )

            // 6 Visual Digit Cells
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { focusRequester.requestFocus() }
                )
            ) {
                for (i in 0 until 6) {
                    val char = otpCode.getOrNull(i)?.toString() ?: ""
                    val isCurrent = i == otpCode.length

                    val cellBorderColor = when {
                        isCurrent -> primaryAccent
                        char.isNotEmpty() -> primaryAccent.copy(alpha = 0.7f)
                        isDark -> Color(0x3394A3B8)
                        else -> Color(0x3364748B)
                    }

                    val cellBgColor = if (isDark) {
                        if (isCurrent) Color(0x331E293B) else Color(0x1F1E293B)
                    } else {
                        if (isCurrent) Color(0xF5FFFFFF) else Color(0xD9FFFFFF)
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp, 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cellBgColor)
                            .border(
                                width = if (isCurrent) 1.8.dp else 1.dp,
                                color = cellBorderColor,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        )
                    }
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444)
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Timer & Resend Link
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isTimerRunning) {
                Text(
                    text = "Resend code in ",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else NeutralGray500
                    )
                )
                Text(
                    text = "0:${resendTimer.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent
                    )
                )
            } else {
                Text(
                    text = "Didn't receive email? ",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else NeutralGray500
                    )
                )
                Text(
                    text = "Resend Code",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onResend
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Submit Button
        PrimaryButton(
            text = "Verify Code",
            onClick = onSubmit,
            isLoading = isLoading,
            brush = primaryGradient
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Change Email Option
        Text(
            text = "Change email address",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFF94A3B8) else NeutralGray600
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onChangeEmail
                )
                .padding(4.dp)
        )
    }
}

/**
 * Step 3: New Password Creation with Strength Indicator
 */
@Suppress("FunctionName")
@Composable
private fun ResetPasswordStep(
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    isDark: Boolean,
    primaryAccent: Color,
    primaryGradient: Brush,
    onSubmit: () -> Unit
) {
    // Password strength calculation
    val hasMinLength = newPassword.length >= 8
    val hasNumber = newPassword.any { it.isDigit() }
    val hasUpperOrSpecial = newPassword.any { it.isUpperCase() } || newPassword.any { !it.isLetterOrDigit() }

    val strengthScore = (if (hasMinLength) 1 else 0) + (if (hasNumber) 1 else 0) + (if (hasUpperOrSpecial) 1 else 0)

    val (strengthText, strengthColor, strengthFraction) = when {
        newPassword.isEmpty() -> Triple("", Color.Transparent, 0f)
        strengthScore == 1 -> Triple("Weak password", Color(0xFFEF4444), 0.33f)
        strengthScore == 2 -> Triple("Moderate password", Color(0xFFF59E0B), 0.66f)
        else -> Triple("Strong password", Emerald400, 1.0f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Set New Password",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            ),
            color = if (isDark) Color.White else Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Create a strong new password for your WesteRoute account.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // New Password Field
        PasswordField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            placeholder = "Enter new password"
        )

        // Password Strength Visual Indicator
        if (newPassword.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Password strength:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = if (isDark) NeutralGray400 else NeutralGray500
                        )
                    )
                    Text(
                        text = strengthText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = strengthColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isDark) Color(0x3394A3B8) else Color(0x33CBD5E1))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(strengthFraction)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(strengthColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Confirm Password Field
        PasswordField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = "Confirm new password"
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444)
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Submit Button
        PrimaryButton(
            text = "Reset Password",
            onClick = onSubmit,
            isLoading = isLoading,
            brush = primaryGradient
        )
    }
}

/**
 * Step 4: Success & Celebration Card
 */
@Suppress("FunctionName")
@Composable
private fun SuccessStep(
    isDark: Boolean,
    primaryAccent: Color,
    primaryGradient: Brush,
    onSignIn: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "success_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Celebration Glow Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(primaryAccent.copy(alpha = 0.15f))
                .border(2.dp, primaryAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Success",
                tint = primaryAccent,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Password Reset Complete!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            ),
            color = if (isDark) Color.White else Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your password has been updated securely. You can now sign in to your WesteRoute account with your new credentials.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Normal
            ),
            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(26.dp))

        PrimaryButton(
            text = "Sign In Now",
            onClick = onSignIn,
            brush = primaryGradient
        )
    }
}
