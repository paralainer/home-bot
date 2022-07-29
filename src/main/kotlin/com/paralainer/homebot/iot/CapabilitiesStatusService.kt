package com.paralainer.homebot.iot

import com.paralainer.homebot.config.CapabilityConfig
import com.paralainer.homebot.config.Config
import com.paralainer.homebot.config.CapabilityProvider
import com.paralainer.homebot.config.CapabilityType
import com.paralainer.homebot.fastmile.FastmileService
import com.paralainer.homebot.torrent.DownloadItem
import com.paralainer.homebot.torrent.TorrentService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CapabilitiesStatusService(
    private val config: Config,
    private val tuyaDevicesService: TuyaDevicesService,
    private val fastmileService: FastmileService,
    private val utorrentService: TorrentService
) {
    suspend fun getCapabilitiesStatus(): List<CapabilityStatus> = coroutineScope {
        val groupedCapabilities = config.capabilities.groupBy { it.provider }

        groupedCapabilities.map { (provider, capabilities) ->
            when (provider) {
                CapabilityProvider.Tuya -> async {
                    tuyaDevicesService.getDevicesStatus(
                        capabilities.map { it.id }
                    )
                }
                CapabilityProvider.Fastmile -> async {
                    fastmileStatus(capabilities)
                }
                CapabilityProvider.UTorrent -> async {
                    utorrentStatus(capabilities)
                }
            }
        }.awaitAll().flatten()
    }


    private suspend fun fastmileStatus(capabilities: List<CapabilityConfig>): List<CapabilityStatus> =
        capabilities.map {
            when (it.type) {
                CapabilityType.Router -> {
                    val is5gUp = fastmileService.is5GUp()

                    CapabilityStatus.RouterStatus(
                        "5G " + if (is5gUp) "up" else "down",
                        it.id
                    )
                }
                else -> throw Exception("${it.type.value} is not supported for fastmile provider")
            }
        }

    private suspend fun utorrentStatus(capabilities: List<CapabilityConfig>): List<CapabilityStatus> =
        capabilities.map {
            when (it.type) {
                CapabilityType.Torrent -> {
                    val downloads = utorrentService.listDownloads()

                    CapabilityStatus.DownloadsStatus(
                        downloads,
                        it.id
                    )
                }
                else -> throw Exception("${it.type.value} is not supported for utorrent provider")
            }
        }

}

sealed interface CapabilityStatus {
    val capabilityId: String

    data class ClimateSensor(val temperature: Double, val humidity: Double, override val capabilityId: String) :
        CapabilityStatus

    data class Blinds(val state: Openable, override val capabilityId: String) : CapabilityStatus

    data class RouterStatus(val status: String, override val capabilityId: String) : CapabilityStatus

    data class DownloadsStatus(val downloads: List<DownloadItem>, override val capabilityId: String) : CapabilityStatus

    enum class Openable {
        Open,
        Closed
    }

    enum class Switchable {
        On,
        Off
    }
}
