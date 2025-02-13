package com.example.nutrilens.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // ✅ Fetch product from local DB first
    LaunchedEffect(barcode) {
        val localProduct = withContext(Dispatchers.IO) { historyDb.getProductByBarcode(barcode) }
        product = localProduct

        // ✅ Only fetch from API if not in local DB
        if (localProduct == null) {
            viewModel.fetchProduct(barcode)
        }
    }

    // ✅ Update UI & save API result to DB if found
    LaunchedEffect(apiProduct) {
        apiProduct?.let { newProduct ->
            if (product == null) {  // ✅ Avoid overwriting existing local data
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                product.imageUrl?.let { imageUrl ->
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = product.name,
                        modifier = Modifier
                            .size(180.dp)
                            .padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = product.name ?: "No Name",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Brand: ${product.brand ?: "Unknown"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                product.nutrition?.let { nutrition ->
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nutrition Facts (per 100g):",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column {
                        Text("Energy: ${nutrition["energy-kcal_100g"] ?: "N/A"} kcal")
                        Text("Carbs: ${nutrition["carbohydrates_100g"] ?: "N/A"}g")
                        Text("Proteins: ${nutrition["proteins_100g"] ?: "N/A"}g")
                        Text("Sugars: ${nutrition["sugars_100g"] ?: "N/A"}g")
                        Text("Fat: ${nutrition["fat_100g"] ?: "N/A"}g")
                    }
                }
            }
        }
    }
}
