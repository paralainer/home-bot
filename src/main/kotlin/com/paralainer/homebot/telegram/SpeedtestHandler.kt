package com.paralainer.homebot.telegram

import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatAction
import com.paralainer.homebot.speedtest.SpeedtestService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component

@Component
class SpeedtestHandler(
    private val speedtestService: SpeedtestService
) {
    suspend fun measureSpeedCommand(env: CommandHandlerEnvironment) {
        val chatId = env.message.chatId()

        try {
            coroutineScope {
                val typingJob = launch {
                    while (isActive) {
                        env.bot.sendChatAction(chatId, ChatAction.TYPING)
                        delay(5000)
                    }
                }

                val result = speedtestService.measureSpeed()

                typingJob.cancel()

                env.bot.sendMessage(
                    chatId,
                    text = "Speed: ${formatSpeed(result.speedMps)} Mbps"
                )
            }
        } catch (ex: Exception) {
            env.bot.sendMessage(
                chatId,
                text = "Measuring failed"
            )

            throw ex
        }
    }

    private fun formatSpeed(speed: Double): String =
        "%.${2}f".format(speed)
}
