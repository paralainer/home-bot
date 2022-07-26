package com.paralainer.homebot.torrent

import com.paralainer.homebot.torrent.DownloadStatus.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TorrentStatusTracker(
    private val torrentService: TorrentService
) {
    private val sharedFlow: SharedFlow<TorrentEvent> by lazy {
        startObserving()
    }

    private val currentTorrents: MutableMap<String, DownloadItem> = mutableMapOf()

    fun observeEvents(): Flow<TorrentEvent> = sharedFlow

    private fun startObserving(): SharedFlow<TorrentEvent> {
        val flow = MutableSharedFlow<TorrentEvent>()
        GlobalScope.launch {
            currentTorrents.putAll(torrentService.listDownloads().associateBy { it.hash })

            while (isActive) {
                delay(2000)
                compareState(flow)
            }
        }
        return flow
    }

    private suspend fun compareState(flow: MutableSharedFlow<TorrentEvent>) {
        torrentService.listDownloads().forEach {
            val knownTorrent = currentTorrents[it.hash]
            when {
                knownTorrent == null ||
                    (knownTorrent.status !is InProgress && it.status is InProgress) -> {
                    flow.emit(TorrentEvent.Started(name = it.name, hash = it.hash))
                }

                knownTorrent.status !is Finished && it.status is Finished ->
                    flow.emit(TorrentEvent.Finished(name = it.name, hash = it.hash))

                knownTorrent.status !is Failed && it.status is Failed ->
                    flow.emit(TorrentEvent.Error(name = it.name, hash = it.hash, error = it.status.error))
            }
            currentTorrents[it.hash] = it
        }
    }
}


sealed interface TorrentEvent {
    data class Started(val name: String, val hash: String) : TorrentEvent
    data class Finished(val name: String, val hash: String) : TorrentEvent
    data class Error(val name: String, val hash: String, val error: String?) : TorrentEvent
}
