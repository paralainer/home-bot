package com.paralainer.homebot.fastmile

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.annotation.JsonProperty
import com.paralainer.homebot.common.CustomObjectMapper
import com.paralainer.homebot.common.apiWebClientBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import reactor.netty.http.client.PrematureCloseException

@Component
class FastmileClient(
    config: FastmileConfig,
    private val auth: FastmileAuthenticator,
    private val objectMapper: CustomObjectMapper
) {
    private val webClient = apiWebClientBuilder().baseUrl(config.baseUrl).build()
    private var authentication: FastmileAuthentication? = null

    suspend fun status(): StatusResponse =
        objectMapper.readValue(
            retrieveBody {
                webClient.get().uri("/fastmile_radio_status_web_app.cgi")
            }.also(::println)
        )

    suspend fun reboot() {
        try {
            retrieveBody { auth ->
                webClient.post().uri("/reboot_web_app.cgi")
                    .body(BodyInserters.fromFormData("csrf_token", auth.csrfToken))
            }
        } catch (ex: WebClientRequestException) {
            // ignore PrematureCloseException cause this is what happens when you restart a router
            if (!ex.contains(PrematureCloseException::class.java)) {
                throw ex
            }
        }
    }

    private suspend fun retrieveBody(
        block: suspend (FastmileAuthentication) -> WebClient.RequestHeadersSpec<*>
    ): String {
        suspend fun doRequest(): String {
            val auth = fetchAuth()
            return block(auth).authenticated(auth).awaitExchange {
                if (it.rawStatusCode() >= 300) {
                    throw AuthException()
                }

                it.awaitBody<String>()
            }
        }

        return try {
            doRequest()
        } catch (ex: AuthException) {
            reAuthenticate()
            doRequest()
        }
    }


    private suspend fun WebClient.RequestHeadersSpec<*>.authenticated(
        auth: FastmileAuthentication
    ): WebClient.RequestHeadersSpec<*> {
        auth.cookies.forEach { (name, value) ->
            cookie(name, value)
        }

        return this
    }

    private suspend fun fetchAuth(): FastmileAuthentication {
        val auth = authentication ?: auth.authenticate()
        authentication = auth
        return auth
    }

    private suspend fun reAuthenticate() {
        authentication = auth.authenticate()
    }

    private class AuthException : RuntimeException()

    data class StatusResponse(
        @JsonProperty("cell_5G_stats_cfg")
        val cell5g: List<Cell5g>
    ) {
        data class Cell5g(
            val stat: Cell5gStat
        )

        data class Cell5gStat(
            @JsonProperty("PCI")
            val pci: Long
        )
    }
}
