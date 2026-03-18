package com.kumanodormitory.pokke.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumanodormitory.pokke.ui.component.ThreeColumnSelector
import com.kumanodormitory.pokke.ui.util.SoundManager
import com.kumanodormitory.pokke.ui.util.debounceClickable
import com.kumanodormitory.pokke.ui.viewmodel.CallViewModel

// 呼び出し画面の色定義
private val CallHeaderColor = Color(0xFF60DEA0)    // jimuto_theme と同系
private val HeaderFontColor = Color.White
private val FooterColor = Color(0xFF333C5E)        // default_theme
private val FooterFontColor = Color(0xFFA9A9A9)    // default_footer_font

@Composable
fun CallScreen(
    viewModel: CallViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar表示
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ===== ヘッダー =====
            CallHeader(onNavigateBack = onNavigateBack)

            // ===== コンテンツ: 3カラムセレクター =====
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 60.dp, vertical = 20.dp)
            ) {
                ThreeColumnSelector(
                    blocks = uiState.blocks,
                    rooms = uiState.rooms,
                    ryoseiList = uiState.ryoseiList,
                    onRyoseiSelected = { ryosei ->
                        viewModel.selectRyosei(ryosei)
                    },
                    selectedBlock = uiState.selectedBlock,
                    onBlockSelected = { block ->
                        viewModel.selectBlock(block)
                    },
                    selectedRoom = uiState.selectedRoom,
                    onRoomSelected = { room ->
                        viewModel.selectRoom(room)
                    },
                    showSearch = true,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    isRyoseiEnabled = { ryosei ->
                        ryosei.discordStatus == "LINKED"
                    },
                    onBlockClick = { SoundManager.playCursorBlock(context) },
                    onRoomClick = { SoundManager.playCursorRoom(context) },
                    onRyoseiClick = { SoundManager.playCursorRyosei(context) },
                    onSearchSubmit = { SoundManager.playSearch(context) }
                )

                // ローディング表示
                if (uiState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // ===== フッター =====
            CallFooter()
        }
    }

    // ===== 呼び出し種別ダイアログ =====
    if (uiState.showCallTypeDialog && uiState.selectedRyosei != null) {
        CallTypeDialog(
            ryoseiName = "${uiState.selectedRyosei!!.room}  ${uiState.selectedRyosei!!.name}",
            isSending = uiState.isSending,
            onSend = { callType ->
                viewModel.sendCall(callType)
                SoundManager.playDone(context)
            },
            onDismiss = { viewModel.dismissCallTypeDialog() }
        )
    }
}

// ===== ヘッダー =====
@Composable
private fun CallHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(CallHeaderColor)
            .padding(horizontal = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 戻るボタン
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.3f))
                .debounceClickable(1000L) { onNavigateBack() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "←",
                fontSize = 24.sp,
                color = HeaderFontColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "寮生の呼び出し",
            color = HeaderFontColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        // 右側のバランス用スペーサー
        Spacer(modifier = Modifier.size(50.dp))
    }
}

// ===== フッター =====
@Composable
private fun CallFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .background(FooterColor)
            .padding(horizontal = 50.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Copyright \u00A9 2026 Kumano Dormitory IT Section",
            color = FooterFontColor,
            fontSize = 14.sp
        )
    }
}

// ===== 呼び出し種別ダイアログ =====
@Composable
private fun CallTypeDialog(
    ryoseiName: String,
    isSending: Boolean,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = {
            Text(
                text = "$ryoseiName\nを呼び出します",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "呼び出し種別を選択してください：",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(4.dp))

                CallTypeOption(
                    label = "来客",
                    icon = Icons.Default.Person,
                    isSelected = selectedType == "visitor",
                    enabled = !isSending,
                    onClick = { selectedType = "visitor" }
                )
                CallTypeOption(
                    label = "電話",
                    icon = Icons.Default.Phone,
                    isSelected = selectedType == "phone",
                    enabled = !isSending,
                    onClick = { selectedType = "phone" }
                )
                CallTypeOption(
                    label = "書留郵便",
                    icon = Icons.Default.Email,
                    isSelected = selectedType == "registered_mail",
                    enabled = !isSending,
                    onClick = { selectedType = "registered_mail" }
                )
            }
        },
        confirmButton = {
            val canSend = selectedType != null && !isSending
            Box(
                modifier = Modifier
                    .then(
                        if (canSend) {
                            Modifier.debounceClickable(1000L) { onSend(selectedType!!) }
                        } else {
                            Modifier
                        }
                    )
            ) {
                TextButton(
                    onClick = {},
                    enabled = canSend
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "送信",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canSend) CallHeaderColor else Color.Gray
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSending
            ) {
                Text("キャンセル", fontSize = 18.sp)
            }
        }
    )
}

// ===== 呼び出し種別オプション =====
@Composable
private fun CallTypeOption(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> CallHeaderColor.copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isSelected -> CallHeaderColor
        else -> Color(0xFFDDDDDD)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(
                if (enabled) {
                    Modifier.debounceClickable(400L) { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) CallHeaderColor else Color.Gray,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (enabled) Color.Black else Color.Gray
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isSelected) {
            Text(
                text = "✓",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CallHeaderColor
            )
        }
    }
}
