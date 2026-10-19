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
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Phone
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

@Suppress("FunctionName")
@Composable
fun CitizenRegisterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    onRegisterSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var agreeTerms by rememberSaveable { mutableStateOf(true) }

    var fullNameError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmPasswordError by rememberSaveable { mutableStateOf<String?>(null) }
    var termsError by rememberSaveable { mutableStateOf<String?>(null) }
    var authErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isNoGoogleAccountError by rememberSaveable { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isGoogleLoading by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val emailRegex = remember { Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$") }

    fun validateForm(): Boolean {
        var isValid = true

        if (fullName.trim().isEmpty()) {
            fullNameError = "Full name is required"
            isValid = false
        } else {
            fullNameError = null
        }

        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) {
            emailError = "Email address is required"
            isValid = false
        } else if (!emailRegex.matches(cleanEmail)) {
            emailError = "Please enter a valid email address"
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

        if (confirmPassword.isEmpty()) {
            confirmPasswordError = "Please confirm your password"
            isValid = false
        } else if (confirmPassword != password) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        } else {
            confirmPasswordError = null
        }

        if (!agreeTerms) {
            termsError = "Please accept the Terms & Conditions to proceed"
            isValid = false
        } else {
            termsError = null
        }

        return isValid
    }

    fun handleGoogleSignUp() {
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
                onRegisterSuccess()
            }.onFailure { error ->
                if (error !is UserCancelledException) {
                    isNoGoogleAccountError = (error is NoGoogleAccountException)
                    authErrorMessage = error.message ?: "Google Sign-Up failed. Please try again."
                }
            }
        }
    }

    fun handleCreateAccount() {
        keyboardController?.hide()
        authErrorMessage = null
        isNoGoogleAccountError = false
        if (!validateForm()) return
        if (isLoading || isGoogleLoading) return

        isLoading = true
        scope.launch {
            val result = FirebaseAuthManager.registerWithEmail(
                email = email.trim(),
                password = password,
                fullName = fullName.trim(),
                context = context
            )
            isLoading = false
            result.onSuccess { citizenUser ->
                UserSessionManager.saveCitizenSession(context, citizenUser)
                onRegisterSuccess()
            }.onFailure { error ->
                authErrorMessage = error.message ?: "Failed to create account"
            }
        }
    }


    LaunchedEffect(Unit) {
        isVisible = true
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
        // Dedicated Top Bar with High Z-Index
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
                        onClick = onNavigateBack
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back to login",
                    tint = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Register Card with animated entry
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
                        // Header: Mini Logo + Citizen Registration Badge
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
                                    text = "Citizen Registration",
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
                            text = "Create Account",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            ),
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Join WasteRoute to keep your city clean.",
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
                                    .padding(bottom = 14.dp)
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
                                                text = if (isNoGoogleAccountError) "Google Account Notice" else "Registration Notice",
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

                        // Fast Google Sign-up Option
                        SocialAuthButton(
                            provider = SocialProvider.GOOGLE,
                            text = "Sign up with Google",
                            isLoading = isGoogleLoading,
                            enabled = !isLoading && !isGoogleLoading,
                            onClick = { handleGoogleSignUp() }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = if (isDark) Color(0x3394A3B8) else Color(0x33CBD5E1)
                            )
                            Text(
                                text = "  or register with email  ",
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

                        // Input 1: Full Name
                        WasteRouteTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                fullNameError = null
                            },
                            placeholder = "Full name",
                            leadingIcon = Icons.Outlined.PersonOutline,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            isError = fullNameError != null,
                            errorMessage = fullNameError
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Input 2: Email Address
                        WasteRouteTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = null
                            },
                            placeholder = "Email address",
                            leadingIcon = Icons.Outlined.Email,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            isError = emailError != null,
                            errorMessage = emailError
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Input 3: Mobile Number (Optional)
                        WasteRouteTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            placeholder = "Mobile number (optional)",
                            leadingIcon = Icons.Outlined.Phone,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Input 4: Password
                        PasswordField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                            },
                            placeholder = "Create password (min 6 chars)",
                            keyboardActions = KeyboardActions(onDone = {}),
                            isError = passwordError != null,
                            errorMessage = passwordError
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Input 5: Confirm Password
                        PasswordField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                confirmPasswordError = null
                            },
                            placeholder = "Confirm password",
                            keyboardActions = KeyboardActions(onDone = { handleCreateAccount() }),
                            isError = confirmPasswordError != null,
                            errorMessage = confirmPasswordError
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Terms & Conditions Checkbox Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Checkbox,
                                    onClick = {
                                        agreeTerms = !agreeTerms
                                        termsError = null
                                    }
                                )
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(19.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        if (agreeTerms) Emerald400 else (if (isDark) Color(0x331E293B) else Color(0x33CBD5E1))
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (agreeTerms) Emerald400 else (if (isDark) Color(0x6694A3B8) else Color(0x6664748B)),
                                        shape = RoundedCornerShape(5.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (agreeTerms) {
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
                                text = "I agree to the Terms & Conditions",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFCBD5E1) else NeutralGray600
                                )
                            )
                        }

                        if (termsError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = termsError ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.align(Alignment.Start)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Primary Action: Create Account Button
                        PrimaryButton(
                            text = "Create Account",
                            isLoading = isLoading,
                            enabled = !isLoading && !isGoogleLoading,
                            onClick = { handleCreateAccount() }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Already have an account link
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Already have an account? ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFCBD5E1) else NeutralGray500
                                )
                            )
                            Text(
                                text = "Sign in",
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
                                        onClick = onNavigateToLogin
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
