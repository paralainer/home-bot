package com.paralainer.homebot.fastmile

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "fastmile")
class FastmileConfig(
    val username: String,
    val password: String,
    val baseUrl: String
)
