package com.paralainer.homebot

import com.paralainer.homebot.config.Config
import com.paralainer.homebot.iot.*
import com.paralainer.homebot.fastmile.FastmileAuthenticator
import com.paralainer.homebot.fastmile.FastmileClient
import com.paralainer.homebot.fastmile.FastmileConfig
import com.paralainer.homebot.fastmile.FastmileService
import com.paralainer.homebot.speedtest.RestSpeedtest
import com.paralainer.homebot.speedtest.SpeedtestService
import com.paralainer.homebot.telegram.*
import com.paralainer.homebot.telegram.monitors.TorrentStatusMonitor
import com.paralainer.homebot.telegram.status.StatusHandler
import com.paralainer.homebot.torrent.*
import com.paralainer.homebot.tuya.*
import org.koin.core.context.startKoin
import org.koin.core.module.Module
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
            readConfig()

            singleOf(::FastmileAuthenticator)
            singleOf(::FastmileClient)
            singleOf(::FastmileService)

            singleOf(::RestSpeedtest) { bind<SpeedtestService>() }

            singleOf(::DownloadHandler)
            singleOf(::RouterHandler)
            singleOf(::SpeedtestHandler)
            singleOf(::StatusHandler)
            singleOf(::TelegramRouter)
            singleOf(::TelegramBot)
            singleOf(::TorrentStatusMonitor)

            singleOf(::UTorrentClient)
            singleOf(::UTorrentService) { bind<TorrentService>() }
            singleOf(::TorrentStatusTracker)

            singleOf(::DeviceStatusService)
            singleOf(::TuyaDevicesService)

            singleOf(::TuyaCloudClient)
        }
    )
}

private fun Module.readConfig() {
    single { FastmileConfig() }
    single { TelegramConfig() }
    single { UTorrentConfig() }
    single {
        TuyaCloudConfig(
            "TUYA_CLIENT_ID".env(),
            "TUYA_CLIENT_SECRET".env(),
        )
    }

    single {
        Config.fromYaml("config.yaml")
    }
}

private fun String.env(): String =
    System.getenv(this) ?: throw Exception("Variable $this is not defined")
