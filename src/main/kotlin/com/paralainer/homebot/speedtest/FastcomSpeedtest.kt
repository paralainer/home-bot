package com.paralainer.homebot.speedtest

import io.netty.resolver.DefaultAddressResolverGroup
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.http.client.HttpClient
import java.net.URI
import java.time.Clock
import java.time.Duration

@Service
class FastcomSpeedtest(
    private val config: FastcomConfig,
    private val clock: Clock
) : SpeedtestService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val apiWebClient = WebClient.builder().clientConnector(
        ReactorClientHttpConnector(
            HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE)
        )
    ).build()


    override suspend fun measureSpeed(): SpeedtestResult {
        val avg = fetchTargets().map {
            logger.info("Target url: " + it.toASCIIString())
            measureSpeed(it)
        }.drop(1).average()

        return SpeedtestResult(avg)
    }

    private suspend fun measureSpeed(uri: URI): Double {
        val start = clock.instant()
        val size = apiWebClient.get().uri(uri).exchangeToMono { clientResponse ->
            clientResponse
                .body(BodyExtractors.toDataBuffers())
                .map {
                    val count = it.readableByteCount()
                    it.read(ByteArray(count))
                    DataBufferUtils.release(it)
                    count
                }
                .collectList()
        }.awaitSingle().sum()

        val stop = clock.instant()
        val time = Duration.between(start, stop).toMillis()
        return (size / time.toDouble()) * 1000 / (1024 * 1024) * 8
    }

    private suspend fun fetchTargets(): List<URI> {
        val response = apiWebClient.get()
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
}
