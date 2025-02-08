package com.example.nutrilens.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.nutrilens.viewmodel.ProductViewModel

@Composable
fun ProductDetailScreen(navController: NavController, barcode: String) {
    val viewModel: ProductViewModel = viewModel()
    val product by viewModel.product.observeAsState()
    val errorMessage by viewModel.errorMessage.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(initial = false)  // ✅ Observe loading state

    LaunchedEffect(barcode) {
        viewModel.fetchProduct(barcode)  // ✅ No callback needed
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { navController.popBackStack() }) {
            Text("Back to Scan")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Text(text = "Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            }
            product != null -> {
                val painter = rememberAsyncImagePainter(product?.imageUrl)

                product?.imageUrl?.let {
                    Image(painter = painter, contentDescription = product?.name)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(text = "Name: ${product?.name ?: "No Name"}", style = MaterialTheme.typography.headlineMedium)
                Text(text = "Brand: ${product?.brand ?: "Unknown"}", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Ingredients: ${product?.ingredients ?: "Not available"}", style = MaterialTheme.typography.bodyMedium)

                // ✅ Displaying Nutritional Info
                product?.nutrition?.let { nutrition ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Nutrition Info:", style = MaterialTheme.typography.headlineSmall)
                    Text(text = "Energy: ${nutrition["energy-kcal_100g"]} kcal per 100g", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Carbohydrates: ${nutrition["carbohydrates_100g"]}g", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Proteins: ${nutrition["proteins_100g"]}g", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Sugars: ${nutrition["sugars_100g"]}g", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Fat: ${nutrition["fat_100g"]}g", style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> {
                Text("Product not found!", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
