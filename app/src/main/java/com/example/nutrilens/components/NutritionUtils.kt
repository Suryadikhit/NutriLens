package com.example.nutrilens.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp

@Composable
fun NutritionItem(label: String, value: String?) {
    Text(
        text = "$label: ${value ?: "N/A"}",
        modifier = Modifier.padding(bottom = 4.dp),
        color = if (value == null) Color.Gray else Color.Unspecified,
        fontStyle = if (value == null) FontStyle.Italic else FontStyle.Normal
    )
}

fun highlightAdditives(additives: List<String>): List<Pair<String, Color>> {
    val colorMap = mapOf(
        "E200" to Color(0xFFE57373), // Red
        "E202" to Color(0xFFE57373), // Red
        "E300" to Color(0xFF81C784), // Green
        "E330" to Color(0xFFFFD54F), // Yellow
        "E400" to Color(0xFF64B5F6), // Blue
        "E500" to Color(0xFFFFD54F), // Yellow
        "E102" to Color(0xFFE0E575), // Light Yellow
        "E220" to Color(0xFFEF9A9A)  // Light Red
    )
    return additives.map { additive ->
        val color = colorMap.entries.firstOrNull { additive.contains(it.key, ignoreCase = true) }?.value ?: Color.Gray
        Pair(additive, color)
    }
}


