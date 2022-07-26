package com.paralainer.homebot.telegram

import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ParseMode
import com.paralainer.homebot.speedtest.RestSpeedtest
import com.paralainer.homebot.speedtest.SpeedtestService

class SpeedtestHandler(
    private val speedtestService: SpeedtestService
) {
    suspend fun measureSpeedCommand(env: CommandHandlerEnvironment) {
        val chatId = env.message.chatId()

        try {
            val result = withTypingJob(env) {
                speedtestService.measureSpeed()
            }
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
