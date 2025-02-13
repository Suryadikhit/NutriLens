package com.example.nutrilens.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrilens.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class SearchViewModel : ViewModel() {
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _products = MutableLiveData<List<Product>>(emptyList())
    val products: LiveData<List<Product>> = _products

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchProducts(query: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getProductsByName(query)
                _products.value = response
            } catch (e: HttpException) {
                _errorMessage.value = "Server error: ${e.message}"
            } catch (e: IOException) {
                _errorMessage.value = "Network error. Please check your connection."
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error occurred: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
