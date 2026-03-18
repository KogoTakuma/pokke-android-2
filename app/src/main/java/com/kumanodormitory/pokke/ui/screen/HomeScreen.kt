package com.kumanodormitory.pokke.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumanodormitory.pokke.R
import com.kumanodormitory.pokke.data.local.entity.OperationLogEntity
import com.kumanodormitory.pokke.data.local.entity.OperationLogWithParcel
import com.kumanodormitory.pokke.ui.util.SoundManager
import com.kumanodormitory.pokke.ui.util.debounceClickable
import com.kumanodormitory.pokke.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 旧アプリの色定義を再現
private val HeaderColor = Color(0xFF333C5E)
private val HeaderFontColor = Color.White
private val OldNoteColor = Color(0xFFEEEEEE)          // verylightgray
private val OthersColor = Color(0xFFD3D3D3)            // lightgray
private val CallColor = Color(0xFF60DEA0)              // jimuto_theme (green)
private val LogBackgroundColor = Color(0xFFF1F1F1)
private val FooterColor = Color(0xFF333C5E)
private val FooterFontColor = Color(0xFFA9A9A9)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val cancelTarget by viewModel.showCancelDialog.collectAsState()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        // ===== ヘッダー（旧: include_header_main） =====
        HeaderBar(
            dutyPersonName = uiState.currentDutyPersonName,
            onDutyChange = {
                SoundManager.playTransition(context)
                onNavigate("duty_change")
            }
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // ===== コンテンツ（横レイアウト: 左=ボタン群、右=履歴） =====
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 20.dp)
            ) {
                // 左側: ボタン群
                LeftButtonPanel(
                    onNavigate = onNavigate,
                    context = context,
                    isDutyPersonSet = uiState.currentDutyPersonName.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 50.dp)
                )

                // 右側: 履歴
                RightLogPanel(
                    logsWithParcel = uiState.recentLogs,
                    isCancellable = { viewModel.isCancellable(it) },
                    onCancelRequest = { viewModel.requestCancel(it) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 30.dp)
                )
            }
        }

        // ===== フッター（旧: include_footer） =====
        FooterBar()
    }

    // 取消確認ダイアログ
    cancelTarget?.let { log ->
        CancelConfirmDialog(
            log = log,
            onConfirm = {
                SoundManager.playDone(context)
                viewModel.confirmCancel()
            },
            onDismiss = { viewModel.dismissCancelDialog() }
        )
    }

    // エラーダイアログ
    uiState.error?.let { errorMessage ->
        SoundManager.playError(context)
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("エラー") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

// ===== ヘッダー =====
@Composable
private fun HeaderBar(
    dutyPersonName: String,
    onDutyChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(HeaderColor)
            .padding(horizontal = 70.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // アプリタイトル
        Text(
            text = "荷物管理アプリ",
            color = HeaderFontColor,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(16.dp))

        // POKKEロゴ（画像）
        Image(
            painter = painterResource(id = R.drawable.pokke_clear),
            contentDescription = "POKKE",
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(90.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // 現在の事務当
        Text(
            text = "現在の事務当：",
            color = HeaderFontColor,
            fontSize = 20.sp
        )

        Text(
            text = dutyPersonName.ifEmpty { "設定されていません" },
            color = HeaderFontColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(280.dp)
        )

        // 事務当交代ボタン（旧: change_jimuto.png）
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .debounceClickable(1000L) { onDutyChange() }
        ) {
            Image(
                painter = painterResource(id = R.drawable.change_jimuto),
                contentDescription = "事務当交代",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ===== 左側ボタン群 =====
@Composable
private fun LeftButtonPanel(
    onNavigate: (String) -> Unit,
    context: android.content.Context,
    isDutyPersonSet: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // 荷物の受け取り（大ボタン）
        ImageActionButton(
            drawableRes = R.drawable.register_v2,
            contentDescription = "荷物の受け取り",
            height = 166,
            enabled = isDutyPersonSet,
            onClick = {
                SoundManager.playTransition(context)
                onNavigate("parcel_register")
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 荷物の引き渡し（大ボタン）
        ImageActionButton(
            drawableRes = R.drawable.release_v2,
            contentDescription = "荷物の引き渡し",
            height = 170,
            enabled = isDutyPersonSet,
            onClick = {
                SoundManager.playTransition(context)
                onNavigate("parcel_delivery")
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 中段: 寮生の呼び出し + 泊まり事務当番
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 寮生の呼び出しボタン（アイコン付き）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CallColor)
                    .debounceClickable(1000L) {
                        SoundManager.playTransition(context)
                        onNavigate("call")
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.yobidashi_icon),
                        contentDescription = "呼び出し",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "寮生の呼び出し",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (isDutyPersonSet) {
                            Modifier.debounceClickable(1000L) {
                                SoundManager.playTransition(context)
                                onNavigate("night_duty")
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tomari_jimuto_v2),
                    contentDescription = "泊まり事務当番",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (!isDutyPersonSet) Modifier.alpha(0.4f) else Modifier)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 下段: 旧型ノート + 管理用画面
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OldNoteColor)
                    .debounceClickable(1000L) {
                        SoundManager.playTransition(context)
                        onNavigate("old_notebook")
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.icon_calender),
                        contentDescription = "カレンダー",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "荷物履歴一覧",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            SubActionButton(
                text = "管理用画面",
                color = OthersColor,
                textColor = Color.Black,
                modifier = Modifier
                    .weight(1f)
                    .height(95.dp),
                onClick = {
                    SoundManager.playTransition(context)
                    onNavigate("admin")
                }
            )
        }
    }
}

// ===== 画像ボタン =====
@Composable
private fun ImageActionButton(
    drawableRes: Int,
    contentDescription: String,
    height: Int,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (enabled) {
                    Modifier.debounceClickable(1000L) { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .then(if (!enabled) Modifier.alpha(0.4f) else Modifier)
        )
    }
}

// ===== 小さなサブボタン =====
@Composable
private fun SubActionButton(
    text: String,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) color else color.copy(alpha = 0.4f))
            .then(
                if (enabled) {
                    Modifier.debounceClickable(1000L) { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// ===== 右側ログパネル =====
@Composable
private fun RightLogPanel(
    logsWithParcel: List<OperationLogWithParcel>,
    isCancellable: (OperationLogWithParcel) -> Boolean,
    onCancelRequest: (OperationLogWithParcel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // ヘッダー行: "履歴" + リフレッシュアイコン
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "履歴",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            // リフレッシュはFlowで自動更新されるため装飾のみ
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "更新",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ログリスト（旧: ListView, 背景 #F1F1F1）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .background(LogBackgroundColor)
        ) {
            if (logsWithParcel.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "操作ログはありません",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    items(logsWithParcel, key = { it.log.id }) { logWithParcel ->
                        LogRow(
                            logWithParcel = logWithParcel,
                            isCancellable = isCancellable(logWithParcel),
                            onCancelRequest = { onCancelRequest(logWithParcel) }
                        )
                        HorizontalDivider(
                            color = Color(0xFFDDDDDD),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

// ===== ログ行（荷物の持ち主を表示、事務当番はかっこ） =====
@Composable
private fun LogRow(
    logWithParcel: OperationLogWithParcel,
    isCancellable: Boolean,
    onCancelRequest: () -> Unit
) {
    val log = logWithParcel.log
    val typeLabel = when (log.operationType) {
        "REGISTER" -> "受け取り"
        "DELIVER" -> "引き渡し"
        "CANCEL_REGISTER" -> "登録取消"
        "CANCEL_DELIVER" -> "渡取消"
        "DUTY_CHANGE" -> "当番交代"
        else -> log.operationType
    }

    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(log.createdAt))

    // 荷物関連操作: 持ち主(部屋番号+名前) + 事務当番(かっこ)
    // 当番交代等: 事務当番名のみ
    val ownerDisplay = if (logWithParcel.parcelOwnerRoom != null && logWithParcel.parcelOwnerName != null) {
        "${logWithParcel.parcelOwnerRoom} ${logWithParcel.parcelOwnerName}"
    } else {
        null
    }
    val dutyDisplay = log.operatedByName ?: ""

    val displayText = if (ownerDisplay != null) {
        "$ownerDisplay (事務当番:$dutyDisplay)"
    } else {
        dutyDisplay
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateStr,
            fontSize = 16.sp,
            modifier = Modifier.width(110.dp)
        )

        Text(
            text = typeLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(90.dp)
        )

        Text(
            text = displayText,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isCancellable) {
            Text(
                text = "取消",
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, Color.Red, RoundedCornerShape(4.dp))
                    .clickable { onCancelRequest() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// ===== フッター =====
@Composable
private fun FooterBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(FooterColor)
            .padding(horizontal = 70.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Copyright \u00A9 2026 Kumano Dormitory IT Section",
            color = FooterFontColor,
            fontSize = 16.sp
        )
    }
}

// ===== 取消確認ダイアログ =====
@Composable
private fun CancelConfirmDialog(
    log: OperationLogEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("操作の取消") },
        text = { Text("この操作を取り消しますか？\n(荷物の状態も元に戻ります)") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("取消実行", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
