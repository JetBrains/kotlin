// A capture-free composable SAM conversion outside a composable is a singleton, as on JVM: the callee
// skips while nothing changes and a state read in the body restarts it alone. Capturing ones differ

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

private fun interface Renderer {
    @Composable
    fun Render(text: String)
}

private fun interface ValueRenderer<T> {
    @Composable
    fun Render(value: T)
}

private val tick = mutableStateOf(0)
private val suffix = mutableStateOf("")
private var renders = 0
private var appShows = 0

private fun makeRenderer(): Renderer = Renderer { text ->
    renders++
    TextNode(text + suffix.value)
}

@Composable
private fun Wrap(content: @Composable () -> Unit) = content()

private fun makeWrapping(): Renderer = Renderer { text ->
    Wrap { TextNode(text) }
    Wrap { TextNode("d") }
}

private inline fun makeInline(): Renderer = Renderer { text -> TextNode(text + "i") }

private fun makeLabelled(label: String): Renderer = Renderer { text -> TextNode(label + text) }

private fun <T> makeValueRenderer(): ValueRenderer<T> = ValueRenderer { value -> TextNode("$value") }

private class Named(val name: String) {
    fun renderer(): Renderer = Renderer { text -> TextNode(name + text) }
}

private class InitHolder(label: String) {
    val renderer: Renderer

    init {
        val local = label
        renderer = Renderer { text -> TextNode(local + text) }
    }
}

@Composable
private fun Show(renderer: Renderer) {
    renderer.Render("a")
}

@Composable
private fun AppShow(renderer: Renderer) {
    appShows++
    renderer.Render("a")
}

@Composable
private fun App() {
    tick.value
    AppShow(makeRenderer())
}

@Composable
private fun Distinct() {
    Show(makeLabelled("x"))
    Show(makeLabelled("y"))
    Show(Named("n").renderer())
    Show(Named("m").renderer())
    Show(InitHolder("i").renderer)
    Show(InitHolder("j").renderer)
    makeValueRenderer<Int>().Render(1)
    Show(makeWrapping())
    Show(makeInline())
}

suspend fun box(): String = withContext(TestFrameClock) {
    if (makeRenderer() !== makeRenderer()) return@withContext "capture-free conversion is not one instance"
    if (makeWrapping() !== makeWrapping()) return@withContext "capture-free conversion with lambdas is not one instance"
    if (makeLabelled("x") === makeLabelled("y")) return@withContext "conversion capturing a parameter is one instance"
    if (Named("n").renderer() === Named("m").renderer()) return@withContext "conversion capturing this is one instance"
    if (InitHolder("i").renderer === InitHolder("j").renderer) return@withContext "conversion capturing a local of init is one instance"

    coroutineScope {
        val root = Node()
        val recomposer = Recomposer(coroutineContext)
        val recomposerJob = launch(start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }
        val composition = Composition(NodeApplier(root), recomposer)

        composition.setContent {
            App()
            Distinct()
        }
        recomposer.awaitIdle()

        suspend fun update(change: () -> Unit) {
            change()
            Snapshot.sendApplyNotifications()
            yield()
            recomposer.awaitIdle()
        }
        update { tick.value++ }
        update { tick.value++ }
        val afterTicks = "renders=$renders, appShows=$appShows"

        update { suffix.value = "!" }
        val result = root.children.map { it.text }

        composition.dispose()
        recomposer.cancel()
        recomposerJob.join()

        when {
            afterTicks != "renders=1, appShows=1" -> "after ticks: $afterTicks"
            result != listOf("a!", "xa", "ya", "na", "ma", "ia", "ja", "1", "a", "d", "ai") -> "result=$result"
            renders != 2 || appShows != 1 -> "renders=$renders, appShows=$appShows"
            else -> "OK"
        }
    }
}
