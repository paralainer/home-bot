package com.paralainer.homebot.iot

import com.paralainer.homebot.config.DeviceType
import com.paralainer.homebot.tuya.TuyaCloudClient
import com.paralainer.homebot.tuya.TuyaDeviceStatus

class TuyaDevicesService(
    private val tuyaCloudClient: TuyaCloudClient
) {

    suspend fun getDeviceStatus(deviceId: String, type: DeviceType): DeviceStatus {
        val status = tuyaCloudClient.getDeviceStatus(deviceId)
        if (status.result == null) {
            throw Exception("Failed to fetch status for device $deviceId")
        }

        return when (type) {
            DeviceType.ClimateSensor -> readClimateSensor(deviceId, status.result)
            DeviceType.BlindsControl -> readBlindsState(deviceId, status.result)
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

    private fun readBlindsState(deviceId: String, result: List<TuyaDeviceStatus.Item>): DeviceStatus.BlindsState {
        val percent = result.find { it.code == "percent_state" }?.value as? Double
            ?: throw Exception("Failed to read blinds state for device  $deviceId")

        val state = if (percent > 80) "closed" else "open"

        return DeviceStatus.BlindsState(
            state = state,
            deviceId = deviceId
        )
    }


    private fun Double.toCelcius(): Double = (this - 32.0) * (5.0 / 9.0)

}

