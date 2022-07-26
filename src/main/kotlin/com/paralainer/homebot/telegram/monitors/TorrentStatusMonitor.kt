package com.paralainer.homebot.telegram.monitors

import com.github.kotlintelegrambot.entities.ChatId
import com.paralainer.homebot.telegram.TelegramBot
import com.paralainer.homebot.telegram.TelegramConfig
import com.paralainer.homebot.torrent.TorrentEvent
import com.paralainer.homebot.torrent.TorrentService
import com.paralainer.homebot.torrent.TorrentStatusTracker
import com.paralainer.homebot.torrent.UTorrentService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.time.toKotlinDuration

class TorrentStatusMonitor(
    telegramBot: TelegramBot,
    private val config: TelegramConfig,
    private val torrentStatusTracker: TorrentStatusTracker,
    private val torrentService: TorrentService
) {

    private val bot = telegramBot.bot

    fun start() = GlobalScope.launch {
        torrentStatusTracker.observeEvents().collect { event ->
            println("Event: $event")
            when (event) {
                is TorrentEvent.Error ->
                    bot.sendMessage(
                        ChatId.fromId(config.notificationUser),
                        """
                                Download error:
                                ${event.name} 
                                ${event.error}   
                                """.trimIndent()
                    )
                is TorrentEvent.Finished -> {
                    bot.sendMessage(
                        ChatId.fromId(config.notificationUser),
                        """
                                Download finished:
                                ${event.name}    
                                """.trimIndent()
                    )

                    cleanupTorrent(event.hash)
                }
                is TorrentEvent.Started ->
                    bot.sendMessage(
                        ChatId.fromId(config.notificationUser),
                        """
                                Download started:
                                ${event.name}    
                                """.trimIndent()
                    )
            }
        }
    }


    private fun CoroutineScope.cleanupTorrent(hash: String) =
        launch {
            delay(Duration.ofMinutes(1).toKotlinDuration())
            runCatching {
                torrentService.removeTorrent(hash)
            }.onFailure {
                it.printStackTrace()
            }
        }

}
