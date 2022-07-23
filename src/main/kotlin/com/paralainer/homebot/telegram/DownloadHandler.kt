package com.paralainer.homebot.telegram

import com.github.kotlintelegrambot.dispatcher.handlers.TextHandlerEnvironment
import com.paralainer.homebot.torrent.TorrentService
import org.springframework.stereotype.Component
import java.net.URI

@Component
class DownloadHandler(
    private val torrentService: TorrentService
) {
    suspend fun addByUrl(env: TextHandlerEnvironment) {
        val url = runCatching { URI.create(env.text) }.getOrNull()
        if (url == null) {
            env.bot.sendMessage(env.message.chatId(), "Invalid magnet link")
            return
        }

        runCatching { torrentService.addByUrl(url) }
            .onFailure {
                it.printStackTrace()
                env.bot.sendMessage(env.message.chatId(), "Adding download failed")
            }

    }
}
