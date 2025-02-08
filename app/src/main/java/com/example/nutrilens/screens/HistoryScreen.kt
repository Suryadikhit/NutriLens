package com.example.nutrilens.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nutrilens.database.HistoryDatabaseHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val historyDb = remember { HistoryDatabaseHelper(context) }

    // ✅ Use mutableStateListOf for better recomposition
    val searchHistory = remember { mutableStateListOf(*historyDb.getHistory().toTypedArray()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Search History") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (searchHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No search history available")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(searchHistory) { item ->
                        HistoryItem(
                            barcode = item.barcode,
                            productImage = item.imageUrl,
                            productName = item.productName,
                            onDelete = {
                                historyDb.deleteHistoryItem(item.barcode)
                                searchHistory.remove(item) // ✅ Remove item locally to avoid full re-fetch
                            },
                            onClick = {
                                navController.navigate("details/${item.barcode}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    barcode: String,
    productName: String?,
    productImage: String?,  // ✅ Product image support
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ✅ Product Image
                productImage?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Product Image",
                        modifier = Modifier
                            .size(60.dp)
                            .padding(end = 8.dp)
                    )
                }

                // ✅ Barcode & Product Name
                Column {
                    Text(text = barcode, style = MaterialTheme.typography.bodyLarge)
                    productName?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ✅ Delete Button
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
