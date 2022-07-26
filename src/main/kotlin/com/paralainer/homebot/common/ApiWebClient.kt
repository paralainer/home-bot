package com.paralainer.homebot.common

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.features.json.*

fun apiWebClient() = HttpClient(Java) {
    install(JsonFeature) {
        serializer = GsonSerializer {
        }
    }

    followRedirects = false
}

