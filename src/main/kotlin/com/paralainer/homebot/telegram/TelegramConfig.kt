package com.paralainer.homebot.telegram

class TelegramConfig(
    val token: String = System.getenv("TELEGRAM_TOKEN"),
    allowedUsers: String = System.getenv("TELEGRAM_ALLOWED_USERS"),
) {
    val allowedUsersList: Set<Long> = allowedUsers.split(",")
        .filter { it.isNotBlank() }.map { it.trim().toLong() }.toSet()

    val notificationUser: Long = allowedUsersList.first() // TODO move to config
}
