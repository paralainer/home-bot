package com.paralainer.homebot.fastmile

import com.fasterxml.jackson.module.kotlin.readValue
import com.paralainer.homebot.common.CustomObjectMapper
import com.paralainer.homebot.common.apiWebClientBuilder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import java.security.MessageDigest
import java.util.*
import kotlin.random.Random

@Component
class FastmileAuthenticator(
    private val config: FastmileConfig,
    private val objectMapper: CustomObjectMapper
) {
    private val webClient = apiWebClientBuilder().baseUrl(config.baseUrl).build()
    private val lock = Mutex()

    suspend fun authenticate(): FastmileAuthentication = lock.withLock {
        doAuth()
    }

    private suspend fun doAuth(): FastmileAuthentication {
        val nonce = nonce()
        val authData = AuthData(
            username = config.username,
            password = config.password,
            nonce = nonce.nonce,
            randomKey = nonce.randomKey
        )

        return webClient.post().uri("/login_web_app.cgi").body(
            BodyInserters.fromFormData("userhash", authData.userHash())
                .with("RandomKeyhash", authData.randomKeyHash())
                .with("response", authData.response())
                .with("nonce", authData.nonce)
                .with("enckey", authData.encKey)
                .with("enciv", authData.encIv)

        ).awaitExchange { resp ->
            if (resp.rawStatusCode() >= 300) {
                throw RuntimeException("Authentication failed. ${resp.rawStatusCode()}")
            }
            val body = objectMapper.readValue<AuthResponse>(resp.awaitBody<String>())

            toAuthentication(
                resp.cookies(),
                body.token
            )
        }
    }

    private fun toAuthentication(
        cookies: MultiValueMap<String, ResponseCookie>,
        csrfToken: String
    ) = FastmileAuthentication(
        cookies.toSingleValueMap().mapValues { (_, value) -> value.value },
        csrfToken
    )

    private suspend fun nonce(): NonceResponse =
        objectMapper.readValue(
            webClient.get().uri("/login_web_app.cgi?nonce").retrieve().awaitBody<String>()
        )

    private data class NonceResponse(
        val nonce: String,
        val randomKey: String
    )

    private data class AuthResponse(
        val token: String
    )

    data class AuthData(
        val username: String,
        val password: String,
        val nonce: String,
        val randomKey: String
    ) {
        val encKey = Random.nextBytes(16).base64()
        val encIv = Random.nextBytes(16).base64()
        fun userHash() = hash(username to nonce)
        fun randomKeyHash() = hash(randomKey to nonce)
        fun response() = hash(
            hash(username to password) to nonce
        )

        private val sha256 = MessageDigest.getInstance("SHA-256")
        private fun hash(pair: Pair<String, String>): String =
            "${pair.first}:${pair.second}".sha256().base64()

        private fun ByteArray.base64(): String = Base64.getEncoder().encodeToString(this)
        private fun String.sha256(): ByteArray = sha256.digest(this.toByteArray())
    }
}

data class FastmileAuthentication(
    val cookies: Map<String, String>,
    val csrfToken: String
)



