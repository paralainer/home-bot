package com.paralainer.homebot.torrent

import org.springframework.stereotype.Component
import java.net.URI
import java.time.Duration

interface TorrentService {
    suspend fun listDownloads(): List<DownloadItem>
    suspend fun addByUrl(url: URI): Unit
}

@Component
class UTorrentService(
    private val client: UTorrentClient
) : TorrentService {
    override suspend fun listDownloads(): List<DownloadItem> =
        client.listDownloads().torrents.map {
            DownloadItem(
                hash = it[0] as String,
                name = it[2] as String,
                percentage = (it[4] as Int).toDouble() / 10,
                status = DownloadStatus.fromStatusString((it[21] as String)),
                eta = Duration.ofSeconds((it[10] as Int).toLong())
            )
        }

    override suspend fun addByUrl(url: URI): Unit = client.addByUrl(url)
}

data class DownloadItem(
    val hash: String,
    val name: String,
    val percentage: Double,
    val status: DownloadStatus,
    val eta: Duration
)

sealed class DownloadStatus(val name: String) {
    object InProgress : DownloadStatus("in progress")
    object Finished : DownloadStatus("finished")
    object Pause : DownloadStatus("pause")
    data class Failed(val error: String?) : DownloadStatus("error")
    class Unknown(name: String) : DownloadStatus(name);

    companion object {
        fun fromStatusString(status: String): DownloadStatus =
            when {
                status.startsWith("error", ignoreCase = true) ->
                    Failed(status.replace("error: ", "", ignoreCase = true))

                status.contains("seeding", ignoreCase = true) ||
                    status.contains("finish", ignoreCase = true) -> Finished

                status.contains("pause", ignoreCase = true) ||
                    status.contains("stop", ignoreCase = true) -> Pause

                status.contains("download", ignoreCase = true) ||
                    status.contains("queuing", ignoreCase = true) -> InProgress

                else -> {
                    println("Received unknown status: $status")
                    Unknown(status)
                }
            }
    }
}
