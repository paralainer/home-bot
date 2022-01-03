package com.paralainer.homebot.speedtest

import com.paralainer.homebot.common.apiWebClient
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.awaitBody

@Service
class RestSpeedtest : SpeedtestService {
    override suspend fun measureSpeed(): SpeedtestResult {
        val result = apiWebClient().get().uri("http://speedtest:8000/speedtest")
            .retrieve().awaitBody<MeasuringResult>()

        return SpeedtestResult(
            downloadSpeedMbps = result.download.toDouble(),
            uploadSpeedMbps = result.upload.toDouble(),
            pingMs = result.ping.toDouble()
        )
    }

    private data class MeasuringResult(val download: String, val upload: String, val ping: String)
}
