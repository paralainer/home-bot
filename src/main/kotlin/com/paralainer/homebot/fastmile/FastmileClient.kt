package com.paralainer.homebot.fastmile

import com.fasterxml.jackson.annotation.JsonProperty
import com.paralainer.homebot.common.apiWebClient
import io.ktor.client.call.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

class FastmileClient(
    private val config: FastmileConfig,
    private val auth: FastmileAuthenticator
) {
    private val webClient = apiWebClient().config {
        Json { accept(ContentType.Any) }
    }

    private var authentication: FastmileAuthentication? = null

    suspend fun status(): StatusResponse =
        withAuthentication {
            webClient.get("${config.baseUrl}/fastmile_radio_status_web_app.cgi") {
                authenticate()
            }
        }

    suspend fun reboot() {
        withAuthentication<String> {
            webClient.submitForm(
                url = "${config.baseUrl}/reboot_web_app.cgi",
                formParameters = Parameters.build { append("csrf_token", auth.csrfToken) }
            ) { authenticate() }
        }
    }

    private suspend inline fun <reified T> withAuthentication(
        block: AuthenticationContext.() -> HttpStatement
    ): T {
        return try {
            doRequest(block)
        } catch (ex: AuthException) {
            reAuthenticate()
            doRequest(block)
        }
    }

    private suspend inline fun <reified T> doRequest(block: AuthenticationContext.() -> HttpStatement): T {
        val auth = fetchAuth()
        val resp = block(AuthenticationContext(auth)).execute()

        if (resp.status.value >= 300) {
            throw AuthException()
        }
        return resp.receive()
    }

    class AuthenticationContext(val auth: FastmileAuthentication) {
        fun HttpRequestBuilder.authenticate() {
            cookie("sid", auth.sid)
            cookie("lsid", auth.lsid)
        }
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
