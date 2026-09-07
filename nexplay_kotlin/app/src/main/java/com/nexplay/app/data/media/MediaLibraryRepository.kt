package com.nexplay.app.data.media

import android.content.Context
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.FolderInfo
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MediaLibrarySourceStatus {
    NOT_SCANNED,
    READY,
    PERMISSION_DENIED,
    FAILED,
}

data class MediaLibraryState(
    val items: List<MediaItem> = emptyList(),
    val audioStatus: MediaLibrarySourceStatus =
        MediaLibrarySourceStatus.NOT_SCANNED,
    val videoStatus: MediaLibrarySourceStatus =
        MediaLibrarySourceStatus.NOT_SCANNED,
    val isRefreshing: Boolean = false,
)

class MediaLibraryRepository internal constructor(
    private val scanAudio: () -> AudioMediaScanResult,
    private val scanVideo: () -> VideoMediaScanResult,
) {
    constructor(context: Context) : this(
        scanAudio = {
            AudioMediaScanner(context).scan()
        },
        scanVideo = {
            VideoMediaScanner(context).scan()
        },
    )

    private val _state = MutableStateFlow(MediaLibraryState())

    val state: StateFlow<MediaLibraryState> = _state.asStateFlow()

    fun getAllMedia(): List<MediaItem> {
        return _state.value.items
    }

    fun getAudio(): List<AudioItem> {
        return _state.value.items.filterIsInstance<AudioItem>()
    }

    fun getVideos(): List<VideoItem> {
        return _state.value.items.filterIsInstance<VideoItem>()
    }

    fun getFolders(): List<FolderInfo> {
        return _state.value.items
            .mapNotNull { it.folder }
            .distinctBy(::folderKey)
            .sortedWith(
                Comparator { first, second ->
                    val nameComparison =
                        first.name.compareTo(
                            second.name,
                            ignoreCase = true,
                        )

                    if (nameComparison != 0) {
                        nameComparison
                    } else {
                        first.path
                            .orEmpty()
                            .compareTo(second.path.orEmpty())
                    }
                },
            )
    }

    fun getMediaInFolder(
        folder: FolderInfo,
    ): List<MediaItem> {
        val targetKey = folderKey(folder)

        return _state.value.items.filter { item ->
            item.folder?.let(::folderKey) == targetKey
        }
    }

    fun getRecentlyAdded(): List<MediaItem> {
        return _state.value.items.sortedWith(
            Comparator { first, second ->
                val dateComparison =
                    (second.dateAddedEpochSeconds ?: Long.MIN_VALUE)
                        .compareTo(
                            first.dateAddedEpochSeconds
                                ?: Long.MIN_VALUE,
                        )

                if (dateComparison != 0) {
                    dateComparison
                } else {
                    compareMediaTitles(first, second)
                }
            },
        )
    }

    fun getByUri(
        uri: String,
    ): MediaItem? {
        return _state.value.items.firstOrNull { item ->
            item.uri == uri
        }
    }

    @Synchronized
    fun refresh(): MediaLibraryState {
        val previousState = _state.value

        _state.value =
            previousState.copy(
                isRefreshing = true,
            )

        val audioResult = safeAudioScan()
        val videoResult = safeVideoScan()

        val previousAudio =
            previousState.items.filterIsInstance<AudioItem>()

        val previousVideo =
            previousState.items.filterIsInstance<VideoItem>()

        val audioItems =
            when (audioResult) {
                is AudioMediaScanResult.Success ->
                    audioResult.items

                AudioMediaScanResult.PermissionDenied ->
                    emptyList()

                is AudioMediaScanResult.Failure ->
                    previousAudio
            }

        val videoItems =
            when (videoResult) {
                is VideoMediaScanResult.Success ->
                    videoResult.items

                VideoMediaScanResult.PermissionDenied ->
                    emptyList()

                is VideoMediaScanResult.Failure ->
                    previousVideo
            }

        val nextState =
            MediaLibraryState(
                items =
                    normalizeLibrary(
                        audioItems + videoItems,
                    ),
                audioStatus = audioResult.toLibraryStatus(),
                videoStatus = videoResult.toLibraryStatus(),
                isRefreshing = false,
            )

        _state.value = nextState

        return nextState
    }

    private fun safeAudioScan(): AudioMediaScanResult {
        return try {
            scanAudio()
        } catch (error: RuntimeException) {
            AudioMediaScanResult.Failure(error)
        }
    }

    private fun safeVideoScan(): VideoMediaScanResult {
        return try {
            scanVideo()
        } catch (error: RuntimeException) {
            VideoMediaScanResult.Failure(error)
        }
    }

    private fun normalizeLibrary(
        items: List<MediaItem>,
    ): List<MediaItem> {
        return items
            .distinctBy { it.uri }
            .sortedWith(
                Comparator(::compareMediaTitles),
            )
    }

    private fun compareMediaTitles(
        first: MediaItem,
        second: MediaItem,
    ): Int {
        val titleComparison =
            first.title.compareTo(
                second.title,
                ignoreCase = true,
            )

        if (titleComparison != 0) {
            return titleComparison
        }

        return first.uri.compareTo(second.uri)
    }

    private fun folderKey(
        folder: FolderInfo,
    ): String {
        return folder.path?.let {
            "path:$it"
        } ?: "name:${folder.name}"
    }

    private fun AudioMediaScanResult.toLibraryStatus():
            MediaLibrarySourceStatus {
        return when (this) {
            is AudioMediaScanResult.Success ->
                MediaLibrarySourceStatus.READY

            AudioMediaScanResult.PermissionDenied ->
                MediaLibrarySourceStatus.PERMISSION_DENIED

            is AudioMediaScanResult.Failure ->
                MediaLibrarySourceStatus.FAILED
        }
    }

    private fun VideoMediaScanResult.toLibraryStatus():
            MediaLibrarySourceStatus {
        return when (this) {
            is VideoMediaScanResult.Success ->
                MediaLibrarySourceStatus.READY

            VideoMediaScanResult.PermissionDenied ->
                MediaLibrarySourceStatus.PERMISSION_DENIED

            is VideoMediaScanResult.Failure ->
                MediaLibrarySourceStatus.FAILED
        }
    }
}