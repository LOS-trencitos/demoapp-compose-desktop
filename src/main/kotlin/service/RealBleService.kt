package service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import model.BleConstants
import model.BleDevice
import org.simplejavable.Adapter
import org.simplejavable.Peripheral
import org.simplejavable.SimpleBleFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Real BLE service implementation using SimpleJavaBLE
 */
class RealBleService : IBleService {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // State flows for UI updates
    private val _unbondedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val unbondedDevices: StateFlow<List<BleDevice>> = _unbondedDevices.asStateFlow()

    private val _bondedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val bondedDevices: StateFlow<List<BleDevice>> = _bondedDevices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BleDevice?>(null)
    override val selectedDevice: StateFlow<BleDevice?> = _selectedDevice.asStateFlow()

    // Currently selected device for notifications
    private var currentDeviceAddress: String? = null
    private var isNotifying = false
    
    // SimpleJavaBLE objects
    private val factory = SimpleBleFactory()
    private val adapter: Adapter?
    private val peripherals = mutableMapOf<String, Peripheral>()
    private var connectedPeripheral: Peripheral? = null
    
    init {
        // Initialize the BLE adapter
        val adapters = factory.getAdapters()
        adapter = if (adapters.isNotEmpty()) adapters[0] else null
        
        if (adapter == null) {
            println("No BLE adapter found")
        } else {
            println("Using BLE adapter: " + adapter.getIdentifier())
        }
    }
    
    /**
     * Start scanning for BLE devices with the specified service UUID
     */
    override fun startScanning() {
        if (adapter == null) return
        
        // Set up scan callback
        adapter.setScanCallback { peripheral ->
            // Check if the peripheral has the required service
            val services = peripheral.services()
            if (services.contains(BleConstants.SERVICE_UUID.toString())) {
                val address = peripheral.address()
                val name = peripheral.identifier()
                
                // Create a BleDevice object
                val device = BleDevice(
                    address = address,
                    shortName = name.take(8), // Limit to 8 bytes
                    isBonded = false
                )
                
                // Store the peripheral
                peripherals[address] = peripheral
                
                // Update device lists
                val currentDevices = _unbondedDevices.value.toMutableList()
                val existingIndex = currentDevices.indexOfFirst { it.address == address }
                if (existingIndex >= 0) {
                    currentDevices[existingIndex] = device
                } else {
                    currentDevices.add(device)
                }
                _unbondedDevices.value = currentDevices.sortedBy { it.shortName }
            }
        }
        
        // Start scanning
        coroutineScope.launch {
            try {
                adapter.scanFor(0) // Scan indefinitely
            } catch (e: Exception) {
                println("Error scanning for devices: ${e.message}")
            }
        }
    }
    
    /**
     * Stop scanning for BLE devices
     */
    override fun stopScanning() {
        adapter?.stopScan()
    }
    
    /**
     * Connect to a device and read its characteristics
     */
    override fun connectToDevice(device: BleDevice) {
        // Stop notifications from previously connected device
        stopSpeedNotifications()
        
        val peripheral = peripherals[device.address] ?: return
        
        coroutineScope.launch {
            try {
                // Connect to the peripheral
                peripheral.connect()
                connectedPeripheral = peripheral
                
                // Read device characteristics
                readDeviceCharacteristics(peripheral, device)
                
                // Update selected device
                _selectedDevice.value = device
                currentDeviceAddress = device.address
                
                // Start speed notifications
                startSpeedNotifications(device)
            } catch (e: Exception) {
                println("Error connecting to device: ${e.message}")
            }
        }
    }
    
    /**
     * Bond with a device
     */
    override fun bondWithDevice(device: BleDevice) {
        val peripheral = peripherals[device.address] ?: return
        
        coroutineScope.launch {
            try {
                // Connect to the peripheral
                peripheral.connect()
                
                // Read device characteristics
                readDeviceCharacteristics(peripheral, device)
                
                // Update device status
                device.isBonded = true
                
                // Update device lists
                updateDeviceLists(device)
                
                // Disconnect
                peripheral.disconnect()
            } catch (e: Exception) {
                println("Error bonding with device: ${e.message}")
            }
        }
    }
    
