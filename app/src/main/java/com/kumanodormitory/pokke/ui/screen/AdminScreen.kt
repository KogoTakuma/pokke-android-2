package com.kumanodormitory.pokke.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.ui.util.formatDateTime
import com.kumanodormitory.pokke.ui.util.formatParcelType
import com.kumanodormitory.pokke.ui.viewmodel.AdminUiState
import com.kumanodormitory.pokke.ui.viewmodel.AdminViewModel
import com.kumanodormitory.pokke.ui.viewmodel.HealthStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("管理用画面") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (!uiState.isAuthenticated) {
            PasswordAuthScreen(
                passwordError = uiState.passwordError,
                onAuthenticate = { viewModel.authenticate(it) },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            AdminMenuContent(
                uiState = uiState,
                onSyncRyosei = { viewModel.syncRyosei() },
                onSyncParcels = { viewModel.syncParcels() },
                onHealthCheck = { viewModel.checkHealth() },
                onConfirmLost = { viewModel.confirmLost(it) },
                onArchiveLost = { viewModel.archiveLostParcels() },
                onToggleArchived = { viewModel.toggleShowArchived() },
                onGenerateSeed = { viewModel.generateSeedData() },
                onDeleteSeed = { viewModel.deleteSeedData() },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun PasswordAuthScreen(
    passwordError: String?,
    onAuthenticate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "管理者認証",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "本当に実行する場合はパスワードを入力してください。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("パスワード") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onAuthenticate(password) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("認証")
                }
            }
        }
    }
}

@Composable
private fun AdminMenuContent(
    uiState: AdminUiState,
    onSyncRyosei: () -> Unit,
    onSyncParcels: () -> Unit,
    onHealthCheck: () -> Unit,
    onConfirmLost: (String) -> Unit,
    onArchiveLost: () -> Unit,
    onToggleArchived: () -> Unit,
    onGenerateSeed: () -> Unit,
    onDeleteSeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 左カラム: 同期 & シードデータ管理
        Column(
            modifier = Modifier
                .width(300.dp)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "システム管理",
                style = MaterialTheme.typography.titleMedium
            )
            
            // 同期セクション
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "サーバー同期", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onSyncRyosei,
                        enabled = !uiState.isSyncingRyosei,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF60DEA0))
                    ) {
                        if (uiState.isSyncingRyosei) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("寮生データ同期", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onSyncParcels,
                        enabled = !uiState.isSyncingParcel,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90D9))
                    ) {
                        if (uiState.isSyncingParcel) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("荷物データ同期", color = Color.White)
                    }
                }
            }

            // シードデータセクション
            Card {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "開発用データ (Seed)", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onGenerateSeed,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("シードデータ投入(100名)")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onDeleteSeed,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("シードデータのみ削除")
                    }
                }
            }
        }

        // 中カラム: 同期ステータス + ヘルスチェック
        Column(
            modifier = Modifier
                .width(250.dp)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "同期ステータス",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "寮生: サーバーから取得\n荷物: 15分毎に自動同期",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "サーバーヘルス",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val (statusText, statusColor) = when (uiState.healthStatus) {
                    HealthStatus.OK -> "OK" to Color(0xFF4CAF50)
                    HealthStatus.ERROR -> "ERROR" to Color(0xFFFF2222)
                    HealthStatus.UNKNOWN -> "未確認" to Color.Gray
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(statusColor, shape = MaterialTheme.shapes.small)
                )
                Text(text = statusText, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = onHealthCheck,
                enabled = !uiState.isCheckingHealth,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isCheckingHealth) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("ヘルスチェック")
            }
        }

        // 右カラム: 紛失荷物管理
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp)
        ) {
            // タブ切り替え: 紛失荷物 / アーカイブ済み
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { if (uiState.showArchived) onToggleArchived() }
                ) {
                    Text(
                        text = "紛失荷物（${uiState.lostParcels.size}）",
                        fontWeight = if (!uiState.showArchived) FontWeight.Bold else FontWeight.Normal,
                        color = if (!uiState.showArchived) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { if (!uiState.showArchived) onToggleArchived() }
                ) {
                    Text(
                        text = "アーカイブ済（${uiState.archivedParcels.size}）",
                        fontWeight = if (uiState.showArchived) FontWeight.Bold else FontWeight.Normal,
                        color = if (uiState.showArchived) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!uiState.showArchived) {
                // 紛失荷物一覧
                if (uiState.lostParcels.isEmpty()) {
                    Text(
                        text = "紛失フラグのついた荷物はありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    var showArchiveDialog by remember { mutableStateOf(false) }

                    Button(
                        onClick = { showArchiveDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF666666)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "一括アーカイブ（${uiState.lostParcels.size}件）",
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.lostParcels, key = { it.id }) { parcel ->
                            LostParcelItem(
                                parcel = parcel,
                                onConfirmLost = onConfirmLost
                            )
                        }
                    }

                    if (showArchiveDialog) {
                        AlertDialog(
                            onDismissRequest = { showArchiveDialog = false },
                            title = { Text("紛失荷物アーカイブ") },
                            text = {
                                Text(
                                    "紛失フラグのついた荷物${uiState.lostParcels.size}件をすべてアーカイブします。\n" +
                                            "アーカイブ後は一覧に表示されなくなります。"
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        onArchiveLost()
                                        showArchiveDialog = false
                                    }
                                ) {
                                    Text("アーカイブ", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showArchiveDialog = false }) {
                                    Text("キャンセル")
                                }
                            }
                        )
                    }
                }
            } else {
                // アーカイブ済み一覧
                if (uiState.archivedParcels.isEmpty()) {
                    Text(
                        text = "アーカイブ済みの荷物はありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.archivedParcels, key = { it.id }) { parcel ->
                            ArchivedParcelItem(parcel = parcel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LostParcelItem(
    parcel: ParcelEntity,
    onConfirmLost: (String) -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${parcel.ownerRoomName} ${parcel.ownerName}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "種別: ${formatParcelType(parcel.parcelType)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "登録: ${formatDateTime(parcel.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF2222)
                )
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("紛失確定", color = Color.White)
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("紛失確定") },
            text = {
                Text(
                    "本当に実行する場合は「確定」を押してください。\n\n" +
                            "${parcel.ownerRoomName} ${parcel.ownerName} の荷物（${formatParcelType(parcel.parcelType)}）" +
                            "を紛失確定します。この操作は取り消せません。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmLost(parcel.id)
                        showConfirmDialog = false
                    }
                ) {
                    Text("確定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun ArchivedParcelItem(parcel: ParcelEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "${parcel.ownerRoomName} ${parcel.ownerName}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "種別: ${formatParcelType(parcel.parcelType)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "登録: ${formatDateTime(parcel.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            parcel.lostConfirmedAt?.let {
                Text(
                    text = "アーカイブ: ${formatDateTime(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
