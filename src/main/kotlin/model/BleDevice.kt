package model

import java.util.UUID

/**
 * Constants for BLE service and characteristics
 */
object BleConstants {
    // Service UUID for train devices
    val SERVICE_UUID: UUID = UUID.fromString("fcbd0001-5e25-4387-99b7-53a5495a0c35") // Replace with actual UUID
    
    // Characteristic UUIDs
    val SPEED_CHARACTERISTIC_UUID: UUID = UUID.fromString("fcbd0002-5e25-4387-99b7-53a5495a0c35")
    val ACCELERATION_CHARACTERISTIC_UUID: UUID = UUID.fromString("fcbd0003-5e25-4387-99b7-53a5495a0c35")
    val DIRECTION_CHARACTERISTIC_UUID: UUID = UUID.fromString("fcbd0005-5e25-4387-99b7-53a5495a0c35")
    val DCC_CODE_CHARACTERISTIC_UUID: UUID = UUID.fromString("fcbd0007-5e25-4387-99b7-53a5495a0c35")
    val LONG_NAME_CHARACTERISTIC_UUID: UUID = UUID.fromString("fcbd0006-5e25-4387-99b7-53a5495a0c35")
    val NETWORK_KEY_CHARACTERISTIC_UUID: UUID = UUID.fromString("fcbd000a-5e25-4387-99b7-53a5495a0c35")
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