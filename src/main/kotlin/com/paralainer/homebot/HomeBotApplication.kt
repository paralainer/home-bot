package com.paralainer.homebot

import com.paralainer.homebot.telegram.TelegramConfig
import com.paralainer.homebot.torrent.UTorrentClient
import com.paralainer.homebot.torrent.UTorrentConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.EnableWebFlux
import java.time.Clock

@SpringBootApplication
@EnableWebFlux
class HomeBotApplication

@Configuration
@EnableConfigurationProperties(
    TelegramConfig::class,
    UTorrentConfig::class
)
class HomeBotApplicationConfiguration {

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}

fun main(args: Array<String>) {
    runApplication<HomeBotApplication>(*args)
}
