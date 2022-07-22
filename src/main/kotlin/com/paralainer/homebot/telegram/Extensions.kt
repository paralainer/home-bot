package com.paralainer.homebot.telegram

import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.Response

fun Chat.chatId(): ChatId = ChatId.fromId(id)
fun Message.chatId(): ChatId = chat.chatId()

fun User.asString() = "${username ?: firstName} ($id)"

fun Update.from(): User? =
    when {
        message != null -> message?.from
        editedMessage != null -> editedMessage?.from
        inlineQuery != null -> inlineQuery?.from
        chosenInlineResult != null -> chosenInlineResult?.from
        callbackQuery != null -> callbackQuery?.from
        shippingQuery != null -> shippingQuery?.from
        preCheckoutQuery != null -> preCheckoutQuery?.from
        editedChannelPost != null -> editedChannelPost?.from
        channelPost != null -> channelPost?.from
        pollAnswer != null -> pollAnswer?.user
        else -> null
    }

fun Update.asString(): String =
    when {
        message != null -> "message: ${message?.text}"
        editedMessage != null -> "editedMessage (${editedMessage?.messageId}): ${editedMessage?.text}"
        inlineQuery != null -> "inlineQuery: ${inlineQuery?.query}"
        chosenInlineResult != null -> "chosenInlineResult: ${chosenInlineResult?.resultId}"
        callbackQuery != null -> "callbackQuery: ${callbackQuery?.data}"
        shippingQuery != null -> "shippingQuery"
        preCheckoutQuery != null -> "preCheckoutQuery"
        editedChannelPost != null -> "editedChannelPost"
        channelPost != null -> "channelPost"
        pollAnswer != null -> "pollAnswer"
        else -> "<unknown>"
    }


fun Pair<Response<com.github.kotlintelegrambot.network.Response<Message>?>?, Exception?>.extractMessage(): Result<Message> {
    val (resp, err) = this
    if (err != null) {
        return Result.failure(err)
    }

    if (resp == null) {
        return Result.failure(RuntimeException("Message response is null"))
    }

    try {
        val body = resp.body()
            ?: return Result.failure(RuntimeException("Message body is null"))

        val msg = body.result
            ?: return Result.failure(RuntimeException("Message is null, error: ${body.errorDescription}"))

        return Result.success(msg)
    } catch (ex: Exception) {
        return Result.failure(ex)
    }
}

suspend fun <T> withTypingJob(env: CommandHandlerEnvironment, block: suspend () -> T): T =
    coroutineScope {
        val typingJob = launch {
            while (isActive) {
                env.bot.sendChatAction(env.message.chatId(), ChatAction.TYPING)
                delay(5000)
            }
        }

        val result = block()

        typingJob.cancel()

        result
    }