    /**
     * Read all characteristics from a device
     */
    private fun readDeviceCharacteristics(peripheral: Peripheral, device: BleDevice) {
        try {
            // Read long name
            val longNameBytes = peripheral.read(
                BleConstants.SERVICE_UUID.toString(),
                BleConstants.LONG_NAME_CHARACTERISTIC_UUID.toString()
            )
            if (longNameBytes.isNotEmpty()) {
                device.longName = String(longNameBytes)
            }
            
            // Read DCC code
            val dccCodeBytes = peripheral.read(
                BleConstants.SERVICE_UUID.toString(),
                BleConstants.DCC_CODE_CHARACTERISTIC_UUID.toString()
            )
            if (dccCodeBytes.isNotEmpty()) {
                val buffer = ByteBuffer.wrap(dccCodeBytes).order(ByteOrder.LITTLE_ENDIAN)
                device.dccCode = buffer.int
            }
            
            // Read speed
            val speedBytes = peripheral.read(
                BleConstants.SERVICE_UUID.toString(),
                BleConstants.SPEED_CHARACTERISTIC_UUID.toString()
            )
            if (speedBytes.isNotEmpty()) {
                device.speed = speedBytes[0].toInt() and 0xFF
            }
            
            // Read acceleration
            val accelerationBytes = peripheral.read(
                BleConstants.SERVICE_UUID.toString(),
                BleConstants.ACCELERATION_CHARACTERISTIC_UUID.toString()
            )
            if (accelerationBytes.isNotEmpty()) {
                device.acceleration = accelerationBytes[0].toInt()
            }
            
            // Read direction
            val directionBytes = peripheral.read(
                BleConstants.SERVICE_UUID.toString(),
                BleConstants.DIRECTION_CHARACTERISTIC_UUID.toString()
            )
            if (directionBytes.isNotEmpty()) {
                device.direction = String(directionBytes)
            }
            
            // Read network key
            val networkKeyBytes = peripheral.read(
                BleConstants.SERVICE_UUID.toString(),
                BleConstants.NETWORK_KEY_CHARACTERISTIC_UUID.toString()
            )
            if (networkKeyBytes.isNotEmpty()) {
                device.networkKey = String(networkKeyBytes)
            }
        } catch (e: Exception) {
            println("Error reading device characteristics: ${e.message}")
        }
    }
    
    /**
     * Set the speed of a device
     */
    override fun setSpeed(device: BleDevice, speed: Int) {
        val validSpeed = device.validateSpeed(speed)
        val peripheral = connectedPeripheral ?: return
        
        coroutineScope.launch {
            try {
                // Write speed characteristic
                val speedBytes = byteArrayOf(validSpeed.toByte())
                peripheral.write(
                    BleConstants.SERVICE_UUID.toString(),
                    BleConstants.SPEED_CHARACTERISTIC_UUID.toString(),
                    speedBytes,
                    false // Write without response
                )
                
                // Update device
                device.speed = validSpeed
                _selectedDevice.value = device.copy()
                updateDeviceLists(device)
            } catch (e: Exception) {
                println("Error setting speed: ${e.message}")
            }
        }
    }
    
    /**
     * Set the acceleration of a device
     */
    override fun setAcceleration(device: BleDevice, acceleration: Int) {
        val validAccel = device.validateAcceleration(acceleration)
        val peripheral = connectedPeripheral ?: return
        
        coroutineScope.launch {
            try {
                // Write acceleration characteristic
                val accelBytes = byteArrayOf(validAccel.toByte())
                peripheral.write(
                    BleConstants.SERVICE_UUID.toString(),
                    BleConstants.ACCELERATION_CHARACTERISTIC_UUID.toString(),
                    accelBytes,
                    false // Write without response
                )
                
                // Update device
                device.acceleration = validAccel
                _selectedDevice.value = device.copy()
                updateDeviceLists(device)
            } catch (e: Exception) {
                println("Error setting acceleration: ${e.message}")
            }
        }
    }
    
    /**
     * Set the direction of a device
     */
    override fun setDirection(device: BleDevice, direction: String) {
        val peripheral = connectedPeripheral ?: return
        
        coroutineScope.launch {
            try {
                // Write direction characteristic
                val directionBytes = direction.toByteArray()
                peripheral.write(
                    BleConstants.SERVICE_UUID.toString(),
                    BleConstants.DIRECTION_CHARACTERISTIC_UUID.toString(),
                    directionBytes,
                    false // Write without response
                )
                
                // Update device
                device.direction = direction
                _selectedDevice.value = device.copy()
                updateDeviceLists(device)
            } catch (e: Exception) {
                println("Error setting direction: ${e.message}")
            }
        }
    }
    
