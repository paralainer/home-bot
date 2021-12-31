package com.paralainer.homebot.speedtest

import com.paralainer.homebot.common.apiWebClient
import com.paralainer.homebot.common.apiWebClientBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class FastcomSpeedtest(
    private val config: FastcomConfig,
    private val clock: Clock
) : SpeedtestService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val fileWebClient = apiWebClientBuilder().exchangeStrategies(
        ExchangeStrategies.builder()
            .codecs {
                it.defaultCodecs()
                    .maxInMemorySize(30 * 1024 * 1024)
            }
            .build()
    ).build()

    override suspend fun measureSpeed(): SpeedtestResult {
        val start = clock.instant()
        val avg = fetchTargets().mapNotNull {
            if (Duration.between(start, clock.instant()) > Duration.ofSeconds(30)) {
                null
            } else {
                logger.info("Target url: " + it.toASCIIString())
                measureSpeed(it)
            }
        }.let {
            if (it.size > 1) it.drop(1) else it
        }.average()

        return SpeedtestResult(avg)
    }

    private suspend fun measureSpeed(uri: URI): Double {
        val (size, time) = fileWebClient.get().uri(uri).awaitExchange {
            val start = clock.instant()
            it.awaitBody<ByteArray>().size to Duration.between(start, clock.instant()).seconds
        }

        return size / time.toDouble() / 125000
    }

    private suspend fun fetchTargets(): List<URI> {
        val response = apiWebClient().get()
            .uri("https://api.fast.com/netflix/speedtest/v2?token=${config.token}&https=true&urlCount=5")
            .retrieve()
            .awaitBody<ApiResponse>()

        return response.targets.map { it.url }
    }

    private data class ApiResponse(
        val targets: List<ApiTarget>
    )

    private data class ApiTarget(
        val url: URI
    )

    private data class Measurement(val size: Int, val start: Instant, val stop: Instant)
}
