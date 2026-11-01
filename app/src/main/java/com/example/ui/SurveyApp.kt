package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import com.example.ui.components.AppNavigationDrawer
import com.example.ui.components.pressScaleEffect
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SurveyFormScreen
import com.example.ui.screens.SurveyListScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.theme.StatusPending
import kotlinx.coroutines.launch

@Composable
fun SurveyApp(
    viewModel: SurveyViewModel,
    modifier: Modifier = Modifier
) {
    var showSplash by remember { mutableStateOf(true) }

    AnimatedContent(
        targetState = showSplash,
        transitionSpec = {
            fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(400))
        },
        label = "splashTransition"
    ) { isSplash ->
        if (isSplash) {
            SplashScreen(
                onSplashFinished = { showSplash = false },
                modifier = modifier
            )
        } else {
            SurveyAppContent(viewModel = viewModel, modifier = modifier)
        }
    }
}

@Composable
private fun SurveyAppContent(
    viewModel: SurveyViewModel,
    modifier: Modifier = Modifier
) {
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val allEnquetes by viewModel.allEnquetes.collectAsState()

    AnimatedContent(
        targetState = loggedInUser != null,
        transitionSpec = {
            if (targetState) {
                // Connexion (Login) : Élévation fluide vers le haut avec scale et rebond doux
                (slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { 90 }
                ) + fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.94f, animationSpec = tween(400))) togetherWith
                (slideOutVertically(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    targetOffsetY = { -60 }
                ) + fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 0.96f))
            } else {
                // Déconnexion (Logout) : Transition descendante fluide et retour au login
                (slideInVertically(
                    animationSpec = tween(380, easing = FastOutSlowInEasing),
                    initialOffsetY = { -70 }
                ) + fadeIn(animationSpec = tween(380)) + scaleIn(initialScale = 0.95f)) togetherWith
                (slideOutVertically(
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    targetOffsetY = { 70 }
                ) + fadeOut(animationSpec = tween(260)) + scaleOut(targetScale = 0.94f))
            }
        },
        label = "AuthTransition",
        modifier = Modifier.fillMaxSize()
    ) { isLoggedIn ->
        if (!isLoggedIn) {
            LoginScreen(viewModel = viewModel, modifier = modifier)
        } else {
            val pendingCount = allEnquetes.count { it.syncStatus == "EN_ATTENTE" || it.syncStatus == "ERREUR" }
            val coroutineScope = rememberCoroutineScope()
            var showLogoutDialog by remember { mutableStateOf(false) }
            val pagerState = rememberPagerState(initialPage = currentTab, pageCount = { 5 })

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    if (viewModel.currentTab.value != page) {
                        viewModel.setTab(page)
                    }
                }
            }

            LaunchedEffect(currentTab) {
                if (pagerState.currentPage != currentTab) {
                    pagerState.animateScrollToPage(currentTab)
                }
            }

            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val selectedDrawerItem = when (pagerState.currentPage) {
                0 -> "Annonce"
                1 -> "Enquêtes"
                2 -> "Nouvelle enquête"
                3 -> "Synchronisation"
                4 -> "Mon profil"
                else -> "Annonce"
            }

            // Handle system Back button: close dialog, close drawer, or return to home page
            BackHandler(enabled = showLogoutDialog || drawerState.isOpen || pagerState.currentPage != 0) {
                if (showLogoutDialog) {
                    showLogoutDialog = false
                } else if (drawerState.isOpen) {
                    coroutineScope.launch { drawerState.close() }
                } else if (pagerState.currentPage != 0) {
                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                viewModel.onUserActivity()
                            }
                        }
                    }
            ) {
                // Background image matching login & sidebar
                Image(
                    painter = painterResource(id = R.drawable.img_login_clean_bg_1792150740897),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.90f)
                            else Color.White.copy(alpha = 0.88f)
                        )
                )

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppNavigationDrawer(
                            user = loggedInUser,
                            isDarkMode = isDarkMode,
                            selectedItem = selectedDrawerItem,
                            pendingCount = pendingCount,
                            onItemSelected = { item ->
                                coroutineScope.launch {
                                    drawerState.close()
                                    when (item) {
                                        "Annonce", "Annonces" -> pagerState.animateScrollToPage(0)
                                        "Enquêtes" -> pagerState.animateScrollToPage(1)
                                        "Nouvelle enquête" -> {
                                            viewModel.startNewSurvey()
                                            pagerState.animateScrollToPage(2)
                                        }
                                        "Synchronisation" -> pagerState.animateScrollToPage(3)
                                        "Mon profil" -> pagerState.animateScrollToPage(4)
                                    }
                                }
                            },
                            onLogout = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    showLogoutDialog = true
                                }
                            },
                            onToggleDarkMode = {
                                viewModel.toggleDarkMode()
                            }
                        )
                    }
                ) {
                    Scaffold(
                        modifier = modifier.fillMaxSize(),
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            key = { it }
                        ) { page ->
                            when (page) {
                                0 -> HomeScreen(
                                    viewModel = viewModel,
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                                )
                                1 -> SurveyListScreen(
                                    viewModel = viewModel,
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                                )
                                2 -> SurveyFormScreen(
                                    viewModel = viewModel,
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                                )
                                3 -> SyncScreen(
                                    viewModel = viewModel,
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                                )
                                4 -> ProfileScreen(
                                    viewModel = viewModel,
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                                )
                            }
                        }
                    }
                }

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
    }
}
}
