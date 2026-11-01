package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserEntity
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending

/**
 * Navigation Drawer with main app destinations:
 * - User Header with avatar, name, email
 * - Annonce (Annonces et communications officielles)
 * - Enquêtes (Surveys list with pending badge)
 * - Nouvelle enquête (Create Survey)
 * - Synchronisation (Sync)
 * - Mon profil (Profile)
 * - Déconnexion & Mode sombre
 */
@Composable
fun AppNavigationDrawer(
    user: UserEntity?,
    isDarkMode: Boolean,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    onLogout: () -> Unit,
    onToggleDarkMode: () -> Unit,
    pendingCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val rawDisplayName = listOfNotNull(
        user?.nom?.takeIf { it.isNotBlank() },
        user?.prenom?.takeIf { it.isNotBlank() }
    ).joinToString(" ").trim()

    val displayName = if (rawDisplayName.isNotBlank()) rawDisplayName else "RAHARINIRINA Juvance"
    val displayEmail = if (!user?.email.isNullOrBlank()) user!!.email else "juvanceraharinirina@gmail.com"
    val userInitial = displayName.firstOrNull()?.uppercase() ?: "J"

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp),
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image matching Login screen
            Image(
                painter = painterResource(id = R.drawable.img_login_clean_bg_1792150740897),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Translucent overlay matching Login screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.90f)
                        else Color.White.copy(alpha = 0.88f)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
            ) {
            Spacer(modifier = Modifier.height(28.dp))

            // User Header Profile Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Avatar
                EnqueteurAvatar(
                    photoUrl = user?.photoUrl,
                    photo = user?.photo,
                    size = 84.dp,
                    showBorder = true,
                    borderColor = Color(0xFF64B5F6),
                    borderWidth = 2.5.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // User Full Name
                Text(
                    text = displayName,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.2).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // User Email
                Text(
                    text = displayEmail,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Divider line
            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            Spacer(modifier = Modifier.height(8.dp))

            // Main Navigation Items
            DrawerMenuItem(
                title = "Annonce",
                icon = Icons.Default.Campaign,
                isSelected = selectedItem == "Annonce" || selectedItem == "Annonces",
                testTag = "tab_home",
                onClick = { onItemSelected("Annonce") }
            )

            DrawerMenuItem(
                title = "Enquêtes",
                icon = Icons.Default.Description,
                isSelected = selectedItem == "Enquêtes",
                badgeCount = pendingCount,
                testTag = "tab_records",
                onClick = { onItemSelected("Enquêtes") }
            )

            DrawerMenuItem(
                title = "Nouvelle enquête",
                icon = Icons.Default.Add,
                isSelected = selectedItem == "Nouvelle enquête",
                testTag = "tab_new_survey",
                onClick = { onItemSelected("Nouvelle enquête") }
            )

            DrawerMenuItem(
                title = "Synchronisation",
                icon = Icons.Default.Sync,
                isSelected = selectedItem == "Synchronisation",
                testTag = "tab_sync",
                onClick = { onItemSelected("Synchronisation") }
            )

            DrawerMenuItem(
                title = "Mon profil",
                icon = Icons.Default.Person,
                isSelected = selectedItem == "Mon profil",
                testTag = "tab_profile",
                onClick = { onItemSelected("Mon profil") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Divider line before actions section
            HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 1.2.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Déconnexion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.2.dp, StatusError, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Déconnexion",
                        tint = StatusError,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(18.dp))
                Text(
                    text = "Déconnexion",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = StatusError
                )
            }

            // Mode sombre (Dark mode toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleDarkMode() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.2.dp, Color(0xFF94A3B8), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = "Mode sombre",
                            tint = Color(0xFF0288D1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    Text(
                        text = "Mode sombre",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF0288D1),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFFE2E8F0)
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}

@Composable
private fun DrawerMenuItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    badgeCount: Int = 0,
    testTag: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleEffect(0.97f)
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 13.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    if (isSelected) Color(0xFFE0F2FE) else Color.Transparent,
                    CircleShape
                )
                .border(
                    width = 1.2.dp,
                    color = if (isSelected) Color(0xFF0288D1) else Color(0xFF94A3B8),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color(0xFF0288D1) else Color(0xFF475569),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(18.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF0288D1) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (badgeCount > 0) {
            Surface(
                shape = CircleShape,
                color = StatusPending
            ) {
                Text(
                    text = "$badgeCount",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
