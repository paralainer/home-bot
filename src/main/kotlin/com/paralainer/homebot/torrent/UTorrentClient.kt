package com.paralainer.homebot.torrent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.paralainer.homebot.common.apiWebClientBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import org.springframework.web.reactive.function.client.awaitBody
import java.net.URI

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

    suspend fun listDownloads(): UTorrentDownload {
        val rawResponse = client.get().uri("?list=1")
            .retrieve().awaitBody<String>()

        return objectMapper.readValue(rawResponse)
    }

    suspend fun addByUrl(url: URI) {
        client.get().uri {
            it.queryParam("action", "add-url")
                .queryParam("s", url.toASCIIString())
                .build()
        }.retrieve().awaitBody<String>()
    }


}

data class UTorrentDownload(
    val torrents: List<List<Any>>
)




