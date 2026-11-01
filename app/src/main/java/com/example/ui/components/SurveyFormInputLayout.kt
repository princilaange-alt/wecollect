package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OptionEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.QuestionWithOptions
import com.example.data.model.QuestionnaireEntity
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SurveyFormInputLayout:
 * A modern, accessible, and reactive Material 3 Jetpack Compose layout
 * dedicated to collecting structured survey fields (Respondent details, text,
 * numeric, dates, single/multi-choice, multimedia, audio, and rating inputs).
 */
@Composable
fun SurveyFormInputLayout(
    questionnaire: QuestionnaireEntity?,
    questions: List<QuestionWithOptions>,
    codeUnique: String,
    dateEnquete: String,
    textResponses: Map<Long, String>,
    selectedOptions: Map<Long, List<Long>>,
    fieldErrors: Map<Long, String> = emptyMap(),
    codeError: String? = null,
    dateError: String? = null,
    onCodeChanged: (String) -> Unit,
    onGenerateCode: () -> Unit,
    onDateChanged: (String) -> Unit,
    onTextChanged: (Long, String) -> Unit,
    onSingleOptionSelected: (Long, Long, String) -> Unit,
    onMultiOptionToggled: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null
) {
    // Calculer le taux de remplissage pour la barre de progression
    val totalFields = questions.size + 2 // +2 pour Code & Date
    val filledFields = (if (codeUnique.isNotBlank()) 1 else 0) +
            (if (dateEnquete.isNotBlank()) 1 else 0) +
            questions.count { qItem ->
                val qId = qItem.question.idQuestion
                when (qItem.question.type.trim().uppercase()) {
                    "CHOIX_UNIQUE" -> (selectedOptions[qId]?.isNotEmpty() == true) || !textResponses[qId].isNullOrBlank()
                    "CHOIX_MULTIPLE" -> selectedOptions[qId]?.isNotEmpty() == true
                    else -> !textResponses[qId].isNullOrBlank()
                }
            }
    val progressFraction = if (totalFields > 0) filledFields.toFloat() / totalFields.toFloat() else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // En-tête personnalisé si fourni
        if (headerContent != null) {
            item {
                headerContent()
            }
        }

        // 1. En-tête du Questionnaire & Progression globale
        item {
            SurveyFormHeaderSection(
                title = questionnaire?.titre ?: "Formulaire de collecte terrain",
                description = questionnaire?.description,
                filledCount = filledFields,
                totalCount = totalFields,
                progressFraction = progressFraction
            )
        }

        // 2. Section Identification du répondant (Code unique + Date de l'enquête)
        item {
            SurveyRespondentInputSection(
                codeUnique = codeUnique,
                dateEnquete = dateEnquete,
                codeError = codeError,
                dateError = dateError,
                onCodeChanged = onCodeChanged,
                onGenerateCode = onGenerateCode,
                onDateChanged = onDateChanged
            )
        }

        // 3. Questions dynamiques du formulaire
        itemsIndexed(
            items = questions,
            key = { _, item -> "survey_q_${item.question.idQuestionnaire}_${item.question.idQuestion}" }
        ) { index, qItem ->
            val q = qItem.question
            val curVal = textResponses[q.idQuestion] ?: ""
            val effectiveVal = if (q.estCodeUnique && curVal.isBlank()) codeUnique else curVal
            val selected = selectedOptions[q.idQuestion] ?: emptyList()
            val error = fieldErrors[q.idQuestion]

            SurveyQuestionInputField(
                index = index + 1,
                questionWithOptions = qItem,
                textValue = effectiveVal,
                selectedOptions = selected,
                fieldError = error,
                onTextChanged = { onTextChanged(q.idQuestion, it) },
                onSingleOptionSelected = { optId, optText -> onSingleOptionSelected(q.idQuestion, optId, optText) },
                onMultiOptionToggled = { optId -> onMultiOptionToggled(q.idQuestion, optId) }
            )
        }

        // Pied de page personnalisé (Boutons d'action, enregistrement, etc.)
        if (footerContent != null) {
            item {
                footerContent()
            }
        }
    }
}

/**
 * En-tête de formulaire avec titre et barre de progression interactive
 */
