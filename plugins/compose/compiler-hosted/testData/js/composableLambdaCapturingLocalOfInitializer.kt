// A composable lambda reading a local of an `init` block or a property initializer captures it, so it
// must not be moved into ComposableSingletons, where the local is out of scope

// DUMP_KT_IR

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlin.coroutines.EmptyCoroutineContext

private class InitHolder(label: String) {
    val content: @Composable (StringBuilder) -> Unit

    init {
        val local = label
        content = { out -> out.append(local) }
    }
}

private class PropertyHolder(label: String) {
    val content: @Composable (StringBuilder) -> Unit = when (val local = label) {
        else -> { out -> out.append(local) }
    }
}

private val topLevelContent: @Composable (StringBuilder) -> Unit = when (val local = "t") {
    else -> { out -> out.append(local) }
}

class NoOpApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) {}
    override fun insertBottomUp(index: Int, instance: Unit) {}
    override fun remove(index: Int, count: Int) {}
    override fun move(from: Int, to: Int, count: Int) {}
    override fun onClear() {}
}

fun box(): String {
    val out = StringBuilder()
    Composition(NoOpApplier(), Recomposer(EmptyCoroutineContext)).setContent {
        InitHolder("a").content(out)
        InitHolder("b").content(out)
        PropertyHolder("c").content(out)
        PropertyHolder("d").content(out)
        topLevelContent(out)
    }
    return if (out.toString() == "abcdt") "OK" else "out=$out"
}
