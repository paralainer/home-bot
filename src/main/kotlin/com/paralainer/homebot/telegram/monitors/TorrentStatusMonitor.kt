package com.paralainer.homebot.telegram.monitors

import com.github.kotlintelegrambot.entities.ChatId
import com.paralainer.homebot.telegram.TelegramBot
import com.paralainer.homebot.telegram.TelegramConfig
import com.paralainer.homebot.torrent.TorrentEvent
import com.paralainer.homebot.torrent.TorrentStatusTracker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class TorrentStatusMonitor(
    telegramBot: TelegramBot,
    private val config: TelegramConfig,
    private val torrentStatusTracker: TorrentStatusTracker
) {

    private val bot = telegramBot.bot

    @PostConstruct
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
                is TorrentEvent.Finished ->
                    bot.sendMessage(
                        ChatId.fromId(config.notificationUser),
                        """
                                Download finished:
                                ${event.name}    
                                """.trimIndent()
                    )
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

}
