package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.auth.AuthScreen
import com.example.auth.attemptAutoSignIn
import com.example.notifications.ClutchXNotificationManager
import com.example.ui.ClutchXApp
import com.example.ui.theme.ClutchXTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    private val pendingNotificationRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingNotificationRoute.value = intent?.getStringExtra(ClutchXNotificationManager.EXTRA_OPEN_ROUTE)
        setContent {
            ClutchXTheme {
                AppContent(
                    pendingRoute = pendingNotificationRoute.value,
                    onPendingRouteConsumed = { pendingNotificationRoute.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationRoute.value = intent.getStringExtra(ClutchXNotificationManager.EXTRA_OPEN_ROUTE)
    }
}

@Composable
fun AppContent(
    pendingRoute: String? = null,
    onPendingRouteConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = { },
                scope = this
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            ClutchXApp(
                currentUserId = user.uid,
                currentUserEmail = user.email.orEmpty(),
                pendingDeepLinkRoute = pendingRoute,
                onDeepLinkConsumed = onPendingRouteConsumed
            )
        }
    }
}
