package com.paralainer.homebot.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch


class TelegramBot(
    private val config: TelegramConfig,
    private val telegramRouter: TelegramRouter
) {
    val bot: Bot by lazy { initBot() }

    fun start() {
        val me = bot.getMe()
        me.fold(
            ifSuccess = { println("Started bot: ${it.id}") },
            ifError = { println("Failed to start bot") }
        )
    }

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
