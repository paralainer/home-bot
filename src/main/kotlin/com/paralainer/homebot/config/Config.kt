package com.paralainer.homebot.config

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.yaml.snakeyaml.Yaml
import java.io.File

class Config(
    val capabilities: List<CapabilityConfig>,
    val status: List<StatusGroup>
) {
    companion object {
        fun fromYaml(file: String): Config {
            val objectMapper = ObjectMapper().registerKotlinModule()
            return objectMapper.readValue(
                objectMapper.writeValueAsString(
                    Yaml().load<Map<String, Any>>(File(file).inputStream())
                ),
                Config::class.java
            )
        }
    }
}

data class StatusGroup(
    val group: String,
    val items: List<ItemConfig>
) {
    data class ItemConfig(
        val text: String?,
        val capabilities: List<String>
    )
}

data class CapabilityConfig(
    val provider: CapabilityProvider,
    val type: CapabilityType,
    val id: String
)

enum class CapabilityProvider(val value: String) {
    Tuya("tuya"),
    Fastmile("fastmile"),
    UTorrent("utorrent");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(string: String): CapabilityProvider =
            values().find { it.value == string } ?: throw Exception("Unknown capability provider $string")
    }
}

enum class CapabilityType(val value: String) {
    ClimateSensor("climate-sensor"),
    BlindsControl("blinds-control"),
    Router("router"),
    Torrent("torrent");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(string: String): CapabilityType =
            CapabilityType.values().find { it.value == string } ?: throw Exception("Unknown capability type $string")
    }
}
