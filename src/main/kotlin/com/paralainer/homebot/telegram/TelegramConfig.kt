package com.paralainer.homebot.telegram

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "telegram")
class TelegramConfig(
    val token: String,
    allowedUsers: String
) {
    val allowedUsersList: Set<Long> = allowedUsers.split(",")
        .filter { it.isNotBlank() }.map { it.trim().toLong() }.toSet()
}
