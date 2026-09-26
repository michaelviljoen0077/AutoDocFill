package com.autodocfill.app.presentation.signature

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

private const val STROKE_WIDTH = 6f

/**
 * Signature capture canvas composable
 */
@Composable
fun SignatureCanvas(
    onSignatureComplete: (Bitmap) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Each stroke is the list of points from one finger-down to finger-up
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Sign Here",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black
            )
            IconButton(onClick = { strokes.clear() }) {
                Icon(Icons.Filled.Clear, "Clear", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> currentStroke = listOf(offset) },
                        onDrag = { change, _ ->
                            change.consume()
                            currentStroke = currentStroke + change.position
                        },
                        onDragEnd = {
                            if (currentStroke.isNotEmpty()) strokes.add(currentStroke)
                            currentStroke = emptyList()
                        },
                        onDragCancel = { currentStroke = emptyList() }
                    )
                }
        ) {
            drawRect(color = Color(0xFFF5F5F5), size = size)

            // Signature line
            drawLine(
                color = Color.Gray,
                start = Offset(50f, size.height - 100f),
                end = Offset(size.width - 50f, size.height - 100f),
                strokeWidth = 2f
            )

            (strokes + listOf(currentStroke)).forEach { points ->
                drawPath(
                    path = points.toPath(),
                    color = Color.Black,
                    style = Stroke(width = STROKE_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { onSignatureComplete(renderSignature(strokes, canvasSize)) },
                modifier = Modifier.weight(1f),
                enabled = strokes.isNotEmpty() && canvasSize != IntSize.Zero
            ) {
                Icon(Icons.Filled.Done, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save")
            }
        }
    }
}

private fun List<Offset>.toPath(): Path = Path().apply {
    if (isEmpty()) return@apply
    moveTo(first().x, first().y)
    drop(1).forEach { lineTo(it.x, it.y) }
}

/**
 * Draw the strokes onto a transparent bitmap cropped to the signature's bounds,
 * so it can be stamped onto a PDF without covering the page.
 */
private fun renderSignature(strokes: List<List<Offset>>, canvasSize: IntSize): Bitmap {
    val points = strokes.flatten()
    val padding = STROKE_WIDTH * 2
    val left = (points.minOf { it.x } - padding).coerceAtLeast(0f)
    val top = (points.minOf { it.y } - padding).coerceAtLeast(0f)
    val right = (points.maxOf { it.x } + padding).coerceAtMost(canvasSize.width.toFloat())
    val bottom = (points.maxOf { it.y } + padding).coerceAtMost(canvasSize.height.toFloat())

    val bitmap = Bitmap.createBitmap(
        (right - left).toInt().coerceAtLeast(1),
        (bottom - top).toInt().coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    strokes.forEach { stroke ->
        if (stroke.size == 1) {
            canvas.drawPoint(stroke[0].x - left, stroke[0].y - top, paint)
        } else {
            val path = android.graphics.Path()
            path.moveTo(stroke[0].x - left, stroke[0].y - top)
            stroke.drop(1).forEach { path.lineTo(it.x - left, it.y - top) }
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}
