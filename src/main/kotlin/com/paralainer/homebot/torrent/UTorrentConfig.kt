package com.paralainer.homebot.torrent

class UTorrentConfig(
    val baseUrl: String = System.getenv("UTORRENT_BASE_URL"),
    val username: String = System.getenv("UTORRENT_USERNAME"),
    val password: String = System.getenv("UTORRENT_PASSWORD")
)
