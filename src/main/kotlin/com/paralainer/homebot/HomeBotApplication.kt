package com.paralainer.homebot

import com.paralainer.homebot.speedtest.FastcomConfig
import com.paralainer.homebot.speedtest.FastcomSpeedtest
import com.paralainer.homebot.telegram.TelegramConfig
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.getBean
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
    FastcomConfig::class
)
class HomeBotApplicationConfiguration {

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}

fun main(args: Array<String>) {
    runApplication<HomeBotApplication>(*args)
}
