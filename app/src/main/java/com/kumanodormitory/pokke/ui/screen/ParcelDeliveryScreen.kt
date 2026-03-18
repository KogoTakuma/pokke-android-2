package com.kumanodormitory.pokke.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import com.kumanodormitory.pokke.ui.util.SoundManager
import com.kumanodormitory.pokke.ui.util.debounceClickableItem
import com.kumanodormitory.pokke.ui.util.formatDateTime
import com.kumanodormitory.pokke.ui.util.formatParcelType
import com.kumanodormitory.pokke.ui.viewmodel.ParcelDeliveryViewModel

// 旧POKKEの引渡テーマカラー
private val ReleaseTheme = Color(0xFFDAA186)
private val ReleaseHeaderFont = Color.White
private val ListBorder = Color(0xFFD3D3D3)

@Composable
fun ParcelDeliveryScreen(
    viewModel: ParcelDeliveryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header — 旧UIの include_header_release を再現
        ReleaseHeader(
            dutyPersonName = uiState.dutyPersonName,
            onBack = onNavigateBack
        )

        // Content — 3カラムレイアウト
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 110.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Column 1: Blocks (旧UI: 160dp幅)
            BlockColumn(
                blocks = uiState.blocks,
                selectedBlock = uiState.selectedBlock,
                onSelectBlock = { block ->
                    viewModel.selectBlock(block)
                    SoundManager.playCursorBlock(context)
                },
                modifier = Modifier.width(160.dp).fillMaxHeight()
            )

            Spacer(modifier = Modifier.width(100.dp))

            // Column 2: Rooms (旧UI: 160dp幅)
            RoomColumn(
                rooms = uiState.rooms,
                selectedRoom = uiState.selectedRoom,
                onSelectRoom = { room ->
                    viewModel.selectRoom(room)
                    SoundManager.playCursorRoom(context)
                },
                modifier = Modifier.width(160.dp).fillMaxHeight()
            )

            Spacer(modifier = Modifier.width(100.dp))

            // Column 3: Ryosei (旧UI: 540dp幅、検索付き)
            RyoseiColumnWithSearch(
                ryoseiList = uiState.ryoseiWithParcels,
                onSelectRyosei = { ryosei ->
                    viewModel.selectRyosei(ryosei)
                    SoundManager.playCursorRyosei(context)
                },
                modifier = Modifier.width(540.dp).fillMaxHeight()
            )
        }

        // Footer — 旧UIの include_footer_release を再現
        ReleaseFooter()
    }

    // 本人確認ダイアログ
    if (uiState.showIdentityDialog) {
        var identityChecked by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissIdentityDialog() },
            title = {
                Text(
                    text = "本人確認",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "荷物を引き渡す前に本人確認を行ってください。",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { identityChecked = !identityChecked }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = identityChecked,
                            onCheckedChange = { identityChecked = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "身分証明書、学生証、橙食券を確認した。",
                            fontSize = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmIdentity()
                    },
                    enabled = identityChecked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReleaseTheme
                    )
                ) {
                    Text("次へ", fontSize = 18.sp, color = ReleaseHeaderFont)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissIdentityDialog() }) {
                    Text("キャンセル", fontSize = 18.sp)
                }
            }
        )
    }

    // 引渡ダイアログ
    if (uiState.showDeliveryDialog) {
        DeliveryDialog(
            ryosei = uiState.selectedRyosei,
            parcels = uiState.parcelsForRyosei,
            selectedIds = uiState.selectedParcelIds,
            isDelivering = uiState.isDelivering,
            onToggle = viewModel::toggleParcelSelection,
            onConfirm = {
                viewModel.deliverSelected {
                    SoundManager.playDone(context)
                    onNavigateBack()
                }
            },
            onDismiss = viewModel::dismissDialog
        )
    }
}

// ─── Header ───────────────────────────────────────────────

