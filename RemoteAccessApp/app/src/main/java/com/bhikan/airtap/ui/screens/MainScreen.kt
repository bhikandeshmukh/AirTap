package com.bhikan.airtap.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bhikan.airtap.R
import com.bhikan.airtap.server.WebServerService
import com.bhikan.airtap.service.ScreenCaptureService
import com.bhikan.airtap.util.QRCodeGenerator
import com.bhikan.airtap.util.copyToClipboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    serverState: WebServerService.ServerState,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onStartScreenMirror: () -> Unit = {},
    userEmail: String? = null
) {
    val context = LocalContext.current
    var showQRDialog by remember { mutableStateOf(false) }
    val isScreenMirroring by ScreenCaptureService.isStreaming.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(false) }
    var requiredPermission by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            onStartServer()
        } else {
            // Show a rationale if any permission is denied
            requiredPermission = permissions.filterValues { !it }.keys.first()
            showPermissionDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User Info Card
            if (userEmail != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.registered_email),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = userEmail,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (serverState.isRunning)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (serverState.isRunning) Color(0xFF4CAF50)
                                else Color(0xFF9E9E9E)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (serverState.isRunning)
                                Icons.Default.Wifi
                            else
                                Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (serverState.isRunning) stringResource(id = R.string.server_running) else stringResource(id = R.string.server_stopped),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (serverState.isRunning) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(id = R.string.connect_from_browser),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val url = "http://${serverState.localIp}:${serverState.port}"

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = url,
                                    modifier = Modifier.weight(1f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )

                                IconButton(onClick = { showQRDialog = true }) {
                                    Icon(Icons.Default.QrCode, contentDescription = stringResource(id = R.string.show_qr))
                                }

                                IconButton(onClick = { copyToClipboard(context, url) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(id = R.string.copy_url))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Start/Stop Button
            Button(
                onClick = {
                    if (serverState.isRunning) {
                        onStopServer()
                    } else {
                        // Check for permissions before starting the server
                        val permissions = getRequiredPermissions()
                        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                            onStartServer()
                        } else {
                            permissionLauncher.launch(permissions.toTypedArray())
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (serverState.isRunning)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (serverState.isRunning)
                        Icons.Default.Stop
                    else
                        Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (serverState.isRunning) stringResource(id = R.string.stop_server) else stringResource(id = R.string.start_server),
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Screen Mirror Button
            OutlinedButton(
                onClick = {
                    if (isScreenMirroring) {
                        val stopIntent = Intent(context, ScreenCaptureService::class.java).apply {
                            action = ScreenCaptureService.ACTION_STOP
                        }
                        context.startService(stopIntent)
                    } else {
                        onStartScreenMirror()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isScreenMirroring)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isScreenMirroring)
                        Icons.AutoMirrored.Filled.StopScreenShare
                    else
                        Icons.AutoMirrored.Filled.ScreenShare,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScreenMirroring) stringResource(id = R.string.stop_screen_mirror) else stringResource(id = R.string.start_screen_mirror),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.how_to_connect),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InstructionItem("1", stringResource(id = R.string.instruction_1))
                    InstructionItem("2", stringResource(id = R.string.instruction_2))
                    InstructionItem("3", stringResource(id = R.string.instruction_3))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Remote Access Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.remote_access),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.remote_access_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }

    // QR Code Dialog
    if (showQRDialog && serverState.isRunning) {
        QRCodeDialog(
            url = "http://${serverState.localIp}:${serverState.port}",
            onDismiss = { showQRDialog = false }
        )
    }

    if (showPermissionDialog) {
        PermissionRequestDialog(
            permission = requiredPermission,
            onDismiss = { showPermissionDialog = false },
            onConfirm = { showPermissionDialog = false }
        )
    }
}

@Composable
private fun QRCodeDialog(
    url: String,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(url) {
        QRCodeGenerator.generateQRCode(url, 400)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(id = R.string.scan_to_connect), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                qrBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(id = R.string.qr_code),
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = url,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.close))
            }
        }
    )
}

@Composable
private fun InstructionItem(number: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun getRequiredPermissions(): List<String> {
    val permissions = mutableListOf<String>()

    // Storage permissions
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    // SMS permissions
    permissions.add(Manifest.permission.READ_SMS)
    permissions.add(Manifest.permission.SEND_SMS)
    permissions.add(Manifest.permission.RECEIVE_SMS)
    permissions.add(Manifest.permission.READ_CONTACTS)

    return permissions
}
