package com.paralainer.homebot.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import org.springframework.stereotype.Component


@Component
class TelegramBot(
    private val config: TelegramConfig,
    private val telegramRouter: TelegramRouter
) {
    val bot: Bot = initBot()

    private fun initBot(): Bot {
        val botInstance = bot {
            token = config.token

            dispatch {
                telegramRouter.registerRoutes(this)
            }
        }

        botInstance.startPolling()

        println("Telegram bot started")

        return botInstance
    }
}
