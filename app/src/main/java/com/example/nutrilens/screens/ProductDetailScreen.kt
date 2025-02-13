package com.example.nutrilens.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
                .padding(16.dp),
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
    val categorizedIngredients = categorizeIngredients(product.ingredients ?: "")
    val highlightedAdditives = highlightAdditives(product.additives ?: emptyList())

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {

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
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(4.dp)
                )
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        " ${product.name ?: "No Name"}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        " Brand: ${product.brand ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        " Quantity: ${product.quantity ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        " Barcode: ${product.barcode ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Overview
        Text(
            "🔍 Overview",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
            modifier = Modifier.padding(8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "🥗 Nutri-Score: ${product.nutriScore ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "📊 NOVA Score: ${product.novaScore ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Nutrition
        product.nutrition?.let { nutrition ->
            Text(
                "🍎 Nutrition Facts (per 100g)",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                modifier = Modifier.padding(8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚡ Energy: ${nutrition["energy-kcal_100g"] ?: "N/A"} kcal", modifier = Modifier.padding(bottom = 4.dp))
                    Text("🍞 Carbs: ${nutrition["carbohydrates_100g"] ?: "N/A"}g", modifier = Modifier.padding(bottom = 4.dp))
                    Text("🍗 Proteins: ${nutrition["proteins_100g"] ?: "N/A"}g", modifier = Modifier.padding(bottom = 4.dp))
                    Text("🍬 Sugars: ${nutrition["sugars_100g"] ?: "N/A"}g", modifier = Modifier.padding(bottom = 4.dp))
                    Text("🧈 Fat: ${nutrition["fat_100g"] ?: "N/A"}g")
                }
            }
        }

        // Categorized Ingredients
        Text(
            "🧪 Ingredients",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
            modifier = Modifier.padding(8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                categorizedIngredients.forEach { (category, items) ->
                    Text("$category:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    items.forEach { ingredient ->
                        Text("• $ingredient", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Highlighted Additives
        product.additives?.let { additives ->
            Text(
                "⚗️ Additives",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                modifier = Modifier.padding(8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (additives.isEmpty()) {
                        Text(
                            "🎉 Good news! It seems there are no additives in this product.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                        )
                    } else {
                        highlightedAdditives.forEach { (additive, color) ->
                            Text(
                                additive,
                                style = MaterialTheme.typography.bodyMedium.copy(color = color),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Packaging
        product.packaging?.let { packaging ->
            Text(
                "📦 Packaging",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                modifier = Modifier.padding(8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(packaging, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Carbon Footprint
        product.carbonFootprint?.takeIf { it.isNotBlank() }?.let { carbonFootprint ->
            Text(
                "🌍 Carbon Footprint",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                modifier = Modifier.padding(8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(carbonFootprint, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } ?: run {
            Text(
                "🌍 Carbon Footprint: Data not available",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}


fun categorizeIngredients(ingredients: String): Map<String, List<String>> {
    val sweeteners = listOf(
        "sugar",
        "glucose",
        "fructose",
        "sucrose",
        "lactose",
        "maltose",
        "corn syrup",
        "aspartame",
        "saccharin",
        "sucralose",
        "stevia"
    )
    val preservatives = listOf(
        "sodium benzoate",
        "potassium sorbate",
        "calcium propionate",
        "sodium nitrite",
        "sodium nitrate",
        "benzoic acid",
        "sorbic acid"
    )
    val colors = listOf(
        "red 40",
        "yellow 5",
        "blue 1",
        "blue 2",
        "green 3",
        "caramel color",
        "annatto",
        "beta-carotene"
    )
    val emulsifiers =
        listOf("lecithin", "mono- and diglycerides", "polysorbate 80", "sorbitan monostearate")
    val stabilizers = listOf("xanthan gum", "guar gum", "pectin", "carrageenan")
    val antioxidants = listOf(
        "ascorbic acid",
        "tocopherols",
        "butylated hydroxytoluene (BHT)",
        "butylated hydroxyanisole (BHA)"
    )
    val flavorEnhancers =
        listOf("monosodium glutamate (MSG)", "disodium inosinate", "disodium guanylate")
    val thickeners = listOf("corn starch", "potato starch", "gelatin", "agar")
    val acids = listOf("citric acid", "lactic acid", "malic acid", "acetic acid")

    val categorized = mutableMapOf<String, MutableList<String>>()

    ingredients.split(",").map { it.trim() }.forEach { ingredient ->
        when {
            sweeteners.any {
                it.equals(
                    ingredient,
                    ignoreCase = true
                )
            } -> categorized.getOrPut("Sweeteners") { mutableListOf() }.add(ingredient)

            preservatives.any {
                it.equals(
                    ingredient,
                    ignoreCase = true
                )
            } -> categorized.getOrPut("Preservatives") { mutableListOf() }.add(ingredient)

            colors.any {
                it.equals(
                    ingredient,
                    ignoreCase = true
                )
            } -> categorized.getOrPut("Colors") { mutableListOf() }.add(ingredient)

            emulsifiers.any {
                it.equals(
                    ingredient,
                    ignoreCase = true
                )
            } -> categorized.getOrPut("Emulsifiers") { mutableListOf() }.add(ingredient)

            stabilizers.any {
                it.equals(
                    ingredient,
                    ignoreCase = true
                )
            } -> categorized.getOrPut("Stabilizers") { mutableListOf() }.add(ingredient)

            antioxidants.any {
                it.equals(
                    ingredient,
                    ignoreCase = true
                )
            } -> categorized.getOrPut("Antioxidants") { mutableListOf() }.add(ingredient)

            flavorEnhancers.any {
                it.equals(
                    ingredient,
                    ignoreCase = true
                )
            } -> categorized.getOrPut("Flavor Enhancers") { mutableListOf() }.add(ingredient)

            thickeners.any {
                it.equals(
                    ingredient,
                    ignoreCase = true
                )
            } -> categorized.getOrPut("Thickeners") { mutableListOf() }.add(ingredient)

            acids.any {
                it.equals(
                    ingredient,
                    ignoreCase = true
                )
            } -> categorized.getOrPut("Acids") { mutableListOf() }.add(ingredient)

            else -> categorized.getOrPut("Others") { mutableListOf() }.add(ingredient)
        }
    }

    return categorized
}


fun highlightAdditives(additives: List<String>): List<Pair<String, Color>> {
    val colorMap = mapOf(
        "e100" to Color.Red,
        "e101" to Color.Yellow,
        "e102" to Color(0xFFFFC107), // Amber
        "e110" to Color(0xFFFF9800), // Orange
        "e120" to Color(0xFFE91E63), // Pink
        "e122" to Color(0xFFD32F2F), // Deep Red
        "e129" to Color(0xFFFF5722), // Deep Orange
        "e133" to Color(0xFF2196F3), // Blue
        "e140" to Color(0xFF4CAF50), // Green
        "e150" to Color(0xFF795548), // Brown
        "e200" to Color.Yellow,
        "e210" to Color(0xFF9E9E9E), // Gray
        "e211" to Color(0xFF607D8B), // Blue Gray
        "e220" to Color(0xFFCDDC39), // Lime
        "e250" to Color(0xFFF44336), // Red
        "e270" to Color(0xFF00BCD4), // Cyan
        "e300" to Color.Green,
        "e301" to Color(0xFF388E3C), // Dark Green
        "e320" to Color(0xFF9C27B0), // Purple
        "e330" to Color(0xFFFFEB3B), // Bright Yellow
        "e420" to Color(0xFF673AB7), // Deep Purple
        "e450" to Color(0xFF03A9F4), // Light Blue
        "e471" to Color(0xFFFFC107), // Amber
        "e500" to Color(0xFFFFEB3B), // Bright Yellow
        "e621" to Color(0xFF9C27B0)  // Purple
    )

    return additives.map { additive ->
        val color = colorMap[additive.lowercase()] ?: Color.Gray
        additive to color
    }
}
