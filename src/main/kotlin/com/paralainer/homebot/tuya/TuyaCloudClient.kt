package com.paralainer.homebot.tuya

import com.google.gson.annotations.SerializedName
import com.paralainer.homebot.common.apiWebClient
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigInteger
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TuyaCloudClient(
    private val config: TuyaCloudConfig
) {

    private val webClient = apiWebClient()

    private var auth: Auth? = null
    private val authLock = Mutex()

    suspend fun getDeviceStatus(deviceId: String): TuyaDeviceStatus =
        webClient.get("${config.baseUrl}/v1.0/iot-03/devices/$deviceId/status") {
            authenticate()
        }

    suspend fun getDevicesStatus(deviceIds: List<String>): TuyaDevicesStatus =
        webClient.get("${config.baseUrl}/v1.0/iot-03/devices/status?device_ids=${deviceIds.joinToString(",")}") {
            authenticate()
        }

    private suspend fun getToken(): TokenResponse =
        webClient.get("${config.baseUrl}/v1.0/token?grant_type=1") {
            authenticate(noRefresh = true)
        }

    private fun sign(
        request: HttpRequestBuilder,
        t: Instant
    ): String = buildString {
        val currentAuth = auth
        append(config.clientId)
        if (currentAuth?.accessToken != null) {
            append(currentAuth.accessToken)
        }
        append(t.toEpochMilli())
        val uri = request.url.build().toURI()

        val stringToSign =
            request.method.value.uppercase() +
                "\n" +
                EMPTY_BODY +
                "\n" +
                "" + // headers
                "\n" +
                uri.path + uri.query.let { if (it.isNullOrEmpty()) "" else "?${it}" }

        append(stringToSign)
    }.hmacSha256()


    private suspend fun HttpRequestBuilder.authenticate(noRefresh: Boolean = false) {
        if (auth.isExpired() && !noRefresh) {
            refreshAuthentication()
        }
        headers.append("client_id", config.clientId)
        headers.append("sign_method", "HMAC-SHA256")
        val t = Instant.now()
        auth?.accessToken?.also {
            headers.append("access_token", it)
        }
        headers.append("t", t.toEpochMilli().toString())
        headers.append("sign", sign(this, t))
    }

    private suspend fun refreshAuthentication(): Auth = authLock.withLock {
        var auth = this.auth
        if (auth != null && !auth.isExpired()) {
            return@withLock auth
        }

        this.auth = null

        val result = getToken().result ?: throw Exception("Authentication failed")

        auth = Auth(
            accessToken = result.accessToken,
            refreshToken = result.accessToken,
            expiresAt = Instant.now().plusSeconds(result.expiry.toLong()).minusSeconds(60)
        )

        this.auth = auth
        println("Reauthenticated for tuya")
        return auth
    }


    private data class Auth(
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: Instant
    )

    private fun Auth?.isExpired(): Boolean =
        this?.expiresAt?.isBefore(Instant.now()) ?: true

    private data class TokenResponse(
        val result: Result?,
        val success: Boolean,
        val t: Long
    ) {
        data class Result(
            @SerializedName("access_token")
            val accessToken: String,
            @SerializedName("refresh_token")
            val refreshToken: String,
            @SerializedName("expire_time")
            val expiry: Int,
            val uid: String
        )
    }

    private fun String.hmacSha256(): String {
        val mac = Mac.getInstance("HmacSHA256");
        val secretKeySpec = SecretKeySpec(config.cleintSecret.toByteArray(), "HmacSHA256");
        mac.init(secretKeySpec);
        val hmacSha256 = mac.doFinal(this.toByteArray())

        return String.format("%064x", BigInteger(1, hmacSha256)).uppercase()
    }

    private companion object {
        const val EMPTY_BODY = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }
}

data class TuyaDevicesStatus(val result: List<Status>?) {
    data class Status(val id: String, val status: List<TuyaDeviceStatus.Item>)
}
data class TuyaDeviceStatus(val result: List<Item>?) {
    data class Item(
        val code: String,
        val value: Any
    )
}
