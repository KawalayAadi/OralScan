package com.oralscan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.oralscan.app.ui.OralScanApp
import com.oralscan.app.ui.theme.OralScanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OralScanTheme {
                OralScanApp()
            }
        }
    }
}
