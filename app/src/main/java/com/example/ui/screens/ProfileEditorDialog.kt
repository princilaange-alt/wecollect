package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserEntity
import com.example.ui.components.EnqueteurAvatar
import com.example.ui.components.pressScaleEffect

/**
 * Interface moderne, claire et intuitive pour l'éditeur d'informations de profil
 * Permet la modification du profil (Nom, Prénom, Téléphone, etc.)
 * et assure la synchronisation bidirectionnelle avec SQLite Room et Supabase.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditorDialog(
    user: UserEntity?,
    onDismiss: () -> Unit,
    onSaveUser: (UserEntity) -> Unit
) {
    val context = LocalContext.current

    val initialNom = user?.nom ?: ""
    val initialPrenom = user?.prenom ?: ""
    val initialTel = user?.telephone ?: ""
    val initialEmail = user?.email ?: ""
    val initialRole = user?.role ?: "ENQUETEUR"
    val initialStatut = user?.statut ?: "ACTIF"
    val initialRegion = user?.region?.ifBlank { "Analamanga" } ?: "Analamanga"
    val initialMatricule = user?.matricule ?: ""

    var nom by remember { mutableStateOf(initialNom) }
    var prenom by remember { mutableStateOf(initialPrenom) }
    var telephone by remember { mutableStateOf(initialTel) }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val userInitial = remember(prenom, nom) {
        val p = prenom.firstOrNull()?.uppercase() ?: ""
        val n = nom.firstOrNull()?.uppercase() ?: ""
        if (p.isNotEmpty() || n.isNotEmpty()) "$p$n" else "E"
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSaving,
            dismissOnClickOutside = !isSaving
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .imePadding(),
            shape = RectangleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header avec dégradé subtil et bouton fermer
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Éditer",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Error Message banner
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFF991B1B),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Scrollable Form Body
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Avatar & Status Preview
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        // Avatar Box
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(3.dp, Color(0xFF64B5F6)), CircleShape)
                                .background(Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            EnqueteurAvatar(
                                photoUrl = user?.photoUrl,
                                photo = user?.photo,
                                size = 112.dp,
                                showBorder = false
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = if (nom.isNotBlank() || prenom.isNotBlank()) "$nom $prenom".trim() else "Agent enquêteur",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = initialEmail.ifBlank { "Compte actif" },
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF64748B)
                            )
                        }
                        }
                    }

                    // Section 1 : Identité Personnelle
                    item {
                        FormSectionHeader(
                            title = "Identité personnelle",
                            icon = Icons.Default.Person
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = nom,
                            onValueChange = {
                                nom = it
                                errorMessage = null
                            },
                            label = { Text("Nom de famille") },
                            placeholder = { Text("ex: RAHARINIRINA") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0288D1),
                                focusedLabelColor = Color(0xFF0288D1)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = prenom,
                            onValueChange = {
                                prenom = it
                                errorMessage = null
                            },
                            label = { Text("Prénom(s)") },
                            placeholder = { Text("ex: Juvance") },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0288D1),
                                focusedLabelColor = Color(0xFF0288D1)
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    item {
                        OutlinedTextField(
                            value = telephone,
                            onValueChange = {
                                telephone = it
                                errorMessage = null
                            },
                            label = { Text("Numéro de téléphone portable") },
                            placeholder = { Text("034 58 128 88") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0288D1),
                                focusedLabelColor = Color(0xFF0288D1)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Sticky Bottom Action Bar with Save and Cancel
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        TextButton(
                            onClick = {
                                if (nom.trim().isBlank()) {
                                    errorMessage = "Veuillez saisir votre nom de famille."
                                    return@TextButton
                                }
                                if (prenom.trim().isBlank()) {
                                    errorMessage = "Veuillez saisir votre prénom."
                                    return@TextButton
                                }

                                isSaving = true
                                val updatedUser = (user ?: UserEntity(
                                    idUser = 1L,
                                    nom = nom.trim(),
                                    prenom = prenom.trim(),
                                    email = "",
                                    telephone = telephone.trim(),
                                    role = "ENQUETEUR",
                                    statut = "ACTIF",
                                    motDePasse = "",
                                    region = initialRegion,
                                    matricule = initialMatricule.takeIf { it.isNotBlank() }
                                )).copy(
                                    nom = nom.trim(),
                                    prenom = prenom.trim(),
                                    telephone = telephone.trim().takeIf { it.isNotBlank() },
                                    region = initialRegion,
                                    matricule = initialMatricule.takeIf { it.isNotBlank() }
                                )

                                onSaveUser(updatedUser)
                                isSaving = false
                                Toast.makeText(
                                    context,
                                    "Profil mis à jour !",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = Color(0xFF0288D1),
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Enregistrement...", color = Color(0xFF0288D1), fontSize = 15.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0288D1),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sauvegarder",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0288D1)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "X Annuler",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF0288D1),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.width(10.dp))
        HorizontalDivider(
            color = Color(0xFFE2E8F0),
            thickness = 1.dp,
            modifier = Modifier.weight(1f)
        )
    }
}