@Composable
fun SurveyFormHeaderSection(
    title: String,
    description: String?,
    filledCount: Int,
    totalCount: Int,
    progressFraction: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barre de progression du formulaire
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progression de la saisie",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$filledCount / $totalCount champs",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (filledCount == totalCount) StatusSuccess else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val animatedProgress by animateFloatAsState(
                targetValue = progressFraction.coerceIn(0f, 1f),
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "surveyProgress"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (animatedProgress >= 1f) StatusSuccess else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * Section Identification du répondant avec Code Unique et Date
 */
@Composable
fun SurveyRespondentInputSection(
    codeUnique: String,
    dateEnquete: String,
    codeError: String?,
    dateError: String?,
    onCodeChanged: (String) -> Unit,
    onGenerateCode: () -> Unit,
    onDateChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
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
                    text = "Code unique du ménage / répondant et date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Champ 1: Code Unique
        SurveyAnimatedUnderlineField(
            value = codeUnique,
            onValueChange = onCodeChanged,
            label = { Text("Code unique du répondant") },
            leadingIcon = {
                Icon(
                    imageVector = if (codeError != null) Icons.Default.Warning else Icons.Default.Badge,
                    contentDescription = null,
                    tint = if (codeError != null) StatusError else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = onGenerateCode,
                    modifier = Modifier.testTag("btn_generate_respondent_code")
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "Générer un nouveau code",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            supportingText = if (codeError != null) {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = codeError,
                            color = StatusError,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else null,
            isError = codeError != null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field_survey_code_unique"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Champ 2: Date de l'enquête
        SurveyAnimatedUnderlineField(
            value = dateEnquete,
            onValueChange = onDateChanged,
            label = { Text("Date de l'enquête (AAAA-MM-JJ)") },
            leadingIcon = {
                Icon(
                    imageVector = if (dateError != null) Icons.Default.Warning else Icons.Default.Today,
                    contentDescription = null,
                    tint = if (dateError != null) StatusError else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        onDateChanged(today)
                    },
                    modifier = Modifier.testTag("btn_set_today_date")
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = "Définir à aujourd'hui",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            supportingText = if (dateError != null) {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateError,
                            color = StatusError,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else null,
            isError = dateError != null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field_survey_date_enquete"),
            singleLine = true
        )
    }
}

/**
 * Question Input Field dispatcher that selects and renders the appropriate UI widget
 * for a survey question according to its data type.
 */
@Composable
fun SurveyQuestionInputField(
    index: Int,
    questionWithOptions: QuestionWithOptions,
    textValue: String,
    selectedOptions: List<Long>,
    fieldError: String?,
    onTextChanged: (String) -> Unit,
    onSingleOptionSelected: (Long, String) -> Unit,
    onMultiOptionToggled: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val q = questionWithOptions.question
    val options = questionWithOptions.options

    val qType = q.type.trim().uppercase()
    val qLabel = q.libelle.trim().lowercase()

    val isAnswered = when (qType) {
        "CHOIX_UNIQUE" -> selectedOptions.isNotEmpty() || textValue.isNotBlank()
        "CHOIX_MULTIPLE" -> selectedOptions.isNotEmpty()
        else -> textValue.isNotBlank()
    }

    val isValid = isAnswered && fieldError == null

    val isPhoneQuestion = (qType == "TEXTE" || qType == "NOMBRE") && (
            qLabel.contains("téléphone") || qLabel.contains("telephone") ||
                    qLabel.contains("contact") || qLabel.contains("phone") ||
                    qLabel.contains("mobile") || qLabel.contains("finday")
            )

    val isEmailQuestion = qType == "TEXTE" && (
            qLabel.contains("email") || qLabel.contains("courriel") || qLabel.contains("e-mail")
            )

    val isImageType = qType in listOf("IMAGE", "PHOTO", "PICTURE", "CAMERA") ||
            qType.contains("IMAGE") || qType.contains("PHOTO") ||
            (qType == "TEXTE" && (qLabel.contains("photo") || qLabel.contains("sary") || qLabel.contains("image")))

    val isAudioType = qType in listOf("AUDIO", "VOCAL", "SON", "VOICE", "ENREGISTREMENT", "MICRO") ||
            qType.contains("AUDIO") || qType.contains("VOCAL") || qType.contains("SON") ||
            (qType == "TEXTE" && (qLabel.contains("audio") || qLabel.contains("vocal") || qLabel.contains("enregistrement") || qLabel.contains("feo")))

    val isRatingType = (qType == "NOMBRE" || qType == "NOTE" || qType == "RATING") && (
            qLabel.contains("note") || qLabel.contains("satisfaction") || qLabel.contains("rating") || qLabel.contains("etoile") || qLabel.contains("étoile")
            )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // En-tête de question avec Numéro, Libellé et Indicateur de validation
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
                    else if (isValid) StatusSuccess
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
                    contentDescription = "Champ valide",
                    tint = StatusSuccess,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Corps du champ selon son type
        when {
            isImageType -> {
                SurveyImageInput(
                    questionId = q.idQuestion,
                    value = textValue,
                    onImageChanged = onTextChanged
                )
            }

            isAudioType -> {
                SurveyAudioInput(
                    questionId = q.idQuestion,
                    value = textValue,
                    onAudioChanged = onTextChanged
                )
            }

            isRatingType -> {
                SurveyRatingInputField(
                    value = textValue.toIntOrNull() ?: 0,
                    onRatingChanged = { onTextChanged(it.toString()) },
                    maxRating = 5
                )
            }

            qType == "NOMBRE" -> {
                SurveyNumberInputField(
                    value = textValue,
                    onValueChange = onTextChanged,
                    isError = fieldError != null,
                    testTag = "survey_number_input_${q.idQuestion}"
                )
            }

            qType == "DATE" -> {
                SurveyDateInputField(
                    value = textValue,
                    onValueChange = onTextChanged,
                    isError = fieldError != null,
                    testTag = "survey_date_input_${q.idQuestion}"
                )
            }

            isPhoneQuestion -> {
                SurveyPhoneInputField(
                    value = textValue,
                    onValueChange = onTextChanged,
                    isError = fieldError != null,
                    testTag = "survey_phone_input_${q.idQuestion}"
                )
            }

            isEmailQuestion -> {
                SurveyEmailInputField(
                    value = textValue,
                    onValueChange = onTextChanged,
                    isError = fieldError != null,
                    testTag = "survey_email_input_${q.idQuestion}"
                )
            }

            qType == "CHOIX_UNIQUE" -> {
                SurveySingleChoiceInputField(
                    options = options,
                    selectedOptionId = selectedOptions.firstOrNull(),
                    textValue = textValue,
                    onOptionSelected = onSingleOptionSelected,
                    onCustomTextChanged = onTextChanged,
                    testTagPrefix = "survey_single_choice_${q.idQuestion}"
                )
            }

            qType == "CHOIX_MULTIPLE" -> {
                SurveyMultipleChoiceInputField(
                    options = options,
                    selectedOptionIds = selectedOptions,
                    onOptionToggled = onMultiOptionToggled,
                    testTagPrefix = "survey_multi_choice_${q.idQuestion}"
                )
            }

            else -> {
                // Texte standard ou paragraphe
                SurveyTextInputField(
                    value = textValue,
                    onValueChange = onTextChanged,
                    isError = fieldError != null,
                    singleLine = !q.libelle.lowercase().contains("remarque") && !q.libelle.lowercase().contains("commentaire"),
                    testTag = "survey_text_input_${q.idQuestion}"
                )
            }
        }

        // Message d'erreur visible si présent
        if (fieldError != null) {
            SurveyVisualErrorMessage(message = fieldError)
        }
    }
}

/**
 * Champ texte avec soulignement animé Material 3
 */
@Composable
fun SurveyTextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    singleLine: Boolean = true,
    placeholder: String = "Saisir votre réponse...",
    testTag: String? = null,
    modifier: Modifier = Modifier
) {
    SurveyAnimatedUnderlineField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onValueChange("") },
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
        isError = isError,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4,
        modifier = modifier.fillMaxWidth(),
        testTag = testTag
    )
}

/**
 * Champ numérique
 */
@Composable
fun SurveyNumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    testTag: String? = null,
    modifier: Modifier = Modifier
) {
    SurveyAnimatedUnderlineField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' || it == '-' }) {
                onValueChange(newValue)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        placeholder = {
            Text(
                "Ex: 25",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onValueChange("") },
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
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        testTag = testTag
    )
}

/**
 * Champ date avec raccourci aujourd'hui
 */
@Composable
fun SurveyDateInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    testTag: String? = null,
    modifier: Modifier = Modifier
) {
    SurveyAnimatedUnderlineField(
        value = value,
        onValueChange = onValueChange,
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
                tint = if (isError) StatusError else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    onValueChange(today)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = "Aujourd'hui",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        testTag = testTag
    )
}

/**
 * Champ téléphone
 */
@Composable
fun SurveyPhoneInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    testTag: String? = null,
    modifier: Modifier = Modifier
) {
    SurveyAnimatedUnderlineField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }) {
                onValueChange(newValue)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        placeholder = {
            Text(
                "Ex: 034 00 000 00 / +261...",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = if (isError) StatusError else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onValueChange("") },
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
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        testTag = testTag
    )
}

