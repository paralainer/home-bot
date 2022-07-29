package com.paralainer.homebot.telegram.status

import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ParseMode
import com.paralainer.homebot.config.Config
import com.paralainer.homebot.iot.CapabilitiesStatusService
import com.paralainer.homebot.iot.CapabilityStatus
import com.paralainer.homebot.iot.CapabilityStatus.Openable
import com.paralainer.homebot.telegram.chatId
import com.paralainer.homebot.telegram.withTypingJob
import com.paralainer.homebot.torrent.DownloadItem
import com.paralainer.homebot.torrent.DownloadStatus
import java.time.Duration

class StatusHandler(
    private val capabilitiesStatusService: CapabilitiesStatusService,
    private val config: Config
) {

    suspend fun status(env: CommandHandlerEnvironment) {
        val status = withTypingJob(env) {
            fetchStatus()
        }

        env.bot.sendMessage(
            env.message.chatId(),
            status.statuses.joinToString("\n\n") { group ->
                group.joinToString("\n") { "`$it`" }
            },
            parseMode = ParseMode.MARKDOWN_V2,
        )
    }

    private suspend fun fetchStatus(): Status {
        val statuses = capabilitiesStatusService.getCapabilitiesStatus().associateBy { it.capabilityId }

        val formattedStatuses = config.status.map { group ->
            group.items.map { itemConfig ->
                buildString {
                    if (itemConfig.text != null) {
                        append(itemConfig.text).append(" ")
                    }

                    append(
                        itemConfig.capabilities.mapNotNull { capabilityId ->
                            val status = statuses[capabilityId]
                            if (status == null) {
                                println("Status for capability $capabilityId not found, skipping")
                                null
                            } else {
                                renderCapability(status)
                            }
                        }.joinToString(" ")
                    )
                }
            }
        }

        return Status(formattedStatuses)
    }

    private fun renderCapability(status: CapabilityStatus): String =
        when (status) {
            is CapabilityStatus.Blinds ->
                when (status.state) {
                    Openable.Open -> "🌅"
                    Openable.Closed -> "🌌"
                }
            is CapabilityStatus.ClimateSensor ->
                "${status.temperature.toInt()}° ${status.humidity.toInt()}%"
            is CapabilityStatus.DownloadsStatus ->
                status.downloads.joinToString("\n") { it.format() }
            is CapabilityStatus.RouterStatus ->
                status.status
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
        val statuses: List<List<String>>
    )
}
