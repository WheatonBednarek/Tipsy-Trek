package com.cs407.tipsytrek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cs407.tipsytrek.ui.DrinkPage
import com.cs407.tipsytrek.ui.HomePage
import com.cs407.tipsytrek.ui.HomePageId
import com.cs407.tipsytrek.ui.SelectionScreen
import com.cs407.tipsytrek.ui.SelectionScreenId
import com.cs407.tipsytrek.ui.theme.TipsyTrekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        System.loadLibrary("liquidfun")
        System.loadLibrary("liquidfun_jni")

        setContent {
            val navController = rememberNavController()
            TipsyTrekTheme {
                NavHost(navController, startDestination = HomePageId) {
                    composable(HomePageId) { HomePage(navController) }
                    composable(SelectionScreenId) { SelectionScreen(navController) }
                    composable<Beverage> { backStack ->
                        DrinkPage(backStack.toRoute(), navController)
                    }
                }
            }
        }
    }
}