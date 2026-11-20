// A composable SAM conversion has its own restart group: a state read in its body restarts it alone,
// without ItemList, also when copied from an inline function. On JS, Wasm and Native it had none (CMP-9447)

// Static caches of custom klibs are compiled without dependencies between them, which fails for
// Compose runtime klibs (e.g. kotlinx-coroutines-core depending on atomicfu).
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// FREE_COMPILER_ARGS: -Xverify-ir=none
// REASON: Compose runtime klib used in tests is compiled with Kotlin 2.3.20, whose Compose plugin
// generated ComposableSingletons objects without supertypes (fixed in 2.5.0 by KT-87962). IR validation
// of the deserialized runtime fails with "IrClass must have at least one supertype". Remove once tests
// use a Compose runtime compiled with Kotlin 2.5.0 or newer.

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

private object TestFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(0L)
}

private class Node(var text: String = "") {
    val children = mutableListOf<Node>()
}

private class NodeApplier(root: Node) : AbstractApplier<Node>(root) {
    override fun insertTopDown(index: Int, instance: Node) {}

    override fun insertBottomUp(index: Int, instance: Node) {
        current.children.add(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        repeat(count) { current.children.removeAt(index) }
    }

    override fun move(from: Int, to: Int, count: Int) {
        val nodes = current.children.subList(from, from + count).toList()
        current.children.subList(from, from + count).clear()
        current.children.addAll(if (to > from) to - count else to, nodes)
    }

    override fun onClear() {
        root.children.clear()
    }
}

@Composable
private fun TextNode(text: String) {
    ComposeNode<Node, NodeApplier>(
        factory = ::Node,
        update = { set(text) { this.text = it } },
    )
}

private fun interface ItemRenderer {
    @Composable
    operator fun invoke(text: String)
}

private val suffix = mutableStateOf("")
private var itemListCompositions = 0

@Composable
private fun ItemList(itemRenderer: ItemRenderer) {
    itemListCompositions++
    listOf("a", "b").forEach { itemRenderer(it) }
}

// A method not named `invoke`: the JS backend took a class of the JVM lambda origin for its own lambda
private fun interface Label {
    @Composable
    fun Content(text: String)
}

@Composable
private fun Labelled(label: Label) {
    label.Content("c")
}

private inline fun inlineRenderer(crossinline transform: (String) -> String): ItemRenderer =
    ItemRenderer { text -> TextNode(transform(text) + suffix.value) }

@Composable
private fun App() {
    ItemList { text -> TextNode(text + suffix.value) }
    ItemList(inlineRenderer { it.uppercase() })
    Labelled { text -> TextNode(text + suffix.value) }
}

fun box(): String = runBlocking(TestFrameClock) {
    coroutineScope {
        val root = Node()
        val recomposer = Recomposer(coroutineContext)
        val recomposerJob = launch(start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }
        val composition = Composition(NodeApplier(root), recomposer)

        composition.setContent { App() }
        recomposer.awaitIdle()
        check(root.children.map { it.text } == listOf("a", "b", "A", "B", "c"))
        check(itemListCompositions == 2)

        suffix.value = "!"
        Snapshot.sendApplyNotifications()
        yield()
        recomposer.awaitIdle()
        val result = root.children.map { it.text }

        composition.dispose()
        recomposer.cancel()
        recomposerJob.join()

        when {
            result != listOf("a!", "b!", "A!", "B!", "c!") -> "result=$result"
            itemListCompositions != 2 -> "itemListCompositions=$itemListCompositions"
            else -> "OK"
        }
    }
}
