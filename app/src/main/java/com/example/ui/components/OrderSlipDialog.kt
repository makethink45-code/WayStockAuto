package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.local.entities.BucketItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrderSlipDialog(
    bucketItems: List<BucketItem>,
    onFlushBucket: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(bucketItems) {
        if (bucketItems.isNotEmpty()) {
            generatedBitmap = generateOrderSlipCanvas(context, bucketItems)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Order Slip Preview",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Grouped by Category • High-Res Canvas",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Canvas Image Zoom Slider Preview
                val pagerState = rememberPagerState(pageCount = { 3 })
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (generatedBitmap != null) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = generatedBitmap!!.asImageBitmap(),
                                    contentDescription = "Order Slip Page ${page + 1}",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Page Indicator
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(3) { iteration ->
                                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(8.dp)
                                )
                            }
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: WhatsApp Share & PNG Download
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // WhatsApp Direct Share
                    Button(
                        onClick = {
                            generatedBitmap?.let { bmp ->
                                shareToWhatsApp(context, bmp)
                                onFlushBucket()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_whatsapp_btn"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share WhatsApp", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Direct PNG Download
                    OutlinedButton(
                        onClick = {
                            generatedBitmap?.let { bmp ->
                                savePngToStorage(context, bmp)
                                onFlushBucket()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("download_png_btn"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PNG", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Generate high-resolution order slip bitmap using Android Canvas
private fun generateOrderSlipCanvas(context: Context, items: List<BucketItem>): Bitmap {
    val width = 800
    val grouped = items.groupBy { it.rootFolderName }
    
    // Calculate required height based on items count and groups
    var estimatedHeight = 220 + (grouped.size * 50) + (items.size * 40) + 120
    if (estimatedHeight < 600) estimatedHeight = 600

    val bitmap = Bitmap.createBitmap(width, estimatedHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background
    canvas.drawColor(Color.WHITE)

    val paintHeader = Paint().apply {
        color = Color.parseColor("#1E293B")
        textSize = 34f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val paintSubHeader = Paint().apply {
        color = Color.parseColor("#64748B")
        textSize = 20f
        isAntiAlias = true
    }

    val paintGroupTitle = Paint().apply {
        color = Color.parseColor("#1E40AF")
        textSize = 24f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val paintItemText = Paint().apply {
        color = Color.parseColor("#0F172A")
        textSize = 22f
        isAntiAlias = true
    }

    val paintLine = Paint().apply {
        color = Color.parseColor("#E2E8F0")
        strokeWidth = 3f
    }

    var y = 60f

    // Header Title
    canvas.drawText("WAYSTOCK MASTER - ORDER SLIP", 40f, y, paintHeader)
    y += 35f

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = "Date: ${dateFormat.format(Date())} • Total Items: ${items.sumOf { it.quantity }}"
    canvas.drawText(dateStr, 40f, y, paintSubHeader)
    y += 30f

    canvas.drawLine(40f, y, width - 40f, y, paintLine)
    y += 40f

    // Render Grouped Items
    for ((group, itemList) in grouped) {
        canvas.drawText("📁 Category: $group", 40f, y, paintGroupTitle)
        y += 35f

        for (item in itemList) {
            val itemLine = "  • ${item.itemName}  ---  ${item.quantity} ${item.unit}"
            canvas.drawText(itemLine, 50f, y, paintItemText)
            y += 38f
        }
        y += 20f
    }

    y += 20f
    canvas.drawLine(40f, y, width - 40f, y, paintLine)
    y += 40f

    val paintFooter = Paint().apply {
        color = Color.parseColor("#94A3B8")
        textSize = 18f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    }
    canvas.drawText("Generated via WayStock Master Cloud & Offline Sync", 40f, y, paintFooter)

    return bitmap
}

// Share Canvas Image directly to WhatsApp / System Share
private fun shareToWhatsApp(context: Context, bitmap: Bitmap) {
    try {
        val file = saveBitmapToCache(context, bitmap)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "WayStock Master Order Slip")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Order Slip"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing slip: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

// Save PNG file with timestamped name (WayStock_SSMMHHDDMMYY.png)
private fun savePngToStorage(context: Context, bitmap: Bitmap) {
    try {
        val sdf = SimpleDateFormat("ssmmHHddMMyy", Locale.getDefault())
        val fileName = "WayStock_${sdf.format(Date())}.png"
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val imageFile = File(cacheDir, fileName)

        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        Toast.makeText(context, "Order Slip Saved as $fileName", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to save PNG: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun saveBitmapToCache(context: Context, bitmap: Bitmap): File {
    val imagesFolder = File(context.cacheDir, "images")
    imagesFolder.mkdirs()
    val file = File(imagesFolder, "order_slip_latest.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return file
}
