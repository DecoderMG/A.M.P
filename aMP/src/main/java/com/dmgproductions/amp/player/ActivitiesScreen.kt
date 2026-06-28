package com.dmgproductions.amp.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ActivitiesScreen(
    state: TrainingUiState,
    onTrain: (ActivityState) -> Unit,
    onCancel: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        com.dmgproductions.amp.ui.components.AmpScreenHeader(
            title = "Activities",
            subtitle = "Everyone moves differently. Record each motion once (about 15s) so A.M.P learns your body and adapts the music automatically.",
        )

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TrainingCard(ActivityState.WALKING, state, onTrain, onCancel)
            TrainingCard(ActivityState.RUNNING, state, onTrain, onCancel)

            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onClearAll,
                enabled = !state.isRecording && (state.walkingTrained || state.runningTrained),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Delete all trained activities")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TrainingCard(
    activity: ActivityState,
    state: TrainingUiState,
    onTrain: (ActivityState) -> Unit,
    onCancel: () -> Unit,
) {
    val isThisRecording = state.recording == activity
    val trained = state.isTrained(activity)
    val accent by animateColorAsState(activity.accent, tween(400), label = "cardAccent")

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).padding(0.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isThisRecording) {
                    val progress by animateFloatAsState(
                        targetValue = 1f - state.secondsLeft / 15f,
                        animationSpec = tween(900),
                        label = "ring",
                    )
                    CircularProgressIndicator(
                        progress = { progress },
                        color = accent,
                        trackColor = accent.copy(alpha = 0.20f),
                        modifier = Modifier.size(56.dp),
                    )
                    Text("${state.secondsLeft}", style = MaterialTheme.typography.titleMedium, color = accent)
                } else {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape)
                            .padding(0.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(activity.icon, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp))
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(activity.label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    when {
                        isThisRecording -> "Recording… keep ${activity.label.lowercase()}"
                        trained -> "Trained · ready"
                        else -> "Not trained yet"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (trained && !isThisRecording) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isThisRecording) {
                OutlinedButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            } else {
                Button(
                    onClick = { onTrain(activity) },
                    enabled = !state.isRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                ) {
                    if (trained) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Retrain", fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("Train", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
