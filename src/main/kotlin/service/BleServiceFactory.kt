package service

/**
 * Factory for creating BLE service implementations
 */
object BleServiceFactory {
    /**
     * Create a BLE service implementation
     * @param useSimulation Whether to use a simulated BLE service or a real one
     * @return An implementation of IBleService
     */
    fun createBleService(useSimulation: Boolean = true): IBleService {
        return if (useSimulation) {
            // Use simulated BLE service
            BleService()
        } else {
            try {
                // Try to use real BLE service
                val realService = Class.forName("service.RealBleService").getDeclaredConstructor().newInstance() as IBleService
                println("Using real BLE service")
                realService
            } catch (e: Exception) {
                // Fall back to simulated BLE service
                println("Failed to create real BLE service: ${e.message}")
                println("Falling back to simulated BLE service")
                BleService()
            }
        }
    }
}