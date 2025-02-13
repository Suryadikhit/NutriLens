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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class Product(
    @SerializedName("barcode") val barcode: String?,
    @SerializedName("product_name") val name: String?,
    @SerializedName("ingredients") val ingredients: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("brands") val brand: String?,
    @SerializedName("quantity") val quantity: String?,
    @SerializedName("nutri_score") val nutriScore: String?,
    @SerializedName("nova_score") val novaScore: String?,
    @SerializedName("additives") val additives: List<String>?,
    @SerializedName("packaging") val packaging: String?,
    @SerializedName("carbon_footprint") val carbonFootprint: String?,
    @SerializedName("nutritional_info") val nutrition: Map<String, Any>? = emptyMap()
)


class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val _product = MutableLiveData<Product?>()
    val product: LiveData<Product?> = _product

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val historyDb = HistoryDatabaseHelper(application.applicationContext) // ✅ Application Context

    fun fetchProduct(barcode: String) {
        Log.d("ProductViewModel", "Fetching product for barcode: $barcode")
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) { // ✅ Ensures network call is on IO thread
            try {
                val response = RetrofitClient.apiService.getProduct(barcode)
                Log.d("ProductViewModel", "API Response: $response")

                if (response.barcode.isNullOrEmpty()) {
                    _errorMessage.postValue("Product not found")
                    _product.postValue(null)
                    Log.w("ProductViewModel", "Product not found for barcode: $barcode")
                    return@launch
                }

                _product.postValue(response)
                _errorMessage.postValue(null)
                Log.d("ProductViewModel", "Product found: ${response.name}")

                // ✅ Save product search history on IO thread
                response.barcode.let { barcode ->
                    viewModelScope.launch(Dispatchers.IO) {
                        historyDb.insertOrUpdateProduct(
                            Product(
                                barcode = barcode,
                                name = response.name,
                                imageUrl = response.imageUrl,
                                brand = response.brand,
                                ingredients = response.ingredients,
                                quantity = response.quantity,
                                nutriScore = response.nutriScore,
                                novaScore = response.novaScore,
                                additives = response.additives,
                                packaging = response.packaging,
                                carbonFootprint = response.carbonFootprint,
                                nutrition = response.nutrition
                            )

                        )
                    }
                }


            } catch (e: retrofit2.HttpException) {
                _errorMessage.postValue("HTTP Error: ${e.code()} - ${e.message()}")
                Log.e("ProductViewModel", "HTTP Exception: ${e.message()}", e)
            } catch (e: java.net.SocketTimeoutException) {
                _errorMessage.postValue("Request timed out. Try again.")
                Log.e("ProductViewModel", "Timeout Exception: ${e.message}", e)
            } catch (e: java.net.UnknownHostException) {
                _errorMessage.postValue("No internet connection.")
                Log.e("ProductViewModel", "No Internet Exception: ${e.message}", e)
            } catch (e: Exception) {
                _errorMessage.postValue("Unexpected error: ${e.message}")
                Log.e("ProductViewModel", "Unknown Exception: ${e.message}", e)
            } finally {
                _isLoading.postValue(false)
                Log.d("ProductViewModel", "API call completed. isLoading = false")
            }
        }
    }
}
