package ru.platformasport.club

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import ru.platformasport.club.ui.navigation.PlatformaRoot
import ru.platformasport.club.ui.theme.Graphite
import ru.platformasport.club.ui.theme.PlatformaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PlatformaTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Graphite),
                    color = Graphite
                ) {
                    PlatformaRoot()
                }
            }
        }
    }
}
