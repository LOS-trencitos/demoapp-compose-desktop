import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import model.BleDevice
import service.IBleService
import service.BleServiceFactory

@Composable
@Preview
fun App() {
    val bleService = remember { BleServiceFactory.createBleService() }
    val coroutineScope = rememberCoroutineScope()

    // State flows
    val unbondedDevices by bleService.unbondedDevices.collectAsState()
    val bondedDevices by bleService.bondedDevices.collectAsState()
    val selectedDevice by bleService.selectedDevice.collectAsState()

    // Start scanning for devices
    LaunchedEffect(Unit) {
        bleService.startScanning()
    }

    MaterialTheme {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left column - Unbonded devices
            Column(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "Available Devices",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )

                Divider()

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                ) {
                    items(unbondedDevices) { device ->
                        DeviceItem(
                            device = device,
                            isSelected = false,
                            onClick = { /* Single click does nothing for unbonded devices */ },
                            onDoubleClick = {
                                // Bond with device on double click
                                coroutineScope.launch {
                                    bleService.bondWithDevice(device)
                                }
                            }
                        )
                    }
                }
            }

            // Center column - Bonded devices
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "Bonded Devices",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )

                Divider()

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                ) {
                    items(bondedDevices) { device ->
                        DeviceItem(
                            device = device,
                            isSelected = selectedDevice?.address == device.address,
                            onClick = {
                                // Connect to device on single click
                                coroutineScope.launch {
                                    bleService.connectToDevice(device)
                                }
                            },
                            onDoubleClick = { /* Double click does nothing for bonded devices */ }
                        )
                    }
                }
            }

            // Right column - Device controls
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "Device Controls",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )

                Divider()

                if (selectedDevice != null) {
                    DeviceControls(
                        device = selectedDevice!!,
                        onSpeedChange = { speed ->
                            coroutineScope.launch {
                                bleService.setSpeed(selectedDevice!!, speed)
                            }
                        },
                        onAccelerationChange = { acceleration ->
                            coroutineScope.launch {
                                bleService.setAcceleration(selectedDevice!!, acceleration)
                            }
                        },
                        onDirectionChange = { direction ->
                            coroutineScope.launch {
                                bleService.setDirection(selectedDevice!!, direction)
                            }
                        },
                        onLongNameChange = { longName ->
                            coroutineScope.launch {
                                bleService.setLongName(selectedDevice!!, longName)
                            }
                        },
                        onNetworkKeyChange = { networkKey ->
                            coroutineScope.launch {
                                bleService.setNetworkKey(selectedDevice!!, networkKey)
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Select a device to control",
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: BleDevice,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color.LightGray.copy(alpha = 0.3f) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleClick() }
                )
            },
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = if (device.isBonded) device.longName else device.shortName,
                fontWeight = FontWeight.Bold
            )

            if (device.isBonded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DCC: ${device.dccCode} | Speed: ${device.speed} | Dir: ${device.direction}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )}
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = device.address,                    fontSize = 12.sp,
                    color = Color.Gray
                )

        }
    }
}

@Composable
fun DeviceControls(
    device: BleDevice,
    onSpeedChange: (Int) -> Unit,
    onAccelerationChange: (Int) -> Unit,
    onDirectionChange: (String) -> Unit,
    onLongNameChange: (String) -> Unit,
    onNetworkKeyChange: (String) -> Unit
) {
    var longNameText by remember(device.address) { mutableStateOf(device.longName) }
    var networkKeyText by remember(device.address) { mutableStateOf(device.networkKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row with speed gauge and acceleration slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Acceleration slider (left)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Acceleration", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = device.acceleration.toFloat(),
                    onValueChange = { onAccelerationChange(it.toInt()) },
                    valueRange = BleDevice.MIN_ACCELERATION.toFloat()..BleDevice.MAX_ACCELERATION.toFloat(),
                    steps = BleDevice.MAX_ACCELERATION - BleDevice.MIN_ACCELERATION - 1,
                    modifier = Modifier.width(150.dp)
                )

                Text(
                    text = device.acceleration.toString(),
                    fontSize = 12.sp
                )
            }

            // Speed gauge (right)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Speed", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Gray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = device.speed.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Speed indicator
                    val angle = 270f * (device.speed.toFloat() / BleDevice.MAX_SPEED.toFloat())
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .rotate(angle)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(40.dp)
                                .background(Color.Red)
                                .align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Speed slider
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Slider(
                value = device.speed.toFloat(),
                onValueChange = { onSpeedChange(it.toInt()) },
                valueRange = 0f..BleDevice.MAX_SPEED.toFloat(),
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Text(
                text = "Speed: ${device.speed}",
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Direction buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left direction button
            Button(
                onClick = { onDirectionChange(BleDevice.DIRECTION_LEFT) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (device.direction == BleDevice.DIRECTION_LEFT) 
                        MaterialTheme.colors.primary else Color.Gray
                ),
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "◀",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Right direction button
            Button(
                onClick = { onDirectionChange(BleDevice.DIRECTION_RIGHT) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (device.direction == BleDevice.DIRECTION_RIGHT) 
                        MaterialTheme.colors.primary else Color.Gray
                ),
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "▶",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Long name input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Long name:",
                modifier = Modifier.width(100.dp)
            )

            OutlinedTextField(
                value = longNameText,
                onValueChange = { longNameText = it },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            IconButton(
                onClick = { onLongNameChange(longNameText) }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save long name"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Network key input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Network key:",
                modifier = Modifier.width(100.dp)
            )

            OutlinedTextField(
                value = networkKeyText,
                onValueChange = { networkKeyText = it },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            IconButton(
                onClick = { onNetworkKeyChange(networkKeyText) }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save network key"
                )
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BLE Train Controller",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        App()
    }
}
