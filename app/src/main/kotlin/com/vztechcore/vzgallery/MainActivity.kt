package com.vztechcore.vzgallery

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.data.ThemeMode
import com.vztechcore.vzgallery.ui.screens.home.HomeScreen
import com.vztechcore.vzgallery.ui.screens.welcome.WelcomeScreen
import com.vztechcore.vzgallery.ui.theme.VzGalleryTheme
import kotlinx.coroutines.launch

// IMPORTANTE: FragmentActivity é OBRIGATÓRIA pra BiometricPrompt 1.1.0
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { Prefs(context) }
            val themeMode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

            VzGalleryTheme(themeMode = themeMode) {
                VzGalleryApp(prefs = prefs)
            }
        }
    }
}

@Composable
fun VzGalleryApp(prefs: Prefs) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val welcomeShown by prefs.welcomeShown.collectAsState(initial = false)

    val fullAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    fullAccessLauncher.launch(intent)
                } catch (_: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        fullAccessLauncher.launch(intent)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    if (!welcomeShown) {
        WelcomeScreen(
            onPermissionGranted = {
                scope.launch {
                    prefs.setWelcomeShown(true)
                    prefs.setPermissionGranted(true)
                }
            }
        )
    } else {
        HomeScreen()
    }
}