package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.ui.components.CanvasMapView
import com.intellinav.ui.components.NavigationBadge
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.NavigationViewModel

@Composable
fun ReplayScreen(
    viewModel: NavigationViewModel,
    onBack: () -> Unit
) {
    val pos by viewModel.positionEstimate.collectAsState()
    val mode by viewModel.navigationMode.collectAsState()
    val mapMatch by viewModel.mapMatchState.collectAsState()
    val isPlaying by viewModel.repository.replayEngine.isPlayingState.collectAsState()
    val progress by viewModel.repository.replayEngine.progress.collectAsState()
    val currentFrame by viewModel.repository.replayEngine.currentFrame.collectAsState()

    DisposableEffect(Unit) {
        viewModel.setReplayMode(true)
        onDispose {
            viewModel.setReplayMode(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceNavy)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = "IO-VNBD Dataset Replay Engine", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Ground vehicle positioning benchmark playback", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        NavigationBadge(mode = mode, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(12.dp))

        CanvasMapView(
            position = pos,
            mapMatchState = mapMatch,
            navigationMode = mode,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Slider
        Slider(
            value = progress,
            onValueChange = { viewModel.seekReplay(it) },
            colors = SliderDefaults.colors(thumbColor = PurpleAccent, activeTrackColor = PurpleAccent),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = "T: ${currentFrame?.timestampMs ?: 0} ms", color = TextMuted, fontSize = 11.sp)
            Text(text = "Track: IO-VNBD-01.csv", color = PurpleAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Playback Controls
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { viewModel.stopReplay() }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Stop", tint = CoralError)
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = { if (isPlaying) viewModel.pauseReplay() else viewModel.playReplay() },
                modifier = Modifier
                    .size(52.dp)
                    .background(PurpleAccent, shape = RoundedCornerShape(26.dp))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = DeepSpaceNavy,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { viewModel.seekReplay(0f) }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restart", tint = ElectricCyan)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SpeedMultiplierButton("0.5x", 0.5f, viewModel)
            SpeedMultiplierButton("1.0x", 1.0f, viewModel)
            SpeedMultiplierButton("2.0x", 2.0f, viewModel)
            SpeedMultiplierButton("5.0x", 5.0f, viewModel)
        }
    }
}

@Composable
private fun SpeedMultiplierButton(label: String, speed: Float, viewModel: NavigationViewModel) {
    OutlinedButton(
        onClick = { viewModel.setReplaySpeed(speed) },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(text = label, color = TextPrimary, fontSize = 11.sp)
    }
}
