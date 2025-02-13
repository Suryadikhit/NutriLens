package com.example.nutrilens.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.nutrilens.network.RetrofitClient
import com.example.nutrilens.viewmodel.Product
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Products") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                isLoading = true
                coroutineScope.launch {
                    try {
                        val response = RetrofitClient.apiService.getProductsByName(searchQuery)
                        products = response
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(products) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        onClick = { navController.navigate("details/${product.name}") }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Image(
                                painter = rememberAsyncImagePainter(model = product.imageUrl),
                                contentDescription = product.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = product.name.toString(), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
