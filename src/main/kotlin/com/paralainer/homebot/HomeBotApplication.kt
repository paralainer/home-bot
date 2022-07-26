package com.paralainer.homebot

import com.paralainer.homebot.fastmile.*
import com.paralainer.homebot.speedtest.*
import com.paralainer.homebot.telegram.*
import com.paralainer.homebot.telegram.monitors.*
import com.paralainer.homebot.torrent.*
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun main(args: Array<String>) {
    val app = configureDeps()
    app.koin.get<TelegramBot>().start()
    app.koin.get<TorrentStatusMonitor>().start()
}

private fun configureDeps() = startKoin {
    printLogger()
    modules(
        module {
            single { FastmileConfig() }
            singleOf(::FastmileAuthenticator)
            singleOf(::FastmileClient)
            singleOf(::FastmileService)

            singleOf(::RestSpeedtest) { bind<SpeedtestService>() }

            single { TelegramConfig() }
            singleOf(::DownloadHandler)
            singleOf(::RouterHandler)
            singleOf(::SpeedtestHandler)
            singleOf(::StatusHandler)
            singleOf(::TelegramRouter)
            singleOf(::TelegramBot)
            singleOf(::TorrentStatusMonitor)

            single { UTorrentConfig() }
            singleOf(::UTorrentClient)
            singleOf(::UTorrentService) { bind<TorrentService>() }
            singleOf(::TorrentStatusTracker)
        }
    )
}
