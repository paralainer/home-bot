package com.paralainer.homebot.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Update
import com.paralainer.homebot.torrent.TorrentEvent
import com.paralainer.homebot.torrent.TorrentService
import com.paralainer.homebot.torrent.TorrentStatusTracker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import javax.annotation.PostConstruct

@Component
class TelegramRouter(
    private val config: TelegramConfig,
    private val speedtestHandler: SpeedtestHandler,
    private val statusHandler: StatusHandler,
    private val torrentStatusTracker: TorrentStatusTracker
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun start() {
        val botInstance = bot {
            token = config.token

            dispatch {
                addHandler(AllowedUsersHandler())

                command("speedtest") {
                    handleAsync(update = update) { speedtestHandler.measureSpeedCommand(this) }
                }

                command("status") {
                    handleAsync(update = update) { statusHandler.status(this) }
                }

                telegramError {
                    logger.warn("Telegram error: ${error.getErrorMessage()}")
                }
            }
        }

        botInstance.startPolling()

        GlobalScope.launch {
            torrentStatusTracker.observeEvents().collect { event ->
                when (event) {
                    is TorrentEvent.Error ->
                        botInstance.sendMessage(
                            ChatId.fromId(config.notificationUser),
                            """
                            Download error:
                            ${event.name} 
                            ${event.error}   
                            """.trimIndent()
                        )
                    is TorrentEvent.Finished ->
                        botInstance.sendMessage(
                            ChatId.fromId(config.notificationUser),
                            """
                            Download finished:
                            ${event.name}    
                            """.trimIndent()
                        )
                    is TorrentEvent.Started ->
                        botInstance.sendMessage(
                            ChatId.fromId(config.notificationUser),
                            """
                            Download started:
                            ${event.name}    
                            """.trimIndent()
                        )
                }
            }
        }

        logger.info("Telegram bot started")
    }

    private inner class AllowedUsersHandler : Handler {
        override fun checkUpdate(update: Update): Boolean = true

        override fun handleUpdate(bot: Bot, update: Update) {
            val from = update.from()
            val senderId = from?.id
            if (!config.allowedUsersList.contains(senderId)) {
                logger.info("Telegram update from non allowed user ${from?.asString()}. Update: ${update.asString()}")
                update.consume()
            } else {
                logger.info("Telegram update from ${from?.asString()}. Update: ${update.asString()}")
            }
        }
    }


    private fun handleAsync(timeout: Duration = Duration.ofMinutes(1), update: Update, block: suspend () -> Any?) {
        handleAsync(timeout, block)
        update.consume()
    }

    private fun handleAsync(timeout: Duration = Duration.ofMinutes(1), block: suspend () -> Any?) {
        GlobalScope.launch {
            try {
                withTimeout(timeout.toMillis()) {
                    block()
                }
            } catch (ex: Exception) {
                logger.warn("Async task failed", ex)
            }
        }
    }
}
