package com.paralainer.homebot.telegram

import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ParseMode
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
                    text = """
                        ```
                        Speed:
                          Down: ${formatSpeed(result.downloadSpeedMbps)} Mbps
                          Up: ${formatSpeed(result.uploadSpeedMbps)} Mbps
                          Ping: ${formatSpeed(result.pingMs)} ms
                        ```  
                    """.trimIndent(),
                    parseMode = ParseMode.MARKDOWN_V2
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
