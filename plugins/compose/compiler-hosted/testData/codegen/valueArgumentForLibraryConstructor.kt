// DUMP_IR

// MODULE: room
// MODULE_KIND: LibraryBinary
// FILE: androidx/room/Relation.kt
package androidx.room

import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Relation(
    val entity: KClass<*> = Any::class,
    val parentColumn: String,
    val entityColumn: String,
    val projection: Array<String> = []
)

// MODULE: main(room)
// FILE: com/example/jetcaster/data/EpisodeToPodcast.kt
package com.example.jetcaster.data

import java.util.Objects

class EpisodeToPodcast {
    @Embedded
    lateinit var episode: String

    @Relation(parentColumn = "podcast_uri", entityColumn = "uri")
    lateinit var _podcasts: List<String>

    @get:Ignore
    val podcast: String
        get() = _podcasts[0]

    /**
     * Allow consumers to destructure this class
     */
    operator fun component1() = episode
    operator fun component2() = podcast

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other is EpisodeToPodcast -> episode == other.episode && _podcasts == other._podcasts
        else -> false
    }

    override fun hashCode(): Int = Objects.hash(episode, _podcasts)
}

// FILE: main.kt
package home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.jetcaster.data.EpisodeToPodcast

@Composable
private fun CategoryPodcasts(onTogglePodcastFollowed: (String) -> Unit) {
}

@Composable
fun EpisodeListItem(
    episode: String,
    podcast: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
}

fun LazyListScope.podcastCategory(
    episodes: List<EpisodeToPodcast>,
    navigateToPlayer: (String) -> Unit,
    onTogglePodcastFollowed: (String) -> Unit,
) {
    item {
        CategoryPodcasts(onTogglePodcastFollowed)
    }

    items(episodes, key = { it.episode }) { item ->
        EpisodeListItem(
            episode = item.episode,
            podcast = item.podcast,
            onClick = navigateToPlayer,
            modifier = Modifier.fillParentMaxWidth()
        )
    }
}