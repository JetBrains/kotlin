@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class)

// Minimal Compose milestone: a runtime-only composition (no UI toolkit,
// no skiko) driven from JS. The Harness OBJECT is the defvar state
// holder — the live SlotTable (remembered values), the Recomposer and
// the frame clock all survive the app's own redefinitions via adoption.
// App bodies swap through the slots; the group keys the compose
// compiler derives decide whether remembered state maps to the same
// slots across a body edit — the Live Edit hard problem, observable here.
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Applier
import androidx.compose.runtime.AbstractApplier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.wasm.WasmExport

// the applier's node: composition content is rendered into a text log
class LogNode

class LogApplier : AbstractApplier<LogNode>(LogNode()) {
    override fun insertTopDown(index: Int, instance: LogNode) {}
    override fun insertBottomUp(index: Int, instance: LogNode) {}
    override fun remove(index: Int, count: Int) {}
    override fun move(from: Int, to: Int, count: Int) {}
    override fun onClear() {}
}

object Harness {
    val frameClock = BroadcastFrameClock()
    val scope = CoroutineScope(Dispatchers.Unconfined + frameClock)
    val revision = mutableStateOf(0)             // __hot_invalidate bumps this
    val observed = mutableStateOf("<unset>")     // App renders into here
    var rememberedRef: androidx.compose.runtime.MutableState<Int>? = null

    private val recomposer = Recomposer(scope.coroutineContext)
    private val composition = Composition(LogApplier(), recomposer)

    init {
        scope.launch { recomposer.runRecomposeAndApplyChanges() }
        composition.setContent { App() }
    }

    private var frameTime = 0L
    fun frame() {
        frameClock.sendFrame(++frameTime)  // resumes awaiting recomposition
    }
}

var rendered = ""

@Composable
fun App() {
    // the revision read puts the root scope in the invalidation set:
    // bumping it (via __hot_invalidate) recomposes, running the NEW body
    val r = Harness.revision.value
    val n = remember { androidx.compose.runtime.mutableStateOf(0).also { Harness.rememberedRef = it } }
    rendered = "render#${r}: remembered=${n.value}"
    Harness.observed.value = rendered
}

@WasmExport("boot")
fun boot(): Int {
    Harness.frame()   // run the initial composition
    return Harness.revision.value
}

@WasmExport("bumpRemembered")
fun bumpRemembered(): Int {
    val ref = Harness.rememberedRef ?: return -1
    ref.value += 1       // normal state change: recomposition, body unchanged
    Harness.frame()
    return ref.value
}

@WasmExport("readRendered")
fun readRendered(): String = Harness.observed.value

@WasmExport("__hot_invalidate")
fun hotInvalidate(): Int {
    Harness.revision.value += 1
    Harness.frame()
    return Harness.revision.value
}

@WasmExport("bumpLoop")
fun bumpLoop(n: Int): Int {
    var r = 0
    var i = 0
    while (i < n) { r = bumpRemembered(); i++ }
    return r
}

@WasmExport("invLoop")
fun invLoop(n: Int): Int {
    val inv = ::hotInvalidate
    var r = 0
    var i = 0
    while (i < n) { r = inv(); i++ }
    return r
}
