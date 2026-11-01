package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import android.widget.Toast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.example.data.model.UserEntity
import com.example.ui.SurveyViewModel
import com.example.ui.components.EnqueteurAvatar
import com.example.ui.components.pressScaleEffect

/**
 * Clean Underline Input Field matching the requested design:
 * An icon on the left, clear placeholder/text, optional trailing icon,
 * and a sleek horizontal underline spanning the width.
 */
@Composable
fun CleanUnderlineInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                leadingIcon()
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = if (isError) Color(0xFFEF4444) else Color(0xFF4B5563),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF1E293B),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(if (isError) Color(0xFFDC2626) else Color(0xFF0284C7)),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        }
                        .testTag(testTag)
                )
            }

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingIcon()
            }
        }

        // Horizontal Underline
        HorizontalDivider(
            thickness = if (isFocused || isError) 1.5.dp else 1.dp,
            color = when {
                isError -> Color(0xFFDC2626)
                isFocused -> Color(0xFF0284C7)
                else -> Color(0xFF94A3B8).copy(alpha = 0.5f)
            }
        )

        // Error message animated below the line
        AnimatedVisibility(
            visible = isError && !errorMessage.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                .padding(start = 4.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = Color(0xFFDC2626),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: SurveyViewModel,
    modifier: Modifier = Modifier
) {
    var identifier by remember { mutableStateOf(viewModel.sessionManager.getLastIdentifier()) }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(viewModel.sessionManager.isRememberMe()) }
    var localError by remember { mutableStateOf<String?>(null) }

    val isLoggingIn by viewModel.isLoggingIn.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val statusChangeNotification by viewModel.statusChangeNotification.collectAsState()
    val deactivationNotice by viewModel.deactivationNotice.collectAsState()

    val displayedError = localError ?: loginError

    val hasPasswordSpecificError = displayedError != null && (
        displayedError.contains("mot de passe", ignoreCase = true) ||
        displayedError.contains("password", ignoreCase = true)
    ) && !displayedError.contains("identifiant et votre mot de passe", ignoreCase = true)

    val hasIdentifierSpecificError = displayedError != null && (
        displayedError.contains("identifiant ou email", ignoreCase = true) ||
        displayedError.contains("identifiant requis", ignoreCase = true) ||
        displayedError.contains("email", ignoreCase = true) ||
        displayedError.contains("désactivé", ignoreCase = true)
    )

    val isGeneralOrBothError = displayedError != null && !hasPasswordSpecificError && !hasIdentifierSpecificError

    val identifierErrorMessage: String? = when {
        hasIdentifierSpecificError -> displayedError
        isGeneralOrBothError && identifier.isBlank() -> "Veuillez saisir votre email."
        else -> null
    }

    val passwordErrorMessage: String? = when {
        hasPasswordSpecificError -> displayedError
        isGeneralOrBothError && password.isBlank() -> "Veuillez saisir votre mot de passe."
        isGeneralOrBothError -> displayedError
        else -> null
    }

    val isIdentifierError = identifierErrorMessage != null
    val isPasswordError = passwordErrorMessage != null
    val scrollState = rememberScrollState()

    val coroutineScope = rememberCoroutineScope()
    var isLoginAnimating by remember { mutableStateOf(false) }

    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                // When light theme, dark icons for crystal clear visibility of network, battery, time
                insetsController.isAppearanceLightStatusBars = !isDarkMode
                insetsController.isAppearanceLightNavigationBars = !isDarkMode
            }
        }
    }

    fun attemptLogin(customId: String? = null, customPass: String? = null) {
        if (isLoginAnimating || isLoggingIn) return
        val cleanId = (customId ?: identifier).trim()
        val cleanPass = (customPass ?: password).trim()
        if (cleanId.isBlank() && cleanPass.isBlank()) {
            localError = "Veuillez renseigner votre email et mot de passe."
            return
        }
        if (cleanId.isBlank()) {
            localError = "Veuillez renseigner votre email."
            return
        }
        if (cleanPass.isBlank()) {
            localError = "Veuillez saisir votre mot de passe."
            return
        }
        localError = null
        viewModel.clearLoginError()

        viewModel.sessionManager.setRememberMe(rememberMe)
        if (rememberMe) {
            viewModel.sessionManager.setLastIdentifier(cleanId)
        } else {
            viewModel.sessionManager.setLastIdentifier("")
        }

        coroutineScope.launch {
            isLoginAnimating = true
            delay(1200)
            viewModel.login(cleanId, cleanPass)
            isLoginAnimating = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Restored background image
        Image(
            painter = painterResource(id = R.drawable.img_login_clean_bg_1792150740897),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Soft translucent overlay for clean text contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.85f))
        )

        // Sleek refined wecollect logo positioned cleanly at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 22.dp, bottom = 4.dp)
                .align(Alignment.TopCenter)
                .testTag("login_hero_banner"),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_wecollect_logo),
                contentDescription = "wecollect logo",
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(36.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Form container centered in the screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main Title: "Connexion"
                Text(
                    text = "Connexion",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F2B48),
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                // Inactivity / Deactivation notice banner
                AnimatedVisibility(
                    visible = deactivationNotice != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    deactivationNotice?.let { notice ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7),
                            border = null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = notice,
                                        color = Color(0xFF92400E),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.clearDeactivationNotice() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fermer",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Status notification banner if account status changed
                AnimatedVisibility(
                    visible = statusChangeNotification != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    statusChangeNotification?.let { (msg, isPos) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPos) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            border = null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPos) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (isPos) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        color = if (isPos) Color(0xFF14532D) else Color(0xFF991B1B),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.dismissStatusChangeNotification() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fermer",
                                        tint = if (isPos) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Input 1: Email
                CleanUnderlineInputField(
                    value = identifier,
                    onValueChange = {
                        identifier = it
                        localError = null
                        viewModel.clearLoginError()
                    },
                    placeholder = "Email",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = if (isIdentifierError) Color(0xFFDC2626) else Color(0xFF1E293B),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    isError = isIdentifierError,
                    errorMessage = identifierErrorMessage,
                    testTag = "login_identifier_input"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Input 2: Mot de passe
                CleanUnderlineInputField(
                    value = password,
                    onValueChange = {
                        password = it
                        localError = null
                        viewModel.clearLoginError()
                    },
                    placeholder = "Mot de passe",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Mot de passe",
                            tint = if (isPasswordError) Color(0xFFDC2626) else Color(0xFF1E293B),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Masquer" else "Afficher",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { attemptLogin() }
                    ),
                    isError = isPasswordError,
                    errorMessage = passwordErrorMessage,
                    testTag = "login_password_input"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Round checkbox / circular indicator: "Se souvenir de moi"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            rememberMe = !rememberMe
                            viewModel.sessionManager.setRememberMe(rememberMe)
                            if (!rememberMe) {
                                viewModel.sessionManager.setLastIdentifier("")
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (rememberMe) Color(0xFF0284C7) else Color.Transparent,
                        border = BorderStroke(
                            width = 1.8.dp,
                            color = if (rememberMe) Color(0xFF0284C7) else Color(0xFF475569)
                        ),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rememberMe) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Se souvenir de moi",
                        fontSize = 14.5.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Primary Button: "Se connecter"
                Button(
                    onClick = { attemptLogin() },
                    enabled = !isLoggingIn && !isLoginAnimating,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(min = 180.dp, max = 220.dp)
                        .height(48.dp)
                        .pressScaleEffect(0.97f)
                        .testTag("login_submit_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDCEEFE),
                        contentColor = Color(0xFF0284C7),
                        disabledContainerColor = Color(0xFFE5F1FC),
                        disabledContentColor = Color(0xFF0284C7).copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 1.dp
                    )
                ) {
                    if (isLoginAnimating || isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color(0xFF0284C7),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Se connecter",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0284C7)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}



