package service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import model.BleDevice
import kotlin.random.Random

/**
 * Simulated service for handling BLE operations
 */
class BleService : IBleService {
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

    // Simulated device names
    private val deviceNames = listOf(
        "Train01", "Train02", "Train03", "Train04", "Train05",
        "Engine01", "Engine02", "Engine03", "Engine04", "Engine05"
    )

    init {
        // Generate some initial simulated devices
        generateSimulatedDevices()
    }

    /**
     * Generate simulated BLE devices
     */
    private fun generateSimulatedDevices() {
        val devices = mutableListOf<BleDevice>()

        // Create 5 unbonded devices
        for (i in 0 until 5) {
            val shortName = deviceNames[i]
            val address = "00:11:22:33:44:${i.toString(16).padStart(2, '0')}"

            devices.add(
                BleDevice(
                    address = address,
                    shortName = shortName,
                    isBonded = false
                )
            )
        }

        // Create 2 bonded devices
        for (i in 5 until 7) {
            val shortName = deviceNames[i]
            val address = "00:11:22:33:44:${i.toString(16).padStart(2, '0')}"

            val device = BleDevice(
                address = address,
                shortName = shortName,
                longName = "My $shortName",
                dccCode = Random.nextInt(0, 30000),
                speed = Random.nextInt(0, 128),
                acceleration = Random.nextInt(-3, 4),
                direction = if (Random.nextBoolean()) BleDevice.DIRECTION_LEFT else BleDevice.DIRECTION_RIGHT,
                isBonded = true
            )

            devices.add(device)
        }

        // Update device lists
        updateDeviceLists(devices)
    }

    /**
     * Start scanning for BLE devices with the specified service UUID
     */
    override fun startScanning() {
        // Simulate scanning by periodically adding new devices
        coroutineScope.launch {
            while (true) {
                delay(5000) // Simulate delay between discoveries

                // Only add new devices if we have fewer than 10
                if (_unbondedDevices.value.size + _bondedDevices.value.size < 10) {
                    val index = Random.nextInt(7, 10)
                    val shortName = deviceNames[index]
                    val address = "00:11:22:33:44:${index.toString(16).padStart(2, '0')}"

                    val device = BleDevice(
                        address = address,
                        shortName = shortName,
                        isBonded = false
                    )

                    // Update device lists
                    val currentDevices = _unbondedDevices.value.toMutableList()
                    currentDevices.add(device)
                    _unbondedDevices.value = currentDevices.sortedBy { it.shortName }
                }
            }
        }
    }

    /**
     * Stop scanning for BLE devices
     */
    override fun stopScanning() {
        // Nothing to do in simulation
    }

    /**
     * Connect to a device and read its characteristics
     */
    override fun connectToDevice(device: BleDevice) {
        // Stop notifications from previously connected device
        stopSpeedNotifications()

        coroutineScope.launch {
            // Simulate connection delay
            delay(500)

            // Update selected device
            _selectedDevice.value = device
            currentDeviceAddress = device.address

            // Start speed notifications
            startSpeedNotifications(device)
        }
    }

    /**
     * Bond with a device
     */
    override fun bondWithDevice(device: BleDevice) {
        coroutineScope.launch {
            // Simulate bonding delay
            delay(1000)

            // Update device status
            device.isBonded = true
            device.longName = "My ${device.shortName}"
            device.dccCode = Random.nextInt(0, 30000)

            // Update device lists
            updateDeviceLists(device)
        }
    }

    /**
     * Set the speed of a device
     */
    override fun setSpeed(device: BleDevice, speed: Int) {
        val validSpeed = device.validateSpeed(speed)

        coroutineScope.launch {
            // Simulate write delay
            delay(100)

            // Update device
            device.speed = validSpeed
            _selectedDevice.value = device.copy()
            updateDeviceLists(device)
        }
    }

    /**
     * Set the acceleration of a device
     */
    override fun setAcceleration(device: BleDevice, acceleration: Int) {
        val validAccel = device.validateAcceleration(acceleration)

        coroutineScope.launch {
            // Simulate write delay
            delay(100)

            // Update device
            device.acceleration = validAccel
            _selectedDevice.value = device.copy()
            updateDeviceLists(device)
        }
    }

    /**
     * Set the direction of a device
     */
    override fun setDirection(device: BleDevice, direction: String) {
        coroutineScope.launch {
            // Simulate write delay
            delay(100)

            // Update device
            device.direction = direction
            _selectedDevice.value = device.copy()
            updateDeviceLists(device)
        }
    }

    /**
     * Set the long name of a device
     */
    override fun setLongName(device: BleDevice, longName: String) {
        val validLongName = device.validateLongName(longName)

        coroutineScope.launch {
            // Simulate write delay
            delay(100)

            // Update device
            device.longName = validLongName
            _selectedDevice.value = device.copy()
            updateDeviceLists(device)
        }
    }

    /**
     * Set the network key of a device
     */
    override fun setNetworkKey(device: BleDevice, networkKey: String) {
        coroutineScope.launch {
            // Simulate write delay
            delay(100)

            // Update device
            device.networkKey = networkKey
            _selectedDevice.value = device.copy()
            updateDeviceLists(device)
        }
    }

    /**
     * Start speed notifications from the device
     */
    private fun startSpeedNotifications(device: BleDevice) {
        if (isNotifying) return

        isNotifying = true
        coroutineScope.launch {
            while (isNotifying && currentDeviceAddress == device.address) {
                delay(2000) // Simulate notification interval

                // Only update if this is still the current device
                if (currentDeviceAddress == device.address) {
                    // Simulate speed changes
                    val currentSpeed = device.speed
                    val newSpeed = when {
                        currentSpeed <= 0 -> Random.nextInt(0, 10)
                        currentSpeed >= 128 -> Random.nextInt(118, 128)
                        else -> currentSpeed + Random.nextInt(-5, 6)
                    }

                    device.speed = device.validateSpeed(newSpeed)
                    _selectedDevice.value = device.copy()
                    updateDeviceLists(device)
                }
            }
        }
    }

    /**
     * Stop speed notifications
     */
    private fun stopSpeedNotifications() {
        isNotifying = false
        currentDeviceAddress = null
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
}