/**
 * Champ Email
 */
@Composable
fun SurveyEmailInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    testTag: String? = null,
    modifier: Modifier = Modifier
) {
    SurveyAnimatedUnderlineField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        placeholder = {
            Text(
                "exemple@domaine.com",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = if (isError) StatusError else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onValueChange("") },
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
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        testTag = testTag
    )
}

/**
 * Choix unique sous forme de cartes d'options cliquables avec RadioButton et champ dynamique pour Autre
 */
@Composable
fun SurveySingleChoiceInputField(
    options: List<OptionEntity>,
    selectedOptionId: Long?,
    textValue: String,
    onOptionSelected: (Long, String) -> Unit,
    onCustomTextChanged: (String) -> Unit,
    testTagPrefix: String = "single_choice",
    modifier: Modifier = Modifier
) {
    fun isAutreOption(libelle: String): Boolean {
        val clean = libelle.trim().lowercase()
        return clean == "autre" || clean == "autres" || clean == "hafa" || clean == "other" ||
                clean.startsWith("autre") || clean.startsWith("hafa") ||
                clean.contains("préciser") || clean.contains("preciser") ||
                clean.contains("à préciser") || clean.contains("a preciser")
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            val isSelected = selectedOptionId == opt.idOption || (selectedOptionId == null && textValue.trim().equals(opt.libelle.trim(), ignoreCase = true)) || textValue.startsWith("${opt.libelle}:")
            val isAutre = isAutreOption(opt.libelle)

            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { onOptionSelected(opt.idOption, opt.libelle) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTagPrefix}_opt_${opt.idOption}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = opt.libelle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Saisie automatique du champ si Autre est sélectionné
                AnimatedVisibility(
                    visible = isSelected && isAutre,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val customValue = if (textValue.startsWith("${opt.libelle}:")) textValue.substringAfter("${opt.libelle}:").trim() else if (textValue != opt.libelle) textValue else ""
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp)
                    ) {
                        SurveyAnimatedUnderlineField(
                            value = customValue,
                            onValueChange = onCustomTextChanged,
                            label = { Text("Préciser pour « ${opt.libelle} »") },
                            placeholder = { Text("Veuillez préciser votre réponse...") },
                            singleLine = true,
                            testTag = "${testTagPrefix}_autre_input"
                        )
                    }
                }
            }
        }

        // Si aucune option configurée, permettre la saisie manuelle
        if (options.isEmpty()) {
            SurveyTextInputField(
                value = textValue,
                onValueChange = onCustomTextChanged,
                placeholder = "Saisir la valeur du choix..."
            )
        }
    }
}

