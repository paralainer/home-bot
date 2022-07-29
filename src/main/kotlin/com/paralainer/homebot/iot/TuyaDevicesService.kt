package com.paralainer.homebot.iot

import com.paralainer.homebot.config.CapabilityProvider
import com.paralainer.homebot.config.CapabilityType
import com.paralainer.homebot.config.Config
import com.paralainer.homebot.iot.CapabilityStatus.Openable
import com.paralainer.homebot.tuya.TuyaCloudClient
import com.paralainer.homebot.tuya.TuyaDeviceStatus

class TuyaDevicesService(
    config: Config,
    private val tuyaCloudClient: TuyaCloudClient
) {

    private val devices: Map<String, TuyaDeviceType> = config.capabilities.mapNotNull {
        if (it.provider == CapabilityProvider.Tuya) {
            it.id to it.type.let(TuyaDeviceType::fromCapabilityType)
        } else null
    }.toMap()

    suspend fun getDevicesStatus(devices: List<String>): List<CapabilityStatus> =
        devices.toList().chunked(20).flatMap { batch ->
            val statuses = tuyaCloudClient.getDevicesStatus(batch)
            if (statuses.result == null) {
                throw Exception("Failed to fetch statues for devices")
            }

            statuses.result.map {
                deviceStatus(it.id, it.status)
            }
        }

    suspend fun getDeviceStatus(deviceId: String, type: TuyaDeviceType): CapabilityStatus {
        val status = tuyaCloudClient.getDeviceStatus(deviceId)
        if (status.result == null) {
            throw Exception("Failed to fetch status for device $deviceId")
        }

        return deviceStatus(deviceId, status.result)
    }

    private fun deviceStatus(
        deviceId: String,
        status: List<TuyaDeviceStatus.Item>
    ): CapabilityStatus {
        return when (devices[deviceId]) {
            TuyaDeviceType.ClimateSensor -> readClimateSensor(deviceId, status)
            TuyaDeviceType.BlindsRoller -> readBlindsState(deviceId, status)
            null -> throw Exception("Unexpected device id $deviceId")
        }
    }

    private fun readClimateSensor(
        deviceId: String,
        result: List<TuyaDeviceStatus.Item>
    ): CapabilityStatus.ClimateSensor {
        val humidity = result.find { it.code == "va_humidity" }?.value as? Double
            ?: throw Exception("Failed to read humidity for device  $deviceId")

        val tempF = result.find { it.code == "va_temperature" }?.value as? Double
            ?: throw Exception("Failed to read temperature for device  $deviceId")

        return CapabilityStatus.ClimateSensor(
            temperature = tempF.toCelcius(),
            humidity = humidity,
            capabilityId = deviceId
        )
    }

    private fun readBlindsState(deviceId: String, result: List<TuyaDeviceStatus.Item>): CapabilityStatus.Blinds {
        val percent = result.find { it.code == "percent_state" }?.value as? Double
            ?: throw Exception("Failed to read blinds state for device  $deviceId")

        val state = if (percent > 80) Openable.Closed else Openable.Open

        return CapabilityStatus.Blinds(
            state = state,
            capabilityId = deviceId
        )
    }


    private fun Double.toCelcius(): Double = (this - 32.0) * (5.0 / 9.0)

}

enum class TuyaDeviceType {
    ClimateSensor,
    BlindsRoller;

    companion object {
        fun fromCapabilityType(capabilityType: CapabilityType): TuyaDeviceType =
            when (capabilityType) {
                CapabilityType.ClimateSensor -> ClimateSensor
                CapabilityType.BlindsControl -> BlindsRoller
                else -> throw Exception("Capability '${capabilityType.value} is not supported by Tuya")
            }
    }
}

