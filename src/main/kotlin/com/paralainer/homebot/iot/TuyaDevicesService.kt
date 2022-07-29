package com.paralainer.homebot.iot

import com.paralainer.homebot.config.DeviceType
import com.paralainer.homebot.iot.DeviceStatus.BlindsState
import com.paralainer.homebot.tuya.TuyaCloudClient
import com.paralainer.homebot.tuya.TuyaDeviceStatus

class TuyaDevicesService(
    private val tuyaCloudClient: TuyaCloudClient
) {
    suspend fun getDevicesStatus(devices: Map<String, DeviceType>): List<DeviceStatus> =
        devices.toList().chunked(20).flatMap { batch ->
            val statuses = tuyaCloudClient.getDevicesStatus(batch.map { it.first })
            if (statuses.result == null) {
                throw Exception("Failed to fetch statues for devices")
            }

            statuses.result.map {
                val type = devices[it.id] ?: throw Exception("Unexpected device returned ${it.id}")
                deviceStatus(type, it.id, it.status)
            }
        }

    suspend fun getDeviceStatus(deviceId: String, type: DeviceType): DeviceStatus {
        val status = tuyaCloudClient.getDeviceStatus(deviceId)
        if (status.result == null) {
            throw Exception("Failed to fetch status for device $deviceId")
        }

        return deviceStatus(type, deviceId, status.result)
    }

    private fun deviceStatus(
        type: DeviceType,
        deviceId: String,
        status: List<TuyaDeviceStatus.Item>
    ): DeviceStatus {
        return when (type) {
            DeviceType.ClimateSensor -> readClimateSensor(deviceId, status)
            DeviceType.BlindsControl -> readBlindsState(deviceId, status)
        }
    }

    private fun readClimateSensor(deviceId: String, result: List<TuyaDeviceStatus.Item>): DeviceStatus.ClimateSensor {
        val humidity = result.find { it.code == "va_humidity" }?.value as? Double
            ?: throw Exception("Failed to read humidity for device  $deviceId")

        val tempF = result.find { it.code == "va_temperature" }?.value as? Double
            ?: throw Exception("Failed to read temperature for device  $deviceId")

        return DeviceStatus.ClimateSensor(
            temperature = tempF.toCelcius(),
            humidity = humidity,
            deviceId = deviceId
        )
    }

    private fun readBlindsState(deviceId: String, result: List<TuyaDeviceStatus.Item>): DeviceStatus.Blinds {
        val percent = result.find { it.code == "percent_state" }?.value as? Double
            ?: throw Exception("Failed to read blinds state for device  $deviceId")

        val state = if (percent > 80) BlindsState.Closed else BlindsState.Open

        return DeviceStatus.Blinds(
            state = state,
            deviceId = deviceId
        )
    }


    private fun Double.toCelcius(): Double = (this - 32.0) * (5.0 / 9.0)

}

