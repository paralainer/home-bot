package com.paralainer.homebot.telegram.status

import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ParseMode
import com.paralainer.homebot.config.Config
import com.paralainer.homebot.fastmile.FastmileService
import com.paralainer.homebot.iot.DeviceStatus
import com.paralainer.homebot.iot.DeviceStatusService
import com.paralainer.homebot.telegram.chatId
import com.paralainer.homebot.telegram.withTypingJob
import com.paralainer.homebot.torrent.DownloadItem
import com.paralainer.homebot.torrent.DownloadStatus
import com.paralainer.homebot.torrent.TorrentService
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import java.time.Duration

class StatusHandler(
    private val torrentService: TorrentService,
    private val routerService: FastmileService,
    private val deviceStatusService: DeviceStatusService,
    private val config: Config
) {

    suspend fun status(env: CommandHandlerEnvironment) {
        val result = withTypingJob(env) {
            fetchStatus()
        }

        val downloads = result.downloadsList.joinToString("\n") { it.format() }
        env.bot.sendMessage(
            env.message.chatId(),
            """
`📶 5G ${result.status5g.value}`  
```
${result.devices}
```               
```
$downloads
```
""".trimIndent(),
            ParseMode.MARKDOWN_V2
        )
    }

    private suspend fun fetchStatus(): Status =
        supervisorScope {
            val downloadsListJob = async { torrentService.listDownloads() }
            val is5gUpJob = async { routerService.is5GUp() }
            val devicesStatusJob = async { deviceStatusService.getDevicesStatus() }

            val status5g = getStatus5g(is5gUpJob)
            val downloadsList = getDownloadsList(downloadsListJob)
            val devicesStatus = getDevicesStatus(devicesStatusJob)

            Status(status5g, downloadsList, devicesStatus)
        }

    private suspend fun getDownloadsList(downloadsListJob: Deferred<List<DownloadItem>>): List<DownloadItem> =
        runCatching { downloadsListJob.await() }.getOrElse {
            it.printStackTrace()
            emptyList()
        }


    private suspend fun getDevicesStatus(devicesStatusJob: Deferred<List<DeviceStatus>>): String =
        runCatching {
            val statuses = devicesStatusJob.await().associateBy { it.deviceId }
            config.ui.rooms.joinToString("\n") { room ->
                room.icon + " " +
                    room.deviceStatuses.mapNotNull {
                        statuses[it]?.asString()
                    }.joinToString(" ")
            }
        }.getOrElse {
            it.printStackTrace()
            ""
        }


    private fun DeviceStatus.asString(): String =
        when (this) {
            is DeviceStatus.ClimateSensor ->
                "${temperature.toInt()}° ${humidity.toInt()}%"
            is DeviceStatus.BlindsState ->
                "🌆 $state"
        }

    private suspend fun getStatus5g(is5gUpJob: Deferred<Boolean>): Status5g =
        runCatching {
            val is5gUp = is5gUpJob.await()

            if (is5gUp) Status5g.Up
            else Status5g.Down
        }.getOrElse {
            it.printStackTrace()
            Status5g.Error
        }

    private fun DownloadItem.format(): String =
        buildString {
            append(status.format())
            if (status != DownloadStatus.Finished) {
                append(" ").append("${percentage.toInt()}%")
            }

            append(" ").append(name.trim().take(15))

            if (status == DownloadStatus.InProgress ||
                status is DownloadStatus.Unknown
            ) {
                append(" ").append(eta.format())
            }
        }

    private fun Duration.format(): String {
        val hours = toHours()
        return buildString {
            if (hours > 0) {
                append(hours).append("h")
            }
            append(toMinutesPart()).append("m")
        }
    }

    private fun DownloadStatus.format(): String =
        when (this) {
            is DownloadStatus.Failed -> "❌"
            DownloadStatus.Finished -> "✅"
            DownloadStatus.InProgress -> "🔄"
            DownloadStatus.Pause -> "⏸"
            is DownloadStatus.Unknown -> "❔"
        }


    private data class Status(
        val status5g: Status5g,
        val downloadsList: List<DownloadItem>,
        val devices: String
    )

    private enum class Status5g(val value: String) {
        Up("up"),
        Down("down"),
        Error("error")
    }
}
