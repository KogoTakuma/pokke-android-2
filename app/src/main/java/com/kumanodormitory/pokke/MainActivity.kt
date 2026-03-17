package com.kumanodormitory.pokke

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kumanodormitory.pokke.ui.PokkeApp
import com.kumanodormitory.pokke.ui.theme.PokkeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PokkeTheme {
                PokkeApp()
            }
        }
    }
}
