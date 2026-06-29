package com.bazaarhelper.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.bazaarhelper.app.core.security.BiometricHelper
import com.bazaarhelper.app.presentation.navigation.BazaarNavGraph
import com.bazaarhelper.app.presentation.theme.BazaarHelperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val biometricHelper = BiometricHelper(this)
        
        setContent {
            val canAuthenticate = remember { biometricHelper.canAuthenticate() }
            var isAuthenticated by remember { mutableStateOf(!canAuthenticate) }
            var authError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(canAuthenticate) {
                if (canAuthenticate && !isAuthenticated) {
                    biometricHelper.authenticate(
                        activity = this@MainActivity,
                        onSuccess = { isAuthenticated = true },
                        onError = { error -> authError = error }
                    )
                }
            }

            BazaarHelperTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isAuthenticated) {
                        val navController = rememberNavController()
                        BazaarNavGraph(navController = navController)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (authError != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = stringResource(R.string.auth_needed),
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            authError = null
                                            biometricHelper.authenticate(
                                                activity = this@MainActivity,
                                                onSuccess = { isAuthenticated = true },
                                                onError = { error -> authError = error }
                                            )
                                        }
                                    ) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            } else {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}