@Composable
private fun ReleaseHeader(
    dutyPersonName: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(ReleaseTheme)
            .padding(horizontal = 70.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "戻る",
                tint = ReleaseHeaderFont,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "引き渡し",
            color = ReleaseHeaderFont,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "現在の事務当：",
            color = ReleaseHeaderFont,
            fontSize = 20.sp
        )

        Text(
            text = dutyPersonName.ifEmpty { "設定されていません" },
            color = ReleaseHeaderFont,
            fontSize = 20.sp,
            modifier = Modifier.width(280.dp)
        )
    }
}

// ─── Footer ───────────────────────────────────────────────

@Composable
private fun ReleaseFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .background(ReleaseTheme)
            .padding(horizontal = 50.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "© Kumano Dormitory",
            color = ReleaseHeaderFont,
            fontSize = 12.sp
        )
    }
}

// ─── Block Column ─────────────────────────────────────────

@Composable
private fun BlockColumn(
    blocks: List<String>,
    selectedBlock: String?,
    onSelectBlock: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.border(1.dp, ListBorder)
    ) {
        items(blocks) { block ->
            val isSelected = block == selectedBlock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .debounceClickableItem(400L) { onSelectBlock(block) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = block,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider()
        }
    }
}

// ─── Room Column ──────────────────────────────────────────

@Composable
private fun RoomColumn(
    rooms: List<String>,
    selectedRoom: String?,
    onSelectRoom: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.border(1.dp, ListBorder)
    ) {
        items(rooms) { room ->
            val isSelected = room == selectedRoom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .debounceClickableItem(400L) { onSelectRoom(room) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = room,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider()
        }
    }
}

// ─── Ryosei Column with Search ────────────────────────────

@Composable
private fun RyoseiColumnWithSearch(
    ryoseiList: List<RyoseiEntity>,
    onSelectRyosei: (RyoseiEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 検索バー（旧UI: 寮生名検索）
        // 検索は ViewModel 側で実装予定。現段階ではプレースホルダー配置
        // 旧UI: EditText 225dp + ImageButton 40dp

        // 寮生リスト
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, ListBorder)
        ) {
            items(ryoseiList) { ryosei ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .debounceClickableItem(400L) { onSelectRyosei(ryosei) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // 旧UI: "部屋名 寮生名" + 荷物数を2行表示 (simple_list_item_2)
                    Column {
                        Text(
                            text = "${ryosei.room}  ${ryosei.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

// ─── Delivery Dialog ──────────────────────────────────────
// 旧UI: ReleaseDialog.java を再現
// - タイトル: "部屋 名前 の荷物を引き渡します。"
// - MultiChoiceItems（全選択デフォルト）
// - 「引き渡し」「キャンセル」ボタン

@Composable
private fun DeliveryDialog(
    ryosei: RyoseiEntity?,
    parcels: List<ParcelEntity>,
    selectedIds: Set<String>,
    isDelivering: Boolean,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDelivering) onDismiss() },
        title = {
            Text(
                text = "${ryosei?.room ?: ""} ${ryosei?.name ?: ""} の荷物を引き渡します。",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn {
                items(parcels) { parcel ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isDelivering) { onToggle(parcel.id) }
                            .padding(vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = parcel.id in selectedIds,
                            onCheckedChange = { onToggle(parcel.id) },
                            enabled = !isDelivering
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatParcelType(parcel.parcelType),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (parcel.isLost) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "紛失",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(Color.Red, shape = MaterialTheme.shapes.small)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = formatDateTime(parcel.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!parcel.registeredByName.isNullOrBlank()) {
                                Text(
                                    text = "受取事務当: ${parcel.registeredByName}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!parcel.note.isNullOrBlank()) {
                                Text(
                                    text = parcel.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            val canConfirm = selectedIds.isNotEmpty() && !isDelivering
            var lastClickTime by remember { mutableStateOf(0L) }
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime >= 1000L) {
                        lastClickTime = now
                        onConfirm()
                    }
                },
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE9EBF5) // 旧UI: data1D
                )
            ) {
                if (isDelivering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "引き渡し",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDelivering
            ) {
                Text(
                    text = "キャンセル",
                    fontSize = 20.sp
                )
            }
        }
    )
}
