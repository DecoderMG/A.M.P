package com.dmgproductions.amp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dmgproductions.amp.ui.components.AmpScreenHeader

private data class HelpItem(val icon: ImageVector, val title: String, val body: String)

private val helpItems = listOf(
    HelpItem(
        Icons.Rounded.GraphicEq,
        "How A.M.P chooses music",
        "A.M.P keeps separate mixes for resting, walking, and running. As your motion changes it crossfades between them, so the energy of the music always matches the energy of your body.",
    ),
    HelpItem(
        Icons.Rounded.DirectionsRun,
        "Training your activities",
        "Open Activities and record each motion once for about 15 seconds. A.M.P learns the signature of your walk and run so detection stays accurate. You can retrain anytime your stride changes.",
    ),
    HelpItem(
        Icons.Rounded.Security,
        "Permissions",
        "Motion sensing and audio run entirely on-device. The microphone and location permissions power activity detection and the visualizer — nothing leaves your phone.",
    ),
    HelpItem(
        Icons.Rounded.SystemUpdate,
        "Installing updates",
        "Builds are distributed as sideloadable APKs. Enable \"Install unknown apps\" for your browser or file manager, then open the downloaded .apk to update.",
    ),
)

@Composable
fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        AmpScreenHeader(title = "Help", subtitle = "Get the most out of activity-aware playback.", onBack = onBack)
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            helpItems.forEach { HelpCard(it) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HelpCard(item: HelpItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Column {
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(item.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
