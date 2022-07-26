package com.paralainer.homebot.speedtest

import com.paralainer.homebot.common.apiWebClient
import io.ktor.client.request.*

class RestSpeedtest : SpeedtestService {
    override suspend fun measureSpeed(): SpeedtestResult {
        val result = apiWebClient().get<MeasuringResult>("http://speedtest:8000/speedtest")

        return SpeedtestResult(
            downloadSpeedMbps = result.download.toDouble(),
            uploadSpeedMbps = result.upload.toDouble(),
            pingMs = result.ping.toDouble()
        )
    }

    private data class MeasuringResult(val download: String, val upload: String, val ping: String)
}
