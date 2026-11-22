// A composable SAM conversion has its own restart group: a state read in its body restarts it alone,
// without ItemList, also when copied from an inline function. On JS, Wasm and Native it had none (CMP-9447)

// WITH_COROUTINES

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
import kotlinx.coroutines.withContext
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

suspend fun box(): String = withContext(TestFrameClock) {
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
