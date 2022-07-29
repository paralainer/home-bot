package com.paralainer.homebot.config

import org.yaml.snakeyaml.Yaml
import java.io.File

class Config(
    val devices: List<DeviceConfig>,
    val ui: UiConfig
) {
    companion object {
        fun fromYaml(file: String): Config {
            val contents = Yaml().load<Map<String, Any>>(File(file).inputStream())

            return Config(
                devicesConfigs(contents),
                uiConfig(contents)
            )
        }

        private fun uiConfig(contents: Map<String, Any>): UiConfig {
            val uiConfig = contents.obj("ui")

            return UiConfig(
                uiConfig.list("rooms").map {
                    val roomConf = it.obj()
                    UiConfig.RoomConfig(
                        name = roomConf.string("name"),
                        icon = roomConf.string("icon"),
                        deviceStatuses = roomConf.list("device-statuses").map { s -> s as String }
                    )
                }
            )
        }


        private fun devicesConfigs(contents: Map<String, Any>) =
            contents.list("devices").map {
                val device = it.obj()
                DeviceConfig(
                    device.string("provider").let(DeviceProvider::fromString),
                    device.string("type").let(DeviceType::fromString),
                    device.string("id")
                )
            }

        private fun Map<String, Any>.obj(key: String): Map<String, Any> {
            val value = this[key] ?: throw Exception("missing key $key")
            return value as? Map<String, Any> ?: throw Exception("$key should be an object")
        }

        private fun Map<String, Any>.string(key: String): String {
            val value = this[key] ?: throw Exception("missing key $key")
            return value as? String ?: throw Exception("$key should be a string")
        }

        private fun Map<String, Any>.list(key: String): List<Any> {
            val value = this[key] ?: throw Exception("missing key $key")
            return value as? List<Any> ?: throw Exception("$key should be a list")
        }

        private fun Any.obj() =
            this as? Map<String, Any> ?: throw Exception("should be an object")
    }
}

data class UiConfig(
    val rooms: List<RoomConfig>
) {
    data class RoomConfig(
        val name: String,
        val icon: String,
        val deviceStatuses: List<String>
    )
}

data class DeviceConfig(
    val provider: DeviceProvider,
    val type: DeviceType,
    val id: String
)

enum class DeviceProvider(val value: String) {
    Tuya("tuya");

    companion object {
        fun fromString(value: String): DeviceProvider =
            values().find { it.value == value } ?: throw Exception("Unknown device provider $value")
    }
}

enum class DeviceType(val value: String) {
    ClimateSensor("climate-sensor"),
    BlindsControl("blinds-control");

    companion object {
        fun fromString(value: String): DeviceType =
            DeviceType.values().find { it.value == value } ?: throw Exception("Unknown device type $value")
    }
}
