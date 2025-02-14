package com.example.nutrilens.components


import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NutritionItem(label: String, value: String?) {
    Text("$label: ${value ?: "N/A"}", Modifier.padding(bottom = 4.dp))
}

fun categorizeIngredients(ingredients: String): Map<String, List<String>> {
    val categories = mutableMapOf(
        "Sweeteners" to listOf("sugar", "glucose", "fructose", "aspartame", "sorbitol"),
        "Preservatives" to listOf("benzoate", "sorbate", "sodium nitrite"),
        "Colorants" to listOf("caramel", "tartrazine", "annatto"),
        "Other" to listOf()
    )

    val categorized = mutableMapOf<String, MutableList<String>>()
    val ingredientList = ingredients.split(",").map { it.trim().lowercase() }

    for (ingredient in ingredientList) {
        var matched = false
        for ((category, keywords) in categories) {
            if (keywords.any { keyword -> ingredient.contains(keyword) }) {
                categorized.getOrPut(category) { mutableListOf() }.add(ingredient)
                matched = true
                break
            }
        }
        if (!matched) categorized.getOrPut("Other") { mutableListOf() }.add(ingredient)
    }

    return categorized
}

fun highlightAdditives(additives: List<String>): List<Pair<String, Color>> {
    val colorMap = mapOf(
        "E200" to Color(0xFFE57373), // Red
        "E300" to Color(0xFF81C784), // Green
        "E400" to Color(0xFF64B5F6), // Blue
        "E500" to Color(0xFFFFD54F)  // Yellow
    )
    return additives.map { additive ->
        val color = colorMap[additive] ?: Color.Gray
        Pair(additive, color)
    }
}
