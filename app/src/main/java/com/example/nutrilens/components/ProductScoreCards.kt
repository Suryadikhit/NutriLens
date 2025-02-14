package com.example.nutrilens.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProductScoreCards(nutriScore: String?, novaScore: String?) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()

    ) {
        nutriScore?.let {
            ScoreCard(
                title = "NUTRI-SCORE",
                score = it,
                colors = listOf(
                    Color(0xFF00C853), // A
                    Color(0xFFAEEA00), // B
                    Color(0xFFFFD600), // C
                    Color(0xFFFF6D00), // D
                    Color(0xFFD50000)  // E
                )
            )
        }
        novaScore?.let {
            ScoreCard(
                title = "NOVA",
                score = it,
                colors = listOf(Color(0xFFFF9800)) // Orange for Nova
            )
        }
    }
}

@Composable
fun ScoreCard(title: String, score: String, colors: List<Color>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .width(160.dp)
            .padding(3.dp)
            .height(100.dp) // Ensure uniform height
    ) {
        Column(
            modifier = Modifier
                .background(Color.DarkGray)
                .fillMaxSize()  // Fill the card
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            if (title == "NUTRI-SCORE") {
                NutriScoreBar(score, colors)
            } else {
                Text(
                    text = score,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.first(),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun NutriScoreBar(score: String, colors: List<Color>) {
    val selectedIndex = when (score.uppercase()) {
        "A" -> 0
        "B" -> 1
        "C" -> 2
        "D" -> 3
        "E" -> 4
        else -> 0
    }

    Row(modifier = Modifier.fillMaxWidth().height(40.dp)) {  // Consistent height
        listOf("A", "B", "C", "D", "E").forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (index == selectedIndex) colors[index] else Color.Gray)
                    .fillMaxHeight()  // Fill height of the row
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = label, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
