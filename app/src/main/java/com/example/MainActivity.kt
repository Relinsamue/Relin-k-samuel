package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CreatorAppUi
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainMenuViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup Full Screen Edge-To-Edge safe margins support
        enableEdgeToEdge()
        
        setContent {
            // Initialize the central creator's view model safely
            val viewModel: MainMenuViewModel = viewModel()
            val isDarkTheme = viewModel.isDarkTheme.collectAsState().value
            
            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    CreatorAppUi(viewModel = viewModel)
                }
            }
        }
    }
}
