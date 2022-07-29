package com.paralainer.homebot.iot

import com.paralainer.homebot.config.Config
import com.paralainer.homebot.config.DeviceProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class DeviceStatusService(
    private val devicesConfig: Config,
    private val tuyaDevicesService: TuyaDevicesService
) {
    suspend fun getDevicesStatus(): List<DeviceStatus> =
        coroutineScope {
            devicesConfig.devices.map {
                async {
                    when (it.provider) {
                        DeviceProvider.Tuya ->
                            tuyaDevicesService.getDeviceStatus(it.id, it.type)
                    }
                }
            }.awaitAll()
        }
}

sealed interface DeviceStatus {
    val deviceId: String

    data class ClimateSensor(val temperature: Double, val humidity: Double, override val deviceId: String) :
        DeviceStatus
}
