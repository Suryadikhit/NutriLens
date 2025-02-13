package com.example.nutrilens.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                IconButton(onClick = { navController.popBackStack(route = "scan", inclusive = false) }) {
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Row with Product Image and Info Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
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

            // Product Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = product.name ?: "No Name", style = MaterialTheme.typography.headlineSmall)
                    Text(text = "Brand: ${product.brand ?: "Unknown"}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Quantity: ${product.quantity ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Barcode: ${product.barcode ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Overview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Nutri-Score: ${product.nutriScore ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                Text("NOVA Score: ${product.novaScore ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Nutrition Card
        product.nutrition?.let { nutrition ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nutrition Facts (per 100g)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Energy: ${nutrition["energy-kcal_100g"] ?: "N/A"} kcal")
                    Text("Carbs: ${nutrition["carbohydrates_100g"] ?: "N/A"}g")
                    Text("Proteins: ${nutrition["proteins_100g"] ?: "N/A"}g")
                    Text("Sugars: ${nutrition["sugars_100g"] ?: "N/A"}g")
                    Text("Fat: ${nutrition["fat_100g"] ?: "N/A"}g")
                }
            }
        }

        // Ingredients Card
        product.ingredients?.let { ingredients ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ingredients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(ingredients, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
