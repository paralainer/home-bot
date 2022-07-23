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
        buildString {
            append(status.name.format().padEnd(2))
            if (status != DownloadStatus.Finished) {
                append("${percentage.toInt().toString().padStart(4)}%")
            }

            append(name.trim().take(15).padEnd(16).padStart(1))

            if (status == DownloadStatus.InProgress ||
                status is DownloadStatus.Unknown
            ) {
                append(eta.format())
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
}