/**
 * Choix multiple sous forme de cases à cocher avec surface tactile et champ dynamique pour Autre
 */
@Composable
fun SurveyMultipleChoiceInputField(
    options: List<OptionEntity>,
    selectedOptionIds: List<Long>,
    onOptionToggled: (Long) -> Unit,
    testTagPrefix: String = "multi_choice",
    modifier: Modifier = Modifier
) {
    fun isAutreOption(libelle: String): Boolean {
        val clean = libelle.trim().lowercase()
        return clean == "autre" || clean == "autres" || clean == "hafa" || clean == "other" ||
                clean.startsWith("autre") || clean.startsWith("hafa") ||
                clean.contains("préciser") || clean.contains("preciser") ||
                clean.contains("à préciser") || clean.contains("a preciser")
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            val isSelected = selectedOptionIds.contains(opt.idOption)
            val isAutre = isAutreOption(opt.libelle)

            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { onOptionToggled(opt.idOption) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTagPrefix}_opt_${opt.idOption}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = opt.libelle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Champ d'évaluation / Étoiles de satisfaction (1 à 5)
 */
@Composable
fun SurveyRatingInputField(
    value: Int,
    onRatingChanged: (Int) -> Unit,
    maxRating: Int = 5,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxRating) {
            val isSelected = i <= value
            Surface(
                onClick = { onRatingChanged(if (value == i) 0 else i) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFFF59E0B) else Color.Transparent),
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Étoile $i",
                        tint = if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (value > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$value / $maxRating",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF59E0B)
            )
        }
    }
}

/**
 * Message d'erreur visuel élégant sous le champ
 */
@Composable
fun SurveyVisualErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    if (message.contains("obligatoire", ignoreCase = true) || message.contains("ce champ", ignoreCase = true)) {
        return
    }
    Row(
        modifier = modifier.padding(start = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = StatusError,
            modifier = Modifier.size(13.dp)
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

/**
 * Champ Material 3 avec soulignement animé réactif et personnalisé
 */
@Composable
fun SurveyAnimatedUnderlineField(
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

    val animatedLineWidth by animateFloatAsState(
        targetValue = if (isFocused || isError) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "animated_underline_width"
    )

    val animatedLineHeight by animateDpAsState(
        targetValue = if (isFocused || isError) 2.5.dp else 1.dp,
        animationSpec = tween(durationMillis = 220),
        label = "animated_underline_height"
    )

    val underlineColor = if (isError) StatusError else activeColor
    val baseUnderlineColor = if (isError) StatusError.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
                    .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            )

            // Soulignement animé
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(baseUnderlineColor)
                )

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

        if (supportingText != null) {
            Box(modifier = Modifier.padding(start = 8.dp, top = 4.dp, end = 8.dp)) {
                supportingText()
            }
        }
    }
}
