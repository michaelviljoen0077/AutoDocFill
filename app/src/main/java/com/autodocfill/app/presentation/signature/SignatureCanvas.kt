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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Signature capture canvas composable
 */
@Composable
fun SignatureCanvas(
    onSignatureComplete: (Bitmap) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var paths by remember { mutableStateOf(listOf<PathData>()) }
    var currentPath by remember { mutableStateOf<PathData?>(null) }
    
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
                style = MaterialTheme.typography.headlineSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { paths = emptyList() }) {
                    Icon(Icons.Filled.Clear, "Clear")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = PathData(
                                path = Path().apply { moveTo(offset.x, offset.y) }
                            )
                        },
                        onDrag = { change, _ ->
                            currentPath?.let { pathData ->
                                pathData.path.lineTo(change.position.x, change.position.y)
                                currentPath = pathData.copy()
                            }
                        },
                        onDragEnd = {
                            currentPath?.let { pathData ->
                                paths = paths + pathData
                                currentPath = null
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw background
                drawRect(
                    color = Color(0xFFF5F5F5),
                    size = size
                )
                
                // Draw signature line
                drawLine(
                    color = Color.Gray,
                    start = Offset(50f, size.height - 100f),
                    end = Offset(size.width - 50f, size.height - 100f),
                    strokeWidth = 2f
                )
                
                // Draw all paths
                paths.forEach { pathData ->
                    drawPath(
                        path = pathData.path,
                        color = Color.Black,
                        style = Stroke(
                            width = 5f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                
                // Draw current path
                currentPath?.let { pathData ->
                    drawPath(
                        path = pathData.path,
                        color = Color.Black,
                        style = Stroke(
                            width = 5f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
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
                onClick = {
                    // Convert canvas to bitmap
                    // In production, capture the actual canvas bitmap
                    // For now, just call the callback
                    // onSignatureComplete(bitmap)
                },
                modifier = Modifier.weight(1f),
                enabled = paths.isNotEmpty()
            ) {
                Icon(Icons.Filled.Done, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save")
            }
        }
    }
}

/**
 * Path data for signature drawing
 */
data class PathData(
    val path: Path,
    val color: Color = Color.Black,
    val strokeWidth: Float = 5f
)
