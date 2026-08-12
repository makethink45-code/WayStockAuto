package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.BucketItem
import com.example.data.local.entities.UnitEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketBottomSheet(
    bucketItems: List<BucketItem>,
    allUnits: List<UnitEntity>,
    onUpdateQuantity: (Long, Int) -> Unit,
    onUpdateUnit: (Long, String) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onClearBucket: () -> Unit,
    onGenerateOrderSlip: () -> Unit,
    onAddCustomUnit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Bucket",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "My Bucket",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${bucketItems.size} items selected",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (bucketItems.isNotEmpty()) {
                    TextButton(onClick = onClearBucket) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (bucketItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.RemoveShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your Bucket is Empty",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(bucketItems, key = { it.id }) { item ->
                        BucketRowCard(
                            item = item,
                            allUnits = allUnits,
                            onUpdateQuantity = { newQty -> onUpdateQuantity(item.id, newQty) },
                            onUpdateUnit = { newUnit -> onUpdateUnit(item.id, newUnit) },
                            onRemoveItem = { onRemoveItem(item.id) },
                            onAddCustomUnit = onAddCustomUnit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Order Slip Action Button
                Button(
                    onClick = onGenerateOrderSlip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_order_slip_btn"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Slip",
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate Order Slip Image",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun BucketRowCard(
    item: BucketItem,
    allUnits: List<UnitEntity>,
    onUpdateQuantity: (Int) -> Unit,
    onUpdateUnit: (String) -> Unit,
    onRemoveItem: () -> Unit,
    onAddCustomUnit: (String) -> Unit
) {
    var isUnitDropdownExpanded by remember { mutableStateOf(false) }
    var customUnitText by remember { mutableStateOf("") }
    var showAddUnitDialog by remember { mutableStateOf(false) }

    // Draggable gesture state for left/right swipe quantity adjustment
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        dragAccumulator += delta
        if (dragAccumulator > 40f) {
            onUpdateQuantity(item.quantity + 1)
            dragAccumulator = 0f
        } else if (dragAccumulator < -40f) {
            if (item.quantity > 1) {
                onUpdateQuantity(item.quantity - 1)
            }
            dragAccumulator = 0f
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Name and Category Path
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.itemName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.folderPath,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(
                    onClick = onRemoveItem,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Row: Gesture Swipe Quantity Box & Unit Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gesture Swipe Control Box
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (item.quantity > 1) onUpdateQuantity(item.quantity - 1) else onRemoveItem() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Text(
                        text = String.format("%02d", item.quantity),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { onUpdateQuantity(item.quantity + 1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Dynamic Units Dropdown
                Box {
                    OutlinedButton(
                        onClick = { isUnitDropdownExpanded = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.unit,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isUnitDropdownExpanded,
                        onDismissRequest = { isUnitDropdownExpanded = false }
                    ) {
                        allUnits.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u.unitName) },
                                onClick = {
                                    onUpdateUnit(u.unitName)
                                    isUnitDropdownExpanded = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+ New Unit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                isUnitDropdownExpanded = false
                                showAddUnitDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddUnitDialog) {
        AlertDialog(
            onDismissRequest = { showAddUnitDialog = false },
            title = { Text("Add Custom Unit") },
            text = {
                OutlinedTextField(
                    value = customUnitText,
                    onValueChange = { customUnitText = it },
                    label = { Text("Unit Name (e.g. Roll, Dozen)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (customUnitText.isNotBlank()) {
                        onAddCustomUnit(customUnitText.trim())
                        onUpdateUnit(customUnitText.trim())
                        showAddUnitDialog = false
                    }
                }) {
                    Text("Add & Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUnitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
