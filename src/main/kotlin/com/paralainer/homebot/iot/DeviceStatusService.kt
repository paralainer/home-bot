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
    suspend fun getDevicesStatus(): List<DeviceStatus> = coroutineScope {
        val groupedDevices = devicesConfig.devices.groupBy { it.provider }

        groupedDevices.map { (provider, devices) ->
            when (provider) {
                DeviceProvider.Tuya -> async {
                    tuyaDevicesService.getDevicesStatus(
                        devices.associate { it.id to it.type }
                    )
                }
            }
        }.awaitAll().flatten()
    }
}

sealed interface DeviceStatus {
    val deviceId: String

    data class ClimateSensor(val temperature: Double, val humidity: Double, override val deviceId: String) :
        DeviceStatus

    data class Blinds(val state: BlindsState, override val deviceId: String) : DeviceStatus

    enum class BlindsState {
        Open,
        Closed
    }
}