    /**
     * Set the long name of a device
     */
    override fun setLongName(device: BleDevice, longName: String) {
        val validLongName = device.validateLongName(longName)
        val peripheral = connectedPeripheral ?: return
        
        coroutineScope.launch {
            try {
                // Write long name characteristic
                val longNameBytes = validLongName.toByteArray()
                peripheral.write(
                    BleConstants.SERVICE_UUID.toString(),
                    BleConstants.LONG_NAME_CHARACTERISTIC_UUID.toString(),
                    longNameBytes,
                    true // Write with response
                )
                
                // Update device
                device.longName = validLongName
                _selectedDevice.value = device.copy()
                updateDeviceLists(device)
            } catch (e: Exception) {
                println("Error setting long name: ${e.message}")
            }
        }
    }
    
    /**
     * Set the network key of a device
     */
    override fun setNetworkKey(device: BleDevice, networkKey: String) {
        val peripheral = connectedPeripheral ?: return
        
        coroutineScope.launch {
            try {
                // Write network key characteristic
                val networkKeyBytes = networkKey.toByteArray()
                peripheral.write(
                    BleConstants.SERVICE_UUID.toString(),
                    BleConstants.NETWORK_KEY_CHARACTERISTIC_UUID.toString(),
                    networkKeyBytes,
                    true // Write with response
                )
                
                // Update device
                device.networkKey = networkKey
                _selectedDevice.value = device.copy()
                updateDeviceLists(device)
            } catch (e: Exception) {
                println("Error setting network key: ${e.message}")
            }
        }
    }
    
    /**
     * Start speed notifications from the device
     */
    private fun startSpeedNotifications(device: BleDevice) {
        if (isNotifying) return
        
        val peripheral = connectedPeripheral ?: return
        
        try {
            // Set up notification callback
            peripheral.setNotifyCallback(
                BleConstants.SERVICE_UUID.toString(),
                BleConstants.SPEED_CHARACTERISTIC_UUID.toString()
            ) { data ->
                if (data.isNotEmpty()) {
                    val speed = data[0].toInt() and 0xFF
                    device.speed = device.validateSpeed(speed)
                    _selectedDevice.value = device.copy()
                    updateDeviceLists(device)
                }
            }
            
            // Enable notifications
            peripheral.notifyCharacteristic(
                BleConstants.SERVICE_UUID.toString(),
                BleConstants.SPEED_CHARACTERISTIC_UUID.toString(),
                true
            )
            
            isNotifying = true
            currentDeviceAddress = device.address
        } catch (e: Exception) {
            println("Error starting speed notifications: ${e.message}")
        }
    }
    
    /**
     * Stop speed notifications
     */
    private fun stopSpeedNotifications() {
        val peripheral = connectedPeripheral ?: return
        
        try {
            // Disable notifications
            peripheral.notifyCharacteristic(
                BleConstants.SERVICE_UUID.toString(),
                BleConstants.SPEED_CHARACTERISTIC_UUID.toString(),
                false
            )
            
            isNotifying = false
            currentDeviceAddress = null
        } catch (e: Exception) {
            println("Error stopping speed notifications: ${e.message}")
        }
    }
    
    /**
     * Update the device lists
     */
    private fun updateDeviceLists(device: BleDevice) {
        if (device.isBonded) {
            // Add to bonded devices if not already present
            _bondedDevices.update { devices ->
                val existingIndex = devices.indexOfFirst { it.address == device.address }
                if (existingIndex >= 0) {
                    devices.toMutableList().apply {
                        this[existingIndex] = device
                    }.sortedWith(compareBy({ it.dccCode }, { it.longName }))
                } else {
                    (devices + device).sortedWith(compareBy({ it.dccCode }, { it.longName }))
                }
            }
            
            // Remove from unbonded devices
            _unbondedDevices.update { devices ->
                devices.filter { it.address != device.address }.sortedBy { it.shortName }
            }
        } else {
            // Add to unbonded devices if not already present
            _unbondedDevices.update { devices ->
                val existingIndex = devices.indexOfFirst { it.address == device.address }
                if (existingIndex >= 0) {
                    devices.toMutableList().apply {
                        this[existingIndex] = device
                    }.sortedBy { it.shortName }
                } else {
                    (devices + device).sortedBy { it.shortName }
                }
            }
        }
    }
    
    /**
     * Update the device lists with multiple devices
     */
    private fun updateDeviceLists(devices: List<BleDevice>) {
        val bonded = mutableListOf<BleDevice>()
        val unbonded = mutableListOf<BleDevice>()
        
        for (device in devices) {
            if (device.isBonded) {
                bonded.add(device)
            } else {
                unbonded.add(device)
            }
        }
        
        _bondedDevices.value = bonded.sortedWith(compareBy({ it.dccCode }, { it.longName }))
        _unbondedDevices.value = unbonded.sortedBy { it.shortName }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopScanning()
        connectedPeripheral?.disconnect()
    }
}