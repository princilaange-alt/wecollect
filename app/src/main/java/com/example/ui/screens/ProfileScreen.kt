package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import com.example.R
import com.example.ui.SurveyViewModel
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import com.example.ui.components.EnqueteurAvatar
import com.example.ui.components.pressScaleEffect
import com.example.ui.components.TopSyncTransferAnimation

@Composable
fun ProfileScreen(
    viewModel: SurveyViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeUser = loggedInUser ?: currentUser
    val questionnaire by viewModel.questionnaire.collectAsState()
    val isPullingForm by viewModel.isPullingForm.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isForceOffline by viewModel.isForceOffline.collectAsState()
    val isConnectedAndOnline by viewModel.isConnectedAndOnline.collectAsState()
    val researchMetrics by viewModel.researchMetrics.collectAsState()
    val statusChangeNotification by viewModel.statusChangeNotification.collectAsState()
    val pendingCount = researchMetrics.pendingEnquetes
    val context = LocalContext.current

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPersonalInfoDialog by remember { mutableStateOf(false) }
    var startEditingInDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val hasAnyOpenDialog = showLogoutDialog || showPersonalInfoDialog || showChangePasswordDialog ||
            showAboutDialog || showHelpDialog || showSettingsDialog || showQrDialog || showOptionsMenu

    BackHandler(enabled = hasAnyOpenDialog) {
        when {
            showLogoutDialog -> showLogoutDialog = false
            showPersonalInfoDialog -> showPersonalInfoDialog = false
            showChangePasswordDialog -> showChangePasswordDialog = false
            showAboutDialog -> showAboutDialog = false
            showHelpDialog -> showHelpDialog = false
            showSettingsDialog -> showSettingsDialog = false
            showQrDialog -> showQrDialog = false
            showOptionsMenu -> showOptionsMenu = false
        }
    }

    var agentId by remember { mutableStateOf("ENQ-101") }
    var agentRole by remember { mutableStateOf(activeUser?.role ?: "ENQUETEUR") }
    var agentStatut by remember { mutableStateOf(activeUser?.statut ?: "ACTIF") }

    // Password fields
    var oldPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordSuccess by remember { mutableStateOf(false) }
    var passwordSuccessMsg by remember { mutableStateOf<String?>(null) }
    val isChangingPassword by viewModel.isChangingPassword.collectAsState()

    val rawDisplayName = listOfNotNull(
        activeUser?.nom?.takeIf { it.isNotBlank() },
        activeUser?.prenom?.takeIf { it.isNotBlank() }
    ).joinToString(" ").trim()

    val displayName = if (rawDisplayName.isNotBlank()) rawDisplayName else "RAHARINIRINA Juvance"
    val displayFullName = if (rawDisplayName.isNotBlank()) "${activeUser?.nom ?: "RAHARINIRINA"} ${activeUser?.prenom ?: "Tahintsoa Juvance"}".trim() else "RAHARINIRINA Tahintsoa Juvance"
    val displayEmail = if (!activeUser?.email.isNullOrBlank()) activeUser!!.email else "juvanceraharinirina@gmail.com"
    val displayPhone = if (!activeUser?.telephone.isNullOrBlank()) activeUser!!.telephone else "0345812888"
    val userInitial = displayName.firstOrNull()?.uppercase() ?: "J"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // --- 1. Top Bar (Hamburger menu ☰, "Profil") ---
        Surface(
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu Hamburger Icon (☰)
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("profile_drawer_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu navigation",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Title: "Profil"
                Text(
                    text = "Profil",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )


                // Settings Gear Icon
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier.testTag("profile_settings_gear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Paramètres de l'application",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }

        // --- 2. Body (Matching Screenshot IMG20261014092659.jpg) ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Transfer Banner
            item {
                TopSyncTransferAnimation(
                    isSyncing = isSyncing,
                    isPullingForm = isPullingForm
                )
            }

            // Notification Banner if present
            if (statusChangeNotification != null) {
                item {
                    statusChangeNotification?.let { (msg, isPos) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPos) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, if (isPos) Color(0xFF86EFAC) else Color(0xFFFCA5A5))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = msg,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isPos) Color(0xFF14532D) else Color(0xFF991B1B)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.dismissStatusChangeNotification() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fermer",
                                        tint = if (isPos) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Header Avatar & Name & Email
            item {
                Spacer(modifier = Modifier.height(28.dp))

                // Avatar Box with Sky-Blue Circle Border
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .border(BorderStroke(3.dp, Color(0xFF64B5F6)), CircleShape)
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    EnqueteurAvatar(
                        photoUrl = activeUser?.photoUrl,
                        photo = activeUser?.photo,
                        size = 112.dp,
                        showBorder = false
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // User Full Name
                Text(
                    text = displayName,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // User Email
                Text(
                    text = displayEmail,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(34.dp))
            }

            // Section "Informations académiques"
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF0288D1),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Informations de l'enquêteur",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // 1. Matricule
                    AcademicInfoItem(
                        icon = Icons.Default.Fingerprint,
                        label = "Matricule",
                        value = activeUser?.matricule?.takeIf { it.isNotBlank() } ?: ""
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. Région d'affectation
                    AcademicInfoItem(
                        icon = Icons.Default.Place,
                        label = "Région d'affectation",
                        value = activeUser?.region?.ifBlank { "Analamanga" } ?: "Analamanga"
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 3. Nom complet
                    AcademicInfoItem(
                        icon = Icons.Default.PersonOutline,
                        label = "Nom complet",
                        value = displayFullName
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 4. Rôle
                    AcademicInfoItem(
                        icon = Icons.Default.Security,
                        label = "Rôle",
                        value = activeUser?.role ?: "ENQUETEUR"
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 5. Téléphone
                    AcademicInfoItem(
                        icon = Icons.Default.Phone,
                        label = "Téléphone",
                        value = displayPhone
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Buttons Row: Éditer informations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "Éditer informations" button
                        TextButton(
                            onClick = {
                                startEditingInDialog = true
                                showPersonalInfoDialog = true
                            },
                            modifier = Modifier
                                .pressScaleEffect(0.97f)
                                .testTag("menu_personal_info")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF0288D1)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Éditer profil",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0288D1)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    // 0. QR Code Dialog
    if (showQrDialog) {
        Dialog(onDismissRequest = { showQrDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Fiche Étudiant / Enquêteur",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Code QR",
                            tint = Color(0xFF0F3E76),
                            modifier = Modifier.size(100.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = displayFullName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    if (!activeUser?.matricule.isNullOrBlank()) {
                        Text(text = "Matricule: ${activeUser!!.matricule}", fontSize = 14.sp, color = Color(0xFF64748B))
                    }
                    Text(text = agentRole, fontSize = 13.5.sp, color = Color(0xFF0288D1), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showQrDialog = false },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3E76)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fermer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 1. Personal Information Editor Dialog (Moderne, complet et synchronisé)
    if (showPersonalInfoDialog) {
        ProfileEditorDialog(
            user = activeUser,
            onDismiss = {
                showPersonalInfoDialog = false
                startEditingInDialog = false
            },
            onSaveUser = { updatedUser ->
                agentRole = updatedUser.role
                agentStatut = updatedUser.statut
                viewModel.updateUser(updatedUser)
            }
        )
    }


    // 2. Change Password Dialog (Styled matching reference image with CleanUnderlineInputField and light blue pill button)
    if (showChangePasswordDialog) {
        Dialog(
            onDismissRequest = {
                if (!isChangingPassword) {
                    showChangePasswordDialog = false
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(0.dp)),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header: Title & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Changer de mot de passe",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        IconButton(
                            onClick = {
                                if (!isChangingPassword) {
                                    showChangePasswordDialog = false
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mettez à jour vos identifiants d'accès sécurisé",
                        fontSize = 12.5.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (passwordSuccess) {
                        Surface(
                            color = StatusSuccess.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = passwordSuccessMsg ?: "Mot de passe modifié avec succès !",
                                    color = StatusSuccess,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showChangePasswordDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .pressScaleEffect(0.97f),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDCEEFE),
                                contentColor = Color(0xFF0284C7)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 1.dp)
                        ) {
                            Text("Fermer", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0284C7))
                        }
                    } else {
                        // Error message banner
                        if (passwordError != null) {
                            Surface(
                                color = StatusError.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = StatusError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = passwordError!!,
                                        color = StatusError,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Form input fields matching reference image with CleanUnderlineInputField
                        // 1. Old password (if applicable)
                        if (loggedInUser?.motDePasse?.isNotBlank() == true) {
                            CleanUnderlineInputField(
                                value = oldPasswordInput,
                                onValueChange = {
                                    oldPasswordInput = it
                                    passwordError = null
                                },
                                placeholder = "Mot de passe actuel",
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VpnKey,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                testTag = "input_old_password"
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // 2. New password (with single show/hide toggle)
                        CleanUnderlineInputField(
                            value = newPasswordInput,
                            onValueChange = {
                                newPasswordInput = it
                                passwordError = null
                            },
                            placeholder = "Nouveau mot de passe",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
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
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            testTag = "input_new_password"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Confirm password
                        val isMismatch = confirmPasswordInput.isNotEmpty() && newPasswordInput != confirmPasswordInput
                        CleanUnderlineInputField(
                            value = confirmPasswordInput,
                            onValueChange = {
                                confirmPasswordInput = it
                                passwordError = null
                            },
                            placeholder = "Confirmer le mot de passe",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isMismatch) Color(0xFFDC2626) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            isError = isMismatch,
                            errorMessage = if (isMismatch) "Les deux mots de passe ne correspondent pas" else null,
                            testTag = "input_confirm_password"
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = "Au moins 4 caractères",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        // Primary Pill Button: light blue pill with blue text matching reference image
                        Button(
                            onClick = {
                                if (newPasswordInput.length < 4) {
                                    passwordError = "Le mot de passe doit comporter au moins 4 caractères"
                                    return@Button
                                }
                                if (newPasswordInput != confirmPasswordInput) {
                                    passwordError = "Les deux mots de passe ne correspondent pas"
                                    return@Button
                                }
                                viewModel.updatePassword(
                                    oldPassword = oldPasswordInput.ifBlank { null },
                                    newPassword = newPasswordInput,
                                    onSuccess = { msg ->
                                        passwordSuccess = true
                                        passwordSuccessMsg = msg
                                        passwordError = null
                                    },
                                    onError = { err ->
                                        passwordError = err
                                    }
                                )
                            },
                            enabled = !isChangingPassword && newPasswordInput.isNotBlank() && confirmPasswordInput.isNotBlank() && !isMismatch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .pressScaleEffect(0.97f)
                                .testTag("button_save_password"),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDCEEFE),
                                contentColor = Color(0xFF0284C7),
                                disabledContainerColor = Color(0xFFE5F1FC),
                                disabledContentColor = Color(0xFF0284C7).copy(alpha = 0.5f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 1.dp)
                        ) {
                            if (isChangingPassword) {
                                CircularProgressIndicator(
                                    color = Color(0xFF0284C7),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enregistrement...", color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                            } else {
                                Text("Enregistrer", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF0284C7))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Annuler Button
                        TextButton(
                            onClick = { showChangePasswordDialog = false },
                            enabled = !isChangingPassword,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Annuler", color = Color(0xFF64748B), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    // 3. About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF0F3E76))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("À propos de l'application", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Système de Collecte & Enquête Terrain",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text("Version 2.4.0 (Build 2026)", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Architecture 100% Offline-First avec base de données SQLite locale Room.\n• Synchronisation bidirectionnelle automatique vers le serveur central.\n• Validations dynamiques des questions obligatoires et unicité des fiches.",
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Fermer")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }

    // 4. Help & Support Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = Color(0xFF0F3E76))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aide & Support Enquêteur", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Guide terrain rapide :", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    Text(
                        text = "1. Saisie hors-ligne : Vous pouvez remplir les fiches même sans réseau mobile. Elles sont immédiatement sécurisées en local.\n2. Synchronisation : Dès le retour dans une zone couverte par Internet, appuyez sur 'Lancer la synchronisation'.\n3. Contact Superviseur : En cas de doute ou d'anomalie sur le terrain, appelez le centre de coordination au +261 34 00 000 00.",
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Compris")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }

    // 5. Settings Dialog (From the gear icon in top bar)
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF0F3E76))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Paramètres de l'application", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Changer le mot de passe
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showSettingsDialog = false
                                passwordError = null
                                passwordSuccess = false
                                passwordSuccessMsg = null
                                oldPasswordInput = ""
                                newPasswordInput = ""
                                confirmPasswordInput = ""
                                passwordVisible = false
                                showChangePasswordDialog = true
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF0F3E76),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Changer de mot de passe", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F3E76))
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    HorizontalDivider()

                    // À propos
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showSettingsDialog = false
                                showAboutDialog = true
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF0F3E76),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("À propos", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F3E76))
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    HorizontalDivider()

                    // Aide & Support
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showSettingsDialog = false
                                showHelpDialog = true
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = Color(0xFF0F3E76),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Aide & Support", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F3E76))
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Fermer")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }

    // 6. Confirm Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Confirmation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Êtes-vous sûr de vouloir vous déconnecter ?",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text(
                        text = "Déconnexion",
                        color = Color(0xFF0288D1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(
                        text = "Annuler",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    tag: String,
    badgeText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleEffect(0.97f)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF334155),
            modifier = Modifier.size(21.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (badgeText.contains("attente") || badgeText.contains("envoyer")) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (badgeText.contains("attente") || badgeText.contains("envoyer")) Color(0xFFD97706) else Color(0xFF16A34A),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Ouvrir",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun InfoItemRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
