package com.intellinav.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.data.model.MapMatchState
import com.intellinav.data.model.NavigationMode
import com.intellinav.data.model.PositionEstimate
import com.intellinav.data.model.SensorHardwareMode
import com.intellinav.ui.theme.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun CanvasMapView(
    position: PositionEstimate,
    mapMatchState: MapMatchState,
    navigationMode: NavigationMode,
    modifier: Modifier = Modifier,
    showDetailedTrails: Boolean = false // Default clean Home UI view
) {
    // Shortest angular rotation tracking to avoid 0/360 degree spin jumps
    var previousHeading by remember { mutableFloatStateOf(position.headingDeg) }
    var continuousHeading by remember { mutableFloatStateOf(position.headingDeg) }

    LaunchedEffect(position.headingDeg) {
        var diff = (position.headingDeg - previousHeading) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        continuousHeading += diff
        previousHeading = position.headingDeg
    }

    val animatedHeading by animateFloatAsState(
        targetValue = continuousHeading,
        animationSpec = tween(durationMillis = 200),
        label = "headingRotation"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF070C16))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val headingRad = Math.toRadians(animatedHeading.toDouble() - 90.0)

            // 1. Draw Radar / Spatial Coordinate Grid
            val gridSpacing = 50.dp.toPx()
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

            for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(x.toFloat(), 0f),
                    end = Offset(x.toFloat(), size.height),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
            }
            for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(0f, y.toFloat()),
                    end = Offset(size.width, y.toFloat()),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
            }

            // 2. Road Network Vector Casing & Centerlines
            val mainRoadPath = Path().apply {
                moveTo(center.x - size.width * 0.45f, center.y + size.height * 0.35f)
                cubicTo(
                    center.x - size.width * 0.15f, center.y + size.height * 0.15f,
                    center.x + size.width * 0.15f, center.y - size.height * 0.15f,
                    center.x + size.width * 0.45f, center.y - size.height * 0.35f
                )
            }

            val crossRoadPath = Path().apply {
                moveTo(center.x - size.width * 0.35f, center.y - size.height * 0.35f)
                lineTo(center.x + size.width * 0.35f, center.y + size.height * 0.35f)
            }

            // Draw Road Surfaces
            drawPath(path = mainRoadPath, color = Color(0xFF111C2E), style = Stroke(width = 36.dp.toPx()))
            drawPath(path = crossRoadPath, color = Color(0xFF0F172A), style = Stroke(width = 24.dp.toPx()))

            // Draw Road Borders & Centerlines
            drawPath(path = mainRoadPath, color = Color(0xFF334155), style = Stroke(width = 38.dp.toPx(), pathEffect = dashEffect))
            drawPath(path = mainRoadPath, color = Color(0xFF475569), style = Stroke(width = 3.dp.toPx(), pathEffect = dashEffect))

            // 3. Trajectory Visualizations
            if (showDetailedTrails) {
                // GNSS Reference Trail
                val gnssTrail = Path().apply {
                    moveTo(center.x - 120f, center.y + 100f)
                    lineTo(center.x - 40f, center.y + 35f)
                    lineTo(center.x, center.y)
                }
                drawPath(
                    path = gnssTrail,
                    color = MintGreen.copy(alpha = if (navigationMode == NavigationMode.GNSS_AVAILABLE) 0.8f else 0.3f),
                    style = Stroke(width = 4.dp.toPx())
                )

                // DR Trajectory Drift Propagation (during outage)
                if (navigationMode == NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE) {
                    val drDriftOffset = (position.accumulatedDriftMeters * 6.dp.toPx()).coerceAtMost(60f)
                    val drTrail = Path().apply {
                        moveTo(center.x, center.y)
                        lineTo(
                            center.x + (60f * cos(headingRad)).toFloat() + drDriftOffset,
                            center.y + (60f * sin(headingRad)).toFloat() - drDriftOffset
                        )
                    }
                    drawPath(
                        path = drTrail,
                        color = AmberWarning,
                        style = Stroke(width = 4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
                    )
                }
            }

            // 4. Eigen-based 95% Position Covariance Ellipse
            // P_pos = [ Pxx  Pxy ]
            //         [ Pxy  Pyy ]
            val pxx = position.covPxX
            val pxy = position.covPxY
            val pyy = position.covPyY

            val trace = pxx + pyy
            val det = pxx * pyy - pxy * pxy
            val disc = sqrt((trace * trace / 4f - det).coerceAtLeast(0f))

            val lambda1 = (trace / 2f + disc).coerceAtLeast(0.1f)
            val lambda2 = (trace / 2f - disc).coerceAtLeast(0.1f)

            // Chi-square factor 5.991 for 95% 2D confidence
            val semiMajorMeters = sqrt(5.991f * lambda1)
            val semiMinorMeters = sqrt(5.991f * lambda2)

            val pxPerMeter = 6.dp.toPx()
            val aPx = (semiMajorMeters * pxPerMeter).coerceIn(16.dp.toPx(), size.width / 2.5f)
            val bPx = (semiMinorMeters * pxPerMeter).coerceIn(12.dp.toPx(), size.width / 3.0f)

            val phiRad = 0.5f * atan2(2f * pxy, (pxx - pyy))
            val phiDeg = Math.toDegrees(phiRad.toDouble()).toFloat()

            val uncertaintyColor = when (navigationMode) {
                NavigationMode.GNSS_AVAILABLE -> MintGreen.copy(alpha = 0.15f)
                NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE -> AmberWarning.copy(alpha = 0.22f)
                NavigationMode.GNSS_RESTORED_FUSION_RECOVERING -> ElectricCyan.copy(alpha = 0.20f)
                else -> Color.White.copy(alpha = 0.10f)
            }

            // Draw rotated 95% confidence covariance ellipse
            rotate(degrees = phiDeg, pivot = center) {
                drawOval(
                    color = uncertaintyColor,
                    topLeft = Offset(center.x - aPx, center.y - bPx),
                    size = Size(aPx * 2f, bPx * 2f)
                )
                drawOval(
                    color = uncertaintyColor.copy(alpha = 0.8f),
                    topLeft = Offset(center.x - aPx, center.y - bPx),
                    size = Size(aPx * 2f, bPx * 2f),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = dashEffect)
                )
            }

            // Forward Velocity Projection Ray
            val projectedRayLength = 40.dp.toPx()
            drawLine(
                color = ElectricCyan.copy(alpha = 0.6f),
                start = center,
                end = Offset(
                    center.x + (projectedRayLength * cos(headingRad)).toFloat(),
                    center.y + (projectedRayLength * sin(headingRad)).toFloat()
                ),
                strokeWidth = 2.dp.toPx(),
                pathEffect = dashEffect
            )

            // 5. Vehicle Marker
            val markerColor = when (navigationMode) {
                NavigationMode.GNSS_AVAILABLE -> MintGreen
                NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE -> AmberWarning
                NavigationMode.GNSS_RESTORED_FUSION_RECOVERING -> ElectricCyan
                else -> ElectricCyan
            }

            rotate(degrees = animatedHeading, pivot = center) {
                val arrowHeight = 22.dp.toPx()
                val arrowWidth = 14.dp.toPx()

                val vehiclePath = Path().apply {
                    moveTo(center.x, center.y - arrowHeight)
                    lineTo(center.x + arrowWidth, center.y + arrowHeight * 0.6f)
                    lineTo(center.x, center.y + arrowHeight * 0.2f)
                    lineTo(center.x - arrowWidth, center.y + arrowHeight * 0.6f)
                    close()
                }

                drawPath(path = vehiclePath, color = markerColor.copy(alpha = 0.4f), style = Stroke(width = 6.dp.toPx()))
                drawPath(path = vehiclePath, color = markerColor)
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = center)
            }
        }

        // Overlay 1: Mode Badge (Top Left)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xEE0F172A), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (navigationMode) {
                                    NavigationMode.GNSS_AVAILABLE -> MintGreen
                                    NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE -> AmberWarning
                                    NavigationMode.GNSS_RESTORED_FUSION_RECOVERING -> ElectricCyan
                                    else -> CoralError
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = position.sensorHardwareMode.label,
                            color = Color(position.sensorHardwareMode.badgeColorHex),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (mapMatchState.isNhcActive) "CENTERLINE MATCHED • NHC ACTIVE" else "INS PROPAGATION",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Overlay 2: Trajectory Legend (Engineering View Only)
        if (showDetailedTrails) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(Color(0xEE0F172A), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(text = "TRAJECTORY LEGEND", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp, 3.dp).background(MintGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "GNSS Fix", color = TextPrimary, fontSize = 9.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp, 3.dp).background(AmberWarning))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Raw DR Path", color = TextPrimary, fontSize = 9.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp, 3.dp).background(ElectricCyan))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Map-Matched DR", color = TextPrimary, fontSize = 9.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp, 6.dp).background(AmberWarning.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "95% ESKF Covariance Ellipse", color = TextPrimary, fontSize = 9.sp)
                }
            }
        }

        // Overlay 3: Compass Rose (Top Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(36.dp)
                .background(Color(0xEE0F172A), CircleShape)
                .border(1.dp, Color(0xFF334155), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "N", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
