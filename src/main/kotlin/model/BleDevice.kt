package model

import java.util.UUID

/**
 * Constants for BLE service and characteristics
 */
object BleConstants {
    // Service UUID for train devices
    val SERVICE_UUID: UUID = UUID.fromString("00001234-0000-1000-8000-00805f9b34fb") // Replace with actual UUID
    
    // Characteristic UUIDs
    val SPEED_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002001-0000-1000-8000-00805f9b34fb")
    val ACCELERATION_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002002-0000-1000-8000-00805f9b34fb")
    val DIRECTION_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002003-0000-1000-8000-00805f9b34fb")
    val DCC_CODE_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002004-0000-1000-8000-00805f9b34fb")
    val LONG_NAME_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002005-0000-1000-8000-00805f9b34fb")
    val NETWORK_KEY_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002006-0000-1000-8000-00805f9b34fb")
}

/**
 * Represents a BLE train device
 */
data class BleDevice(
    val address: String,
    val shortName: String, // 8 Byte long Short name
    var longName: String = "", // User definable long name up to 100 UTF-8 Characters
    var dccCode: Int = 0, // Numerical DCC Code between 0 and 30000
    var speed: Int = 0, // Speed value between 0 and 128
    var acceleration: Int = 0, // Acceleration value between -3 and +3
    var direction: String = "00", // Direction value: "00" (stop), "01" (right), "10" (left)
    var networkKey: String = "", // Network key for mesh network
    var isBonded: Boolean = false // Whether the device is bonded
) {
    companion object {
        // Direction constants
        const val DIRECTION_STOP = "00"
        const val DIRECTION_RIGHT = "01"
        const val DIRECTION_LEFT = "10"
        
        // Limits
        const val MAX_SPEED = 128
        const val MIN_ACCELERATION = -3
        const val MAX_ACCELERATION = 3
        const val MIN_DCC_CODE = 0
        const val MAX_DCC_CODE = 30000
        const val MAX_LONG_NAME_LENGTH = 100
    }
    
    // Validate and constrain values
    fun validateSpeed(value: Int): Int = value.coerceIn(0, MAX_SPEED)
    
    fun validateAcceleration(value: Int): Int = value.coerceIn(MIN_ACCELERATION, MAX_ACCELERATION)
    
    fun validateDccCode(value: Int): Int = value.coerceIn(MIN_DCC_CODE, MAX_DCC_CODE)
    
    fun validateLongName(value: String): String = 
        if (value.length > MAX_LONG_NAME_LENGTH) value.substring(0, MAX_LONG_NAME_LENGTH) else value
}