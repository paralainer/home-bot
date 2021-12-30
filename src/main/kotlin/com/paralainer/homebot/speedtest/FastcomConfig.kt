package com.paralainer.homebot.speedtest

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "fastcom")
@ConstructorBinding
class FastcomConfig(
   val token: String
)
