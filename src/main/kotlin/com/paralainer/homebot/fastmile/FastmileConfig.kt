package com.paralainer.homebot.fastmile


class FastmileConfig(
    val username: String = System.getenv("FASTMILE_USERNAME"),
    val password: String = System.getenv("FASTMILE_PASSWORD"),
    val baseUrl: String = System.getenv("FASTMILE_BASE_URL")
)
