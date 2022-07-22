package com.paralainer.homebot.torrent

import com.paralainer.homebot.common.CustomObjectMapper
import io.kotest.common.runBlocking
import org.junit.jupiter.api.Test

internal class UTorrentClientTest {
    @Test
    fun testList() = runBlocking {
        val downloads = UTorrentClient(CustomObjectMapper()).listDownloads()
        println(downloads)
    }
}
