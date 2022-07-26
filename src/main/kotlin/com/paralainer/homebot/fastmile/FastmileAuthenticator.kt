package com.paralainer.homebot.fastmile

import com.paralainer.homebot.common.apiWebClient
import io.ktor.client.call.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.*
import kotlin.random.Random

class FastmileAuthenticator(
    private val config: FastmileConfig
) {

    private val webClient = apiWebClient().config {
        Json { accept(ContentType.Any) }
    }

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

        val resp: HttpResponse = webClient.submitForm<HttpStatement>("${config.baseUrl}/login_web_app.cgi",
            formParameters = Parameters.build {
                append("userhash", authData.userHash())
                append("RandomKeyhash", authData.randomKeyHash())
                append("response", authData.response())
                append("nonce", authData.nonce)
                append("enckey", authData.encKey)
                append("enciv", authData.encIv)
            }).execute()

        if (resp.status.value >= 300) {
            throw RuntimeException("Authentication failed. ${resp.status.value}")
        }
        val body = resp.receive<AuthResponse>()

        resp.setCookie()
        resp.headers["Set-Cookie"]

        return toAuthentication(
            resp.setCookie(),
            body.token
        )
    }

    private fun toAuthentication(
        cookie: List<Cookie>,
        csrfToken: String
    ) = FastmileAuthentication(
        cookie.find { it.name == "sid" }?.value ?: throw Exception("sid cookie not found in auth response"),
        cookie.find { it.name == "lsid" }?.value ?: throw Exception("lsid cookie not found in auth response"),
        csrfToken
    )

    private suspend fun nonce(): NonceResponse =
        webClient.get("${config.baseUrl}/login_web_app.cgi?nonce")

    data class NonceResponse(
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
    val sid: String,
    val lsid: String,
    val csrfToken: String
)
