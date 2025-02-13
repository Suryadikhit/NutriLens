package com.example.nutrilens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nutrilens.screens.HistoryScreen
import com.example.nutrilens.screens.ProductDetailScreen
import com.example.nutrilens.screens.ScanScreen
import com.example.nutrilens.screens.SearchScreen
import com.example.nutrilens.ui.theme.NutriLensTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NutriLensTheme {
                NutriLensApp()
            }
        }

    }

        @Composable
        fun NutriLensApp() {

            val navController = rememberNavController()

            NavHost(navController, startDestination = "scan") {
                composable("scan") { ScanScreen(navController) }
                composable("details/{barcode}") { backStackEntry ->
                    val barcode = backStackEntry.arguments?.getString("barcode")
                    ProductDetailScreen(navController, barcode ?: "")
                }
                composable("history") { HistoryScreen(navController) }
                composable("SearchScreen") { SearchScreen(navController) }
            }
        }
     }