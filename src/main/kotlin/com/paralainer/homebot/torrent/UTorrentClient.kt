package com.paralainer.homebot.torrent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.paralainer.homebot.common.apiWebClientBuilder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import java.time.Duration
import java.time.Instant

@Component
class UTorrentClient(
    config: UTorrentConfig,
    private val objectMapper: ObjectMapper
) {
    private val client = apiWebClientBuilder()
        .baseUrl(config.baseUrl)
        .filter(
            ExchangeFilterFunctions
                .basicAuthentication(config.username, config.password)
        )
        .build()

    private var cachedToken: Token? = null
    private val lock = Mutex()

    private suspend fun getToken(): Token =
        lock.withLock { // pretty dumb, but I don't care cause concurrency is very low
            val cachedToken = this.cachedToken
            if (cachedToken == null || cachedToken.isExpired()) {
                val token = fetchToken()
                this.cachedToken = token
                return token
            } else {
                cachedToken
            }
        }

    private suspend fun fetchToken(): Token {
        val (cookie, rawResponse) = client.get().uri("token.html").awaitExchange {
            it.cookies().getFirst("GUID")!!.value to it.awaitBody<String>()
        }
        val parsedToken = rawResponse.replace(Regex("<[^>]+>"), "")

        return Token(
            parsedToken,
            cookie,
            Instant.now()
        )
    }

    suspend fun listDownloads(): UTorrentDownload {
        val token = getToken()
        val rawResponse = client.get().uri("?token=${token.value}&list=1").cookie("GUID", token.cookie)
            .retrieve().awaitBody<String>()

        return objectMapper.readValue(rawResponse)
    }

    private data class Token(val value: String, val cookie: String, val cachedAt: Instant) {
        fun isExpired(): Boolean =
            Duration.between(cachedAt, Instant.now()) > Duration.ofMinutes(20)
    }
}

data class UTorrentDownload(
    val torrents: List<List<Any>>
)




