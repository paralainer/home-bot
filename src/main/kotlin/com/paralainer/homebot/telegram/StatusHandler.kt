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
}
