package com.paralainer.homebot.telegram

import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.paralainer.homebot.fastmile.FastmileService

class RouterHandler(
    private val routerService: FastmileService
) {
    suspend fun restartRouter(env: CommandHandlerEnvironment) {
        withTypingJob(env) {
            runCatching {
                routerService.reboot()
                env.bot.sendMessage(env.message.chatId(), "Router restarted")
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
}
