package com.paralainer.homebot.common

import io.netty.resolver.DefaultAddressResolverGroup
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

fun apiWebClientBuilder() = WebClient.builder().clientConnector(
    ReactorClientHttpConnector(
        HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE)
    )
)

fun apiWebClient() = apiWebClientBuilder().build()

