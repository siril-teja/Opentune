package com.opentune.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opentune.data.MusicRepository
import com.opentune.data.model.SearchFilter
import com.opentune.data.model.SearchResult
import com.opentune.data.model.Song
import com.opentune.data.model.UiState
import com.opentune.data.settings.AppSettings
import com.opentune.ui.LinkTarget
import com.opentune.ui.Links
import com.opentune.ui.friendlyError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import com.opentune.data.innertube.StreamResolver
import kotlinx.coroutines.Dispatchers

class SearchViewModel : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    /** The query the results belong to; null before the first search. */
    private val _submitted = MutableStateFlow<String?>(null)
    val submitted = _submitted.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter.ALL)
    val filter = _filter.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val _results = MutableStateFlow<UiState<List<SearchResult>>>(UiState.Loading)
    val results = _results.asStateFlow()

    /** A YouTube video link in the box, and the song it names once that's been looked up. */
    data class PastedLink(val videoId: String, val song: UiState<Song>)

    private val _link = MutableStateFlow<PastedLink?>(null)
    val link = _link.asStateFlow()

    /** A pasted link plays as soon as it's read: the song and its radio, and where the song is in it. */
    private val _playLink = Channel<Pair<List<Song>, Int>>(Channel.CONFLATED)
    val playLink = _playLink.receiveAsFlow()

    private var searchJob: Job? = null
    private var linkJob: Job? = null

    init {
        viewModelScope.launch { watchSuggestions() }
    }

    @OptIn(FlowPreview::class)
    private suspend fun watchSuggestions() {
        _query.map { it.trim() }.distinctUntilChanged().debounce(220).collectLatest { q ->
            if (q.isEmpty() || q == _submitted.value || _link.value != null) {
                _suggestions.value = emptyList()
                return@collectLatest
            }
            _suggestions.value = runCatching { MusicRepository.suggestions(q) }.getOrDefault(_suggestions.value)
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
        readLink(value)
    }

    fun submit(value: String) {
        val q = value.trim()
        if (q.isEmpty()) return
        // A link isn't searched for: it's already being played.
        if (_link.value != null) return
        _query.value = q
        _submitted.value = q
        AppSettings.addRecentSearch(q)
        run()
    }

    fun setFilter(filter: SearchFilter) {
        if (_filter.value == filter) return
        _filter.value = filter
        run()
    }

    fun retry() = run()

    fun retryLink() {
        val id = _link.value?.videoId ?: return
        lookUp(id)
    }

    /** Notices a youtube.com, youtu.be or music.youtube.com video link as it's pasted. */
    private fun readLink(text: String) {
        val id = (Links.parse(text) as? LinkTarget.Song)?.videoId
        if (id == _link.value?.videoId) return
        linkJob?.cancel()
        if (id == null) {
            _link.value = null
            return
        }
        lookUp(id)
    }

    /** Finds the song for [id], with the radio YouTube would play after it, and plays them. */
    private fun lookUp(id: String) {
        linkJob?.cancel()
        _link.value = PastedLink(id, UiState.Loading)
        linkJob = viewModelScope.launch {
            val (queue, at) = MusicRepository.linkQueue(id)
            _link.value = PastedLink(id, UiState.Success(queue[at]))
            _playLink.trySend(queue to at)
        }
    }

    private fun run() {
        val q = _submitted.value ?: return
        val f = _filter.value
        searchJob?.cancel()
        val key = "$f:${q.trim().lowercase()}"
        // A search made a minute ago comes back at once; it's refreshed anyway.
        _results.value = recentSearches[key]?.let { UiState.Success(it) } ?: UiState.Loading
        searchJob = viewModelScope.launch {
            val fresh = try {
                UiState.Success(MusicRepository.search(q, f))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_results.value is UiState.Success) null else UiState.Error(friendlyError(e))
            }
            fresh?.let { _results.value = it }
            (fresh as? UiState.Success)?.data?.let { rows ->
                recentSearches[key] = rows
                warmTopResult(rows)
            }
        }
    }

    /**
     * Finds the stream for the first song in the results before it's tapped,
     * since it's the one most likely to be. The stream URL is cached for 20
     * minutes, so the tap only has to fetch audio.
     */
    private fun warmTopResult(rows: List<SearchResult>) {
        val top = rows.firstNotNullOfOrNull {
            when (it) {
                is SearchResult.TopTrack -> it.song
                is SearchResult.Track -> it.song
                else -> null
            }
        } ?: return
        viewModelScope.launch(Dispatchers.IO) { runCatching { StreamResolver.resolve(top.videoId) } }
    }

    private val recentSearches = object : LinkedHashMap<String, List<SearchResult>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<SearchResult>>?) = size > 60
    }
}
