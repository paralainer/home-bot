package com.paralainer.homebot.torrent

import com.paralainer.homebot.common.apiWebClient
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import java.net.URI

class UTorrentClient(
    private val config: UTorrentConfig,
) {
    private val client = apiWebClient().config {
        Json { accept(ContentType.Any) }

        install(Auth) {
            basic {
                credentials { BasicAuthCredentials(config.username, config.password) }
            }
        }
    }

    suspend fun listDownloads(): UTorrentDownload =
        client.get("${config.baseUrl}?list=1")

    suspend fun addByUrl(url: URI) {
        client.submitForm<String>(
            config.baseUrl, formParameters = Parameters.build {
                append("action", "add-url")
                append("s", url.toASCIIString())
            },
            encodeInQuery = true
        )
    }

    suspend fun remove(hash: String) {
        client.submitForm<String>(
            config.baseUrl, formParameters = Parameters.build {
                append("action", "remove")
                append("hash", hash)
            },
            encodeInQuery = true
        )
    }
}

data class UTorrentDownload(
    val torrents: List<List<Any>>
)
