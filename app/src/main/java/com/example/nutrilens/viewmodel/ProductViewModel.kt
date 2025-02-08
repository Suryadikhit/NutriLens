package com.example.nutrilens.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.nutrilens.database.HistoryDatabaseHelper
import com.example.nutrilens.network.RetrofitClient
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch

data class Product(
    @SerializedName("barcode") val barcode: String?,
    @SerializedName("product_name") val name: String?,
    @SerializedName("ingredients") val ingredients: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("brands") val brand: String?,
    @SerializedName("nutritional_info") val nutrition: Map<String, Any>? = emptyMap()
)

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val _product = MutableLiveData<Product?>()
    val product: LiveData<Product?> = _product

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val historyDb = HistoryDatabaseHelper(application.applicationContext) // ✅ Use Application Context

    fun fetchProduct(barcode: String) {
        Log.d("ProductViewModel", "Fetching product for barcode: $barcode")
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getProduct(barcode)
                Log.d("ProductViewModel", "API Response: $response")

                if (response.barcode != null) {
                    _product.value = response
                    _errorMessage.value = null
                    Log.d("ProductViewModel", "Product found: ${response.name}")

                    // ✅ Save product search history
                    historyDb.insertHistory(response.barcode, response.name, response.imageUrl)
                } else {
                    _errorMessage.value = "Product not found"
                    _product.value = null
                    Log.w("ProductViewModel", "Product not found for barcode: $barcode")
                }
            } catch (e: retrofit2.HttpException) {
                _errorMessage.value = "HTTP Error: ${e.code()} - ${e.message()}"
                Log.e("ProductViewModel", "HTTP Exception: ${e.message()}", e)
            } catch (e: java.net.SocketTimeoutException) {
                _errorMessage.value = "Request timed out. Try again."
                Log.e("ProductViewModel", "Timeout Exception: ${e.message}", e)
            } catch (e: java.net.UnknownHostException) {
                _errorMessage.value = "No internet connection."
                Log.e("ProductViewModel", "No Internet Exception: ${e.message}", e)
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
                Log.e("ProductViewModel", "Unknown Exception: ${e.message}", e)
            } finally {
                _isLoading.value = false
                Log.d("ProductViewModel", "API call completed. isLoading = false")
            }
        }
    }
}
