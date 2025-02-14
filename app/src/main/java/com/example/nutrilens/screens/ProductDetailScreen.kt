package com.example.nutrilens.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.nutrilens.components.NutritionItem
import com.example.nutrilens.components.ProductScoreCards
import com.example.nutrilens.components.categorizeIngredients
import com.example.nutrilens.components.highlightAdditives
import com.example.nutrilens.database.HistoryDatabaseHelper
import com.example.nutrilens.viewmodel.Product
import com.example.nutrilens.viewmodel.ProductViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(navController: NavController, barcode: String) {
    val viewModel: ProductViewModel = viewModel()
    val context = LocalContext.current
    val historyDb = remember { HistoryDatabaseHelper(context) }

    var product by remember { mutableStateOf<Product?>(null) }
    val apiProduct by viewModel.product.observeAsState()
    val errorMessage by viewModel.errorMessage.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(initial = false)

    LaunchedEffect(barcode) {
        val localProduct = withContext(Dispatchers.IO) { historyDb.getProductByBarcode(barcode) }
        product = localProduct
        if (localProduct == null) {
            viewModel.fetchProduct(barcode)
        }
    }

    LaunchedEffect(apiProduct) {
        apiProduct?.let { newProduct ->
            if (product == null) {
                product = newProduct
                withContext(Dispatchers.IO) { historyDb.insertOrUpdateProduct(newProduct) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Product Details") }, navigationIcon = {
                IconButton(onClick = {
                    navController.popBackStack(
                        route = "scan",
                        inclusive = false
                    )
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                errorMessage != null -> Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )

                product != null -> ProductDetailsView(product!!)
                else -> Text("Product not found!", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
fun ProductDetailsView(product: Product) {
    val categorizedIngredients =
        categorizeIngredients(product.ingredients ?: "No ingredients available")
    val highlightedAdditives = highlightAdditives(product.additives ?: listOf("No additives"))

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp) // Adds spacing between items
    ) {
        item {
            // Product Image and Info Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                product.imageUrl?.let { imageUrl ->
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = product.name ?: "Product Image",
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    )
                } ?: Text(
                    "No image available",
                    modifier = Modifier.padding(8.dp),
                    color = Color.Gray
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            product.name ?: "No Name",
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 15.sp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            "Brand: ${product.brand ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            "Quantity: ${product.quantity ?: "N/A"}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            "Barcode: ${product.barcode ?: "N/A"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Overview
            SectionHeader("🔍 Overview")
            SectionCard {
                ProductScoreCards(product.nutriScore, product.novaScore)
            }

            // Nutrition Facts
            SectionHeader("🍎 Nutrition Facts (per 100g)")
            SectionCard {
                val nutrition = product.nutrition ?: emptyMap()
                if (nutrition.isEmpty()) {
                    Text("No nutrition data available", color = Color.Gray)
                } else {
                    NutritionItem("⚡ Energy", nutrition["energy-kcal_100g"].toString())
                    NutritionItem("🍞 Carbs", nutrition["carbohydrates_100g"].toString())
                    NutritionItem("🍗 Proteins", nutrition["proteins_100g"].toString())
                    NutritionItem("🍬 Sugars", nutrition["sugars_100g"].toString())
                    NutritionItem("🧈 Fat", nutrition["fat_100g"].toString())
                }
            }

            // Ingredients
            SectionHeader("🧪 Ingredients")
            SectionCard {
                if (categorizedIngredients.isEmpty()) {
                    Text("No ingredients available", color = Color.Gray)
                } else {
                    categorizedIngredients.forEach { (category, items) ->
                        Text(
                            "$category:",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        items.forEach { ingredient ->
                            Text("• $ingredient", Modifier.padding(bottom = 4.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // Additives
            SectionHeader("⚗️ Additives")
            SectionCard {
                if (highlightedAdditives.isEmpty() || highlightedAdditives == listOf("No additives")) {
                    Text(
                        "🎉 Good news! It seems there are no additives in this product.",
                        color = Color.White
                    )
                } else {
                    highlightedAdditives.forEach { (additive, color) ->
                        Text(additive, color = color, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }

            // Packaging
            SectionHeader("📦 Packaging")
            SectionCard {
                Text(product.packaging ?: "No packaging data available", color = Color.Gray)
            }

            // Carbon Footprint
            SectionHeader("🌍 Carbon Footprint")
            SectionCard {
                Text(product.carbonFootprint ?: "Data not available", color = Color.Gray)
            }
        }
    }
}
@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}



