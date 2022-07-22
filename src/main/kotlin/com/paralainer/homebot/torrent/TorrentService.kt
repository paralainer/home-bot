package com.paralainer.homebot.torrent

import org.springframework.stereotype.Component

interface TorrentService {
    suspend fun listDownloads(): List<DownloadItem>
}

@Component
class UTorrentService(
    private val client: UTorrentClient
) : TorrentService {
    override suspend fun listDownloads(): List<DownloadItem> =
        client.listDownloads().torrents.map {
            DownloadItem(
                it[2].toString(),
                (it[4] as Int).toDouble() / 10,
                it[21].toString().split(" ").first()
            )
        }
}

data class DownloadItem(
    val name: String,
    val percentage: Double,
    val status: String
)

sealed class DownloadStatus(val name: String) {
    object InProgress : DownloadStatus("in-progress")
    object Finished : DownloadStatus("finished")
    object Pause : DownloadStatus("pause")
    data class Failed(val error: String?) : DownloadStatus("error")
}
