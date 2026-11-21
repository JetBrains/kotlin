// An anonymous `fun` with 10 values converted to a composable fun interface: the implementing object
// takes the eleventh `$changed` slot, so a second mask is needed. JS used to fail to build it

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

private fun interface TenValues {
    @Composable
    fun Content(a0: Int, a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int)
}

@Composable
private fun Show(content: TenValues) {
    content.Content(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
}

@Composable
private fun App(captured: Int) {
    Show(TenValues(@Composable fun(a0: Int, a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int) {
        TextNode("sum=" + (captured + a0 + a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8 + a9))
    }))
}

suspend fun box(): String = withContext(TestFrameClock) {
    coroutineScope {
        val root = Node()
        val recomposer = Recomposer(coroutineContext)
        val recomposerJob = launch(start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }
        val composition = Composition(NodeApplier(root), recomposer)

        val input = mutableStateOf(0)
        composition.setContent { App(input.value) }
        recomposer.awaitIdle()
        val initial = root.children.map { it.text }

        input.value = 100
        Snapshot.sendApplyNotifications()
        yield()
        recomposer.awaitIdle()
        val updated = root.children.map { it.text }

        composition.dispose()
        recomposer.cancel()
        recomposerJob.join()

        when {
            initial != listOf("sum=45") -> "initial=$initial"
            updated != listOf("sum=145") -> "updated=$updated"
            else -> "OK"
        }
    }
}
