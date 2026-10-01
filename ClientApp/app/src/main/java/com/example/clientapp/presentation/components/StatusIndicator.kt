package com.example.clientapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A coloured dot plus label, e.g. "● Connected".
 *
 * Presentational only: it receives a boolean and text, and decides nothing.
 */
@Composable
fun StatusIndicator(
    active: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.semantics { contentDescription = "Status: $label" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(active = active)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Box(active: Boolean) {
    val color = if (active) Color(0xFF2E7D32) else Color(0xFFC62828)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(12.dp)
            .background(color = color, shape = CircleShape),
    )
}
