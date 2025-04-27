package service

import kotlinx.coroutines.flow.StateFlow
import model.BleDevice

/**
 * Interface for BLE service implementations
 */
interface IBleService {
    /**
     * State flow of unbonded devices
     */
    val unbondedDevices: StateFlow<List<BleDevice>>
    
    /**
     * State flow of bonded devices
     */
    val bondedDevices: StateFlow<List<BleDevice>>
    
    /**
     * State flow of the currently selected device
     */
    val selectedDevice: StateFlow<BleDevice?>
    
    /**
     * Start scanning for BLE devices with the specified service UUID
     */
    fun startScanning()
    
    /**
     * Stop scanning for BLE devices
     */
    fun stopScanning()
    
    /**
     * Connect to a device and read its characteristics
     */
    fun connectToDevice(device: BleDevice)
    
    /**
     * Bond with a device
     */
    fun bondWithDevice(device: BleDevice)
    
    /**
     * Set the speed of a device
     */
    fun setSpeed(device: BleDevice, speed: Int)
    
    /**
     * Set the acceleration of a device
     */
    fun setAcceleration(device: BleDevice, acceleration: Int)
    
    /**
     * Set the direction of a device
     */
    fun setDirection(device: BleDevice, direction: String)
    
    /**
     * Set the long name of a device
     */
    fun setLongName(device: BleDevice, longName: String)
    
    /**
     * Set the network key of a device
     */
    fun setNetworkKey(device: BleDevice, networkKey: String)
}