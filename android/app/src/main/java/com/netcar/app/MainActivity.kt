package com.netcar.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.notifications.ReminderNotificationHelper
import com.netcar.app.ui.navigation.NetCarApp
import com.netcar.app.ui.theme.NetCARTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: NetCarRepository

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Channel is already created; repository will post due notifications when granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentActivityRef = WeakReference(this)
        ReminderNotificationHelper.ensureChannelCreated(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        handleIncomingAuthIntent(intent)

        enableEdgeToEdge()
        setContent {
            NetCARTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NetCarApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingAuthIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        currentActivityRef = WeakReference(this)
    }

    private fun handleIncomingAuthIntent(incomingIntent: Intent?) {
        val dataUri = incomingIntent?.data?.toString() ?: return
        if (dataUri.contains("id_token=") ||
            dataUri.contains("firebaseauth") ||
            dataUri.contains("deep_link_id=") ||
            dataUri.startsWith("netcar://auth")
        ) {
            lifecycleScope.launch {
                runCatching {
                    repository.completeOAuthFromCallbackUri(dataUri)
                }
            }
        }
    }

    companion object {
        @Volatile
        private var currentActivityRef: WeakReference<MainActivity>? = null

        fun launchExternalUrl(context: Context, url: String): Boolean {
            return runCatching {
                val parsedUri = Uri.parse(url)
                val activity = currentActivityRef?.get()
                if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                    val intent = Intent(Intent.ACTION_VIEW, parsedUri)
                    activity.startActivity(intent)
                    true
                } else {
                    val appCtx = context.applicationContext
                    val intent = Intent(Intent.ACTION_VIEW, parsedUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    appCtx.startActivity(intent)
                    true
                }
            }.getOrDefault(false)
        }
    }
}
