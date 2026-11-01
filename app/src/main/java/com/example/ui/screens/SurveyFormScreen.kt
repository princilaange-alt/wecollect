package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.example.data.model.OptionEntity
import com.example.data.model.PersonneEntity
import com.example.data.model.QuestionWithOptions
import com.example.ui.SurveyViewModel
import com.example.ui.components.OnlineStatusBadge
import com.example.ui.components.FacebookShimmerCrossfade
import com.example.ui.components.FacebookFormSkeleton
import com.example.ui.components.SurveyAudioInput
import com.example.ui.components.SurveyImageInput
import com.example.ui.components.TopSyncTransferAnimation
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import com.example.ui.components.pressScaleEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyFormScreen(
    viewModel: SurveyViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val questionnaire by viewModel.questionnaire.collectAsState()
    val allQuestionnaires by viewModel.allQuestionnaires.collectAsState()
    val allPersonnes by viewModel.allPersonnes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val codeUnique by viewModel.codeUnique.collectAsState()
    val dateEnquete by viewModel.dateEnquete.collectAsState()
    val textResponses by viewModel.textResponses.collectAsState()
    val selectedOptions by viewModel.selectedOptions.collectAsState()
    val autreResponses by viewModel.autreResponses.collectAsState()
    val validationState by viewModel.validationState.collectAsState()
    val formMessage by viewModel.formMessage.collectAsState()
    val isConnectedAndOnline by viewModel.isConnectedAndOnline.collectAsState()
    val globalSyncState by viewModel.globalSyncState.collectAsState()
    val isPullingForm by viewModel.isPullingForm.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isRefreshingData by viewModel.isRefreshingData.collectAsState()
    val liveUpdateNotification by viewModel.liveUpdateNotification.collectAsState()
    val lastRealtimeSyncTime by viewModel.lastRealtimeSyncTime.collectAsState()
    val duplicateDetectionDialog by viewModel.duplicateDetectionDialog.collectAsState()

    var showQuestionnaireSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = showQuestionnaireSheet || duplicateDetectionDialog != null) {
        if (showQuestionnaireSheet) {
            showQuestionnaireSheet = false
        } else if (duplicateDetectionDialog != null) {
            viewModel.dismissDuplicateDialog()
        }
    }

    val isCurrentlyLoading = isRefreshingData || isPullingForm || isSyncing

    // 3-second save animation states
    val coroutineScope = rememberCoroutineScope()
    var isSavingInFlight by remember { mutableStateOf(false) }
    var saveAnimationStep by remember { mutableIntStateOf(0) } // 0: Idle, 1: Enregistrement en cours (0s-1.8s), 2: Succès validé (1.8s-3.0s)
    val saveProgressAnim = remember { Animatable(0f) }

    val animatedBtnColor by animateColorAsState(
        targetValue = when (saveAnimationStep) {
            2 -> Color(0xFF10B981) // Vibrant emerald green for success
            1 -> if (isConnectedAndOnline) Color(0xFF0F3E76) else Color(0xFF0D9488)
            else -> if (isConnectedAndOnline) Color(0xFF0F3E76) else Color(0xFF0D9488)
        },
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "saveBtnColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Surface(
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu Drawer Button on Left
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .pressScaleEffect(0.85f)
                        .testTag("survey_form_drawer_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu navigation",
                        tint = Color(0xFF0F2942),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Nouvelle enquête",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F2942),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )

                // Indicateur d'état réseau en haut à droite (En ligne / Hors-ligne)
                OnlineStatusBadge(
                    isConnectedAndOnline = isConnectedAndOnline,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        // Animated top transfer banner during sync or pull (5 seconds)
        TopSyncTransferAnimation(
            isSyncing = isSyncing,
            isPullingForm = isPullingForm
        )

        val showInitialSkeleton = questions.isEmpty() && isCurrentlyLoading

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            FacebookShimmerCrossfade(
                isLoading = showInitialSkeleton,
                skeletonContent = { FacebookFormSkeleton() },
                actualContent = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
        val showLiveNotif = liveUpdateNotification?.let { notif ->
            !notif.contains("formulaire", ignoreCase = true) &&
            !notif.contains("questionnaire", ignoreCase = true) &&
            !notif.contains("publi", ignoreCase = true)
        } ?: false

        if (showLiveNotif) {
            item {
                Surface(
                    color = Color(0xFF0F3E76),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = liveUpdateNotification ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissLiveUpdateNotification() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        val showFormMsg = formMessage?.let { (msg, _) ->
            !msg.contains("formulaire", ignoreCase = true) &&
            !msg.contains("publi", ignoreCase = true) &&
            !msg.contains("questionnaire", ignoreCase = true)
        } ?: false

        if (showFormMsg) {
            item {
                val (msg, isErr) = formMessage!!
                Surface(
                    color = if (isErr) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isErr) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isErr) Color(0xFFDC2626) else Color(0xFF16A34A),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = msg,
                            color = if (isErr) Color(0xFF991B1B) else Color(0xFF166534),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearFormMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = if (isErr) Color(0xFF991B1B) else Color(0xFF166534),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. Questionnaire Header (Centered Pro layout)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = questionnaire?.titre ?: if (isPullingForm) "Chargement..." else "Aucun questionnaire disponible",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (allQuestionnaires.size > 1) 40.dp else 12.dp)
                    )

                    if (allQuestionnaires.size > 1) {
                        IconButton(
                            onClick = { showQuestionnaireSheet = true },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(36.dp)
                                .pressScaleEffect(0.85f)
                                .testTag("btn_titre_questionnaire_three_lines")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Changer de questionnaire",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }



                if (!questionnaire?.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = questionnaire?.description ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }

        // Form Submission Error Messages (Only displayed if an error occurs)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                AnimatedVisibility(visible = formMessage != null && formMessage?.second == true) {
                    formMessage?.let { (msg, isError) ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = StatusError.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = StatusError,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusError,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearFormMessage() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Identification de l'Enquête & du Répondant (Clean, without background card/border)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Identification du Répondant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Code unique et date de l'enquête terrain",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val hasCodeError = validationState.codeDuplicateError != null || validationState.isAlreadyAnsweredForCurrentQuestionnaire

                // Field 1: Code Unique avec soulignement animé
                AnimatedUnderlineField(
                    value = codeUnique,
                    onValueChange = { viewModel.onCodeChanged(it) },
                    label = { Text("Code unique du répondant") },
                    leadingIcon = {
                        Icon(
                            imageVector = when {
                                hasCodeError -> Icons.Default.Warning
                                validationState.isExistingCode -> Icons.Default.CheckCircle
                                else -> Icons.Default.Badge
                            },
                            contentDescription = null,
                            tint = when {
                                hasCodeError -> StatusError
                                validationState.isExistingCode -> Color(0xFF2563EB)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { viewModel.generateNewCode() },
                            modifier = Modifier
                                .pressScaleEffect(0.85f)
                                .testTag("generate_code_btn")
                        ) {
                            Icon(
                                Icons.Default.Autorenew,
                                contentDescription = "Générer",
                                tint = if (hasCodeError) StatusError else MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    supportingText = {
                        if (validationState.codeError != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = StatusError,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = validationState.codeError ?: "Code requis",
                                    color = StatusError,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else if (validationState.isExistingCode) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Personne identifiée dans la base • Éligible pour ce questionnaire",
                                    color = Color(0xFF2563EB),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Code valide",
                                    color = Color(0xFF15803D),
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    },
                    isError = validationState.codeError != null || hasCodeError || codeUnique.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("survey_code_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Field 2: Date de l'enquête avec soulignement animé
                AnimatedUnderlineField(
                    value = dateEnquete,
                    onValueChange = { viewModel.onDateChanged(it) },
                    label = { Text("Date de l'enquête (AAAA-MM-JJ)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            tint = if (validationState.dateError != null) StatusError else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                viewModel.onDateChanged(today)
                            },
                            modifier = Modifier
                                .pressScaleEffect(0.85f)
                                .testTag("today_date_btn")
                        ) {
                            Icon(
                                Icons.Default.Today,
                                contentDescription = "Aujourd'hui",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    supportingText = {
                        if (validationState.dateError != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = StatusError,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = validationState.dateError ?: "Date requise",
                                    color = StatusError,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                text = "Format attendu : AAAA-MM-JJ (ex: 2026-09-25)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    },
                    isError = validationState.dateError != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("survey_date_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }

        // Questions List
        itemsIndexed(
            items = questions,
            key = { _, qItem -> "q_${qItem.question.idQuestionnaire}_${qItem.question.idQuestion}" }
        ) { index, qItem ->
            val curVal = textResponses[qItem.question.idQuestion]
            val effectiveVal = if (qItem.question.estCodeUnique && curVal.isNullOrBlank()) codeUnique else (curVal ?: "")
            val autreVal = autreResponses[qItem.question.idQuestion] ?: ""
            QuestionCard(
                index = index + 1,
                questionWithOptions = qItem,
                textValue = effectiveVal,
                autreValue = autreVal,
                selectedOptions = selectedOptions[qItem.question.idQuestion] ?: emptyList(),
                fieldError = validationState.fieldErrors[qItem.question.idQuestion],
                onTextChanged = { viewModel.onTextChanged(qItem.question.idQuestion, it) },
                onAutreTextChanged = { viewModel.onAutreTextChanged(qItem.question.idQuestion, it) },
                onSingleOptionSelected = { optId, optText ->
                    viewModel.onSingleOptionSelected(qItem.question.idQuestion, optId, optText)
                },
                onMultiOptionToggled = { optId ->
                    viewModel.onMultiOptionToggled(qItem.question.idQuestion, optId)
                }
            )
        }

        // Supabase sync / loading indicator if no questions are loaded yet
        if (questions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (isPullingForm) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Fampidinana ny questionnaires avy amin'ny Supabase...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Aucun questionnaire disponible",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Izay questionnaire navoaka (PUBLIÉ) ao amin'ny Supabase ihany no hiseho eto.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(
                                onClick = { viewModel.pullFormsFromSupabase() },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Actualiser / Synchroniser")
                            }
                        }
                    }
                }
            }
        }



        // Action Buttons: Save to SQLite (retained in fiches) & Auto-Sync to Supabase
        if (questions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Primary Button: Enregistrer l'enquête with 3-second animated sequence
                    Button(
                        onClick = {
                            if (!isSavingInFlight && validationState.isReadyToSubmit) {
                                coroutineScope.launch {
                                    isSavingInFlight = true
                                    saveAnimationStep = 1
                                    saveProgressAnim.snapTo(0f)

                                    // Launch 3-second progress indicator animation
                                    val progressJob = launch {
                                        saveProgressAnim.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
                                        )
                                    }

                                    // Phase 1: Saving / Synchronizing state (1800ms)
                                    delay(1800)
                                    saveAnimationStep = 2 // Transition to success checkmark

                                    // Phase 2: Success state feedback (1200ms -> total 3000ms)
                                    delay(1200)

                                    // Perform the actual survey commit to database and Supabase
                                    viewModel.submitSurvey(andSync = true)

                                    // Reset animation state
                                    progressJob.cancel()
                                    saveProgressAnim.snapTo(0f)
                                    saveAnimationStep = 0
                                    isSavingInFlight = false
                                }
                            }
                        },
                        enabled = validationState.isReadyToSubmit && !isSavingInFlight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScaleEffect(0.96f)
                            .height(56.dp)
                            .testTag("save_and_sync_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = animatedBtnColor,
                            disabledContainerColor = if (isSavingInFlight) animatedBtnColor else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                when (saveAnimationStep) {
                                    1 -> {
                                        // Phase 1 (0-1.8s)
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (isConnectedAndOnline) "Enregistrement & Synchronisation..." else "Enregistrement dans SQLite...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                    }
                                    2 -> {
                                        // Phase 2 (1.8s-3.0s)
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Enregistré avec succès !",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.5.sp,
                                            color = Color.White
                                        )
                                    }
                                    else -> {
                                        // Idle
                                        Icon(
                                            imageVector = if (isConnectedAndOnline) Icons.Default.CloudUpload else Icons.Default.Save,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (isConnectedAndOnline) "Enregistrer et synchroniser" else "Enregistrer l'enquête",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.5.sp
                                        )
                                    }
                                }
                            }

                            // 3-second live progress track at the bottom of the button
                            if (isSavingInFlight) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                        .background(Color.White.copy(alpha = 0.25f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(saveProgressAnim.value)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }


                    FilledTonalButton(
                        onClick = { viewModel.resetForm() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScaleEffect(0.96f)
                            .height(48.dp)
                            .testTag("reset_form_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Effacer et recommencer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
                    }
                }
            )
        }
    }

    // ModalBottomSheet hisafidianana ny questionnaire rehefa tsindrina ny "trois tirets" (☰)
    if (showQuestionnaireSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuestionnaireSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Questionnaires",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${allQuestionnaires.size} formulaire(s) disponible(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.pullFormsFromSupabase() },
                            modifier = Modifier
                                .size(36.dp)
                                .pressScaleEffect(0.85f)
                        ) {
                            if (isCurrentlyLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Actualiser en direct",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showQuestionnaireSheet = false },
                            modifier = Modifier
                                .size(36.dp)
                                .pressScaleEffect(0.85f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (allQuestionnaires.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aucun questionnaire disponible",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Les questionnaires en statut « BROUILLON » ne sont pas affichés.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        allQuestionnaires.forEachIndexed { index, qItem ->
                            val isSelected = questionnaire?.idQuestionnaire == qItem.idQuestionnaire
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectQuestionnaire(qItem.idQuestionnaire)
                                        showQuestionnaireSheet = false
                                    }
                                    .pressScaleEffect(0.98f)
                                    .padding(vertical = 12.dp, horizontal = 4.dp)
                                    .testTag("sheet_questionnaire_item_${qItem.idQuestionnaire}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = qItem.titre,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (qItem.version.isNotBlank()) {
                                                Text(
                                                    text = qItem.version,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF047857)
                                                )
                                            }

                                            val isAnsweredByThisCode = validationState.answeredQuestionnaires.any { it.equals(qItem.titre, ignoreCase = true) }
                                            if (codeUnique.isNotBlank() && isAnsweredByThisCode) {
                                                Text(
                                                    text = if (qItem.version.isNotBlank()) "• Déjà répondu" else "Déjà répondu",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusError
                                                )
                                            }
                                        }


                                        if (!qItem.description.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = qItem.description ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Sélectionné",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                if (index < allQuestionnaires.lastIndex) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal AlertDialog d'avertissement de doublons sur les champs critiques (Téléphone, ID unique, etc.)
    if (duplicateDetectionDialog != null) {
        val dup = duplicateDetectionDialog!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Doublon détecté",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Un enregistrement existe déjà dans la base pour ce champ critique :",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!dup.conflictingField.isNullOrBlank()) {
                                Row {
                                    Text(
                                        text = "Champ : ",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = dup.conflictingField!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }
                            if (!dup.conflictingValue.isNullOrBlank()) {
                                Row {
                                    Text(
                                        text = "Valeur en conflit : ",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = dup.conflictingValue!!,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                            if (!dup.conflictingCode.isNullOrBlank()) {
                                Row {
                                    Text(
                                        text = "Code personne lié : ",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = dup.conflictingCode!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }
                            if (dup.conflictingEnqueteId != null) {
                                Row {
                                    Text(
                                        text = "Enquête existante : ",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = "#${dup.conflictingEnqueteId}${dup.conflictingDate?.let { " du $it" } ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Veuillez vérifier et corriger la valeur avant d'enregistrer à nouveau.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissDuplicateDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3E76))
                ) {
                    Text("Corriger la saisie", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun VisualErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    if (message.contains("obligatoire", ignoreCase = true) || message.contains("ce champ", ignoreCase = true)) {
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(StatusError.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = StatusError,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = message,
            color = StatusError,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun QuestionCard(
    index: Int,
    questionWithOptions: QuestionWithOptions,
    textValue: String,
    autreValue: String,
    selectedOptions: List<Long>,
    fieldError: String? = null,
    onTextChanged: (String) -> Unit,
    onAutreTextChanged: (String) -> Unit,
    onSingleOptionSelected: (Long, String) -> Unit,
    onMultiOptionToggled: (Long) -> Unit
) {
    val q = questionWithOptions.question
    val options = questionWithOptions.options

    fun isAutreOption(libelle: String): Boolean {
        val clean = libelle.trim().lowercase()
        return clean == "autre" || clean == "autres" || clean == "hafa" || clean == "other" ||
                clean.startsWith("autre") || clean.startsWith("hafa") ||
                clean.contains("préciser") || clean.contains("preciser") ||
                clean.contains("à préciser") || clean.contains("a preciser")
    }

    val selectedAutreOpt = options.find { opt ->
        selectedOptions.contains(opt.idOption) && isAutreOption(opt.libelle)
    }
    val hasAutreSelected = selectedAutreOpt != null

    val isAnswered = when (q.type) {
        "CHOIX_UNIQUE" -> (selectedOptions.isNotEmpty() || textValue.isNotBlank()) && (!hasAutreSelected || autreValue.isNotBlank())
        "CHOIX_MULTIPLE" -> selectedOptions.isNotEmpty() && (!hasAutreSelected || autreValue.isNotBlank())
        else -> textValue.isNotBlank()
    }

    val isValid = isAnswered && fieldError == null

    val qTypeNormalized = q.type.trim().uppercase()
    val qLabelNormalized = q.libelle.trim().lowercase()

    val isPhoneQuestion = (qTypeNormalized == "TEXTE" || qTypeNormalized == "NOMBRE") && (
        qLabelNormalized.contains("téléphone") ||
        qLabelNormalized.contains("telephone") ||
        qLabelNormalized.contains("contact") ||
        qLabelNormalized.contains("phone") ||
        qLabelNormalized.contains("mobile") ||
        qLabelNormalized.contains("finday") ||
        (qLabelNormalized.contains("numéro") && (qLabelNormalized.contains("tel") || qLabelNormalized.contains("appel") || qLabelNormalized.contains("joindre")))
    )

    val isEmailQuestion = qTypeNormalized == "TEXTE" && (
        qLabelNormalized.contains("email") ||
        qLabelNormalized.contains("courriel") ||
        qLabelNormalized.contains("e-mail")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                    val isMandatory = q.obligatoire
                    Text(
                        text = "$index.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isMandatory) Color(0xFFDC2626)
                        else if (isValid) Color(0xFF15803D)
                        else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = q.libelle.replace("*", "").trim(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isValid) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Valide",
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val isImageType = qTypeNormalized in listOf("IMAGE", "PHOTO", "PICTURE", "CAMERA") ||
                    qTypeNormalized.contains("IMAGE") || qTypeNormalized.contains("PHOTO") ||
                    (qTypeNormalized == "TEXTE" && (qLabelNormalized.contains("photo") || qLabelNormalized.contains("sary") || qLabelNormalized.contains("image")))

            val isAudioType = qTypeNormalized in listOf("AUDIO", "VOCAL", "SON", "VOICE", "ENREGISTREMENT", "MICRO") ||
                    qTypeNormalized.contains("AUDIO") || qTypeNormalized.contains("VOCAL") || qTypeNormalized.contains("SON") ||
                    (qTypeNormalized == "TEXTE" && (qLabelNormalized.contains("audio") || qLabelNormalized.contains("vocal") || qLabelNormalized.contains("enregistrement") || qLabelNormalized.contains("feo") || qLabelNormalized.contains("peo")))

            when {
                isImageType -> {
                    SurveyImageInput(
                        questionId = q.idQuestion,
                        value = textValue,
                        onImageChanged = onTextChanged
                    )
                    if (fieldError != null) {
                        VisualErrorMessage(message = fieldError)
                    }
                }

                isAudioType -> {
                    SurveyAudioInput(
                        questionId = q.idQuestion,
                        value = textValue,
                        onAudioChanged = onTextChanged
                    )
                    if (fieldError != null) {
                        VisualErrorMessage(message = fieldError)
                    }
                }

                qTypeNormalized == "NOMBRE" -> {
                    AnimatedUnderlineField(
                        value = textValue,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' || it == '-' }) {
                                onTextChanged(newValue)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = {
                            Text(
                                "Ex: 5",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        trailingIcon = if (textValue.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = { onTextChanged("") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Effacer",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else null,
                        supportingText = if (fieldError != null) {
                            { VisualErrorMessage(message = fieldError) }
                        } else null,
                        isError = fieldError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("question_number_${q.idQuestion}"),
                        singleLine = true
                    )
                }

                qTypeNormalized == "DATE" -> {
                    AnimatedUnderlineField(
                        value = textValue,
                        onValueChange = onTextChanged,
                        placeholder = {
                            Text(
                                "AAAA-MM-JJ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                tint = if (fieldError != null) StatusError else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    onTextChanged(today)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Today,
                                    contentDescription = "Aujourd'hui",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        supportingText = {
                            if (fieldError != null) {
                                VisualErrorMessage(message = fieldError)
                            } else {
                                Text(
                                    text = "Format attendu : AAAA-MM-JJ (ex: 2026-09-25)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        },
                        isError = fieldError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("question_date_${q.idQuestion}"),
                        singleLine = true
                    )
                }

                qTypeNormalized == "CHOIX_UNIQUE" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        options.forEach { option ->
                            val isSelected = selectedOptions.contains(option.idOption) || textValue == option.libelle || textValue.startsWith("${option.libelle}:")
                            val isAutre = isAutreOption(option.libelle)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                SelectableOptionCard(
                                    text = option.libelle,
                                    isSelected = isSelected,
                                    isRadio = true,
                                    onClick = { onSingleOptionSelected(option.idOption, option.libelle) }
                                )

                                // Saisie automatique dynamique lorsque "Autre" est sélectionné
                                AnimatedVisibility(
                                    visible = isSelected && isAutre,
                                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
                                    ) {
                                        AnimatedUnderlineField(
                                            value = autreValue,
                                            onValueChange = onAutreTextChanged,
                                            label = { Text("Préciser pour « ${option.libelle} »") },
                                            placeholder = { Text("Veuillez préciser votre réponse...") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.EditNote,
                                                    contentDescription = null,
                                                    tint = if (fieldError != null && autreValue.isBlank()) StatusError else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            trailingIcon = if (autreValue.isNotEmpty()) {
                                                {
                                                    IconButton(
                                                        onClick = { onAutreTextChanged("") },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Effacer",
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else null,
                                            isError = fieldError != null && autreValue.isBlank(),
                                            supportingText = if (autreValue.isBlank()) {
                                                {
                                                    Text(
                                                        text = "Veuillez préciser votre réponse",
                                                        color = StatusError,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            } else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("autre_input_${q.idQuestion}"),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                        if (fieldError != null) {
                            VisualErrorMessage(message = fieldError)
                        }
                    }
                }

                qTypeNormalized == "CHOIX_MULTIPLE" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        options.forEach { option ->
                            val isSelected = selectedOptions.contains(option.idOption)
                            val isAutre = isAutreOption(option.libelle)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                SelectableOptionCard(
                                    text = option.libelle,
                                    isSelected = isSelected,
                                    isRadio = false,
                                    onClick = { onMultiOptionToggled(option.idOption) }
                                )

                                // Saisie automatique dynamique lorsque "Autre" est sélectionné
                                AnimatedVisibility(
                                    visible = isSelected && isAutre,
                                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
                                    ) {
                                        AnimatedUnderlineField(
                                            value = autreValue,
                                            onValueChange = onAutreTextChanged,
                                            label = { Text("Préciser pour « ${option.libelle} »") },
                                            placeholder = { Text("Veuillez préciser votre réponse...") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.EditNote,
                                                    contentDescription = null,
                                                    tint = if (fieldError != null && autreValue.isBlank()) StatusError else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            trailingIcon = if (autreValue.isNotEmpty()) {
                                                {
                                                    IconButton(
                                                        onClick = { onAutreTextChanged("") },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Effacer",
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else null,
                                            isError = fieldError != null && autreValue.isBlank(),
                                            supportingText = if (autreValue.isBlank()) {
                                                {
                                                    Text(
                                                        text = "Veuillez préciser votre réponse",
                                                        color = StatusError,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            } else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("autre_multi_input_${q.idQuestion}"),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                        if (fieldError != null) {
                            VisualErrorMessage(message = fieldError)
                        }
                    }
                }

                else -> {
                    AnimatedUnderlineField(
                        value = textValue,
                        onValueChange = onTextChanged,
                        placeholder = {
                            Text(
                                text = when {
                                    isPhoneQuestion -> "0345812888"
                                    isEmailQuestion -> "contact@exemple.mg"
                                    else -> "Saisissez votre réponse..."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = when {
                            isPhoneQuestion -> {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = if (fieldError != null) StatusError else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            isEmailQuestion -> {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = if (fieldError != null) StatusError else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            else -> null
                        },
                        keyboardOptions = when {
                            isPhoneQuestion -> KeyboardOptions(keyboardType = KeyboardType.Phone)
                            isEmailQuestion -> KeyboardOptions(keyboardType = KeyboardType.Email)
                            else -> KeyboardOptions.Default
                        },
                        trailingIcon = if (textValue.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = { onTextChanged("") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Effacer",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else null,
                        supportingText = {
                            if (fieldError != null) {
                                VisualErrorMessage(message = fieldError)
                            } else if (isPhoneQuestion && textValue.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Format valide",
                                        color = Color(0xFF15803D),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else if (isPhoneQuestion) {
                                Text(
                                    text = "Format attendu : 10 chiffres (ex: 0345812888)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            } else if (isEmailQuestion && textValue.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Format valide",
                                        color = Color(0xFF15803D),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        },
                        isError = fieldError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("question_text_${q.idQuestion}"),
                        singleLine = isPhoneQuestion || isEmailQuestion,
                        maxLines = if (isPhoneQuestion || isEmailQuestion) 1 else 4
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun SelectableOptionCard(
    text: String,
    isSelected: Boolean,
    isRadio: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .pressScaleEffect(0.99f)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        if (isRadio) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            )
        } else {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Champ de saisie moderne avec ligne inférieure animée (Animated Underline).
 * L'indicateur inférieur s'anime avec effet spring & gradient lors de la prise de focus.
 */
@Composable
fun AnimatedUnderlineField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    testTag: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    // Animation fluide d'expansion de la ligne de soulignement (de 0 à 100%)
    val animatedLineWidth by animateFloatAsState(
        targetValue = if (isFocused || isError) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "animated_underline_width"
    )

    // Animation de la hauteur de la ligne active
    val animatedLineHeight by animateDpAsState(
        targetValue = if (isFocused || isError) 2.5.dp else 1.dp,
        animationSpec = tween(durationMillis = 220),
        label = "animated_underline_height"
    )

    val underlineColor = if (isError) {
        StatusError
    } else {
        activeColor
    }

    val baseUnderlineColor = if (isError) {
        StatusError.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                label = label,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                singleLine = singleLine,
                maxLines = maxLines,
                keyboardOptions = keyboardOptions,
                readOnly = readOnly,
                isError = isError,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = if (isError) StatusError else activeColor,
                    errorCursorColor = StatusError,
                    errorLabelColor = StatusError,
                    errorLeadingIconColor = StatusError,
                    errorTrailingIconColor = StatusError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    }
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            )

            // Ligne de soulignement animée en bas
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                contentAlignment = Alignment.Center
            ) {
                // Ligne de base inactive (toujours visible, fine et élégante 1dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(baseUnderlineColor)
                )

                // Ligne active animée qui s'agrandit depuis le centre vers les bords
                if (animatedLineWidth > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedLineWidth)
                            .height(animatedLineHeight)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        underlineColor.copy(alpha = 0.8f),
                                        underlineColor,
                                        underlineColor.copy(alpha = 0.8f)
                                    )
                                ),
                                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                    )
                }
            }
        }

        // Texte d'aide ou message d'erreur
        if (supportingText != null) {
            Box(modifier = Modifier.padding(start = 8.dp, top = 4.dp, end = 8.dp)) {
                supportingText()
            }
        }
    }
}

