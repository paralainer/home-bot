package com.paralainer.homebot.telegram

import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ParseMode
import com.paralainer.homebot.torrent.DownloadItem
import com.paralainer.homebot.torrent.DownloadStatus
import com.paralainer.homebot.torrent.TorrentService
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class StatusHandler(
    private val torrentService: TorrentService
) {

    suspend fun status(env: CommandHandlerEnvironment) {
        val result = withTypingJob(env) {
            torrentService.listDownloads()
        }

        val downloads = result.joinToString("\n") { it.format() }
        env.bot.sendMessage(
            env.message.chatId(),
            """
```
$downloads
```
""".trimIndent(),
            ParseMode.MARKDOWN_V2
        )
    }


    private fun DownloadItem.format(): String =
        name.trim().take(10).padEnd(10) +
            "${percentage.toInt().toString().padStart(4)}% " +
            status.name.padEnd(12) +
            (eta.format().takeIf {
                status == DownloadStatus.InProgress ||
                    status is DownloadStatus.Unknown
            } ?: "")

    private fun Duration.format(): String {
        val hours = toHours()
        return buildString {
            if (hours > 0) {
                append(hours).append("h")
            }
            append(toMinutesPart()).append("m")
        }
    }

}
