package com.paralainer.homebot.telegram

import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.paralainer.homebot.torrent.TorrentService
import org.springframework.stereotype.Component

@Component
class StatusHandler(
    private val torrentService: TorrentService
) {

    suspend fun status(env: CommandHandlerEnvironment) {
        val result = withTypingJob(env) {
            torrentService.listDownloads()
        }

        env.bot.sendMessage(
            env.message.chatId(),
            result.joinToString("\n") { "${it.name} ${String.format("%.2d", it.percentage)}% ${it.status}" }
        )
    }
}
