package com.paralainer.homebot.torrent

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "utorrent")
class UTorrentConfig(
    val baseUrl: String,
    val username: String,
    val password: String
)
