// LANGUAGE: +FullValueClasses
// DUMP_KT_IR

// MODULE: lib
// FILE: Test.kt
import androidx.compose.runtime.Composable

value class Full(val i: Int)

inline value class Inline(val i: Int)

val log = mutableListOf<String>()

@Composable fun Content(full: Full = Full(1), inline: Inline = Inline(2)) {
    log += "Content($full, $inline)"
}

@Composable fun Show(full: Full, inline: Inline) {
    log += "Show($full, $inline)"
}

// MODULE: main(lib)
// FILE: main.kt
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlin.coroutines.EmptyCoroutineContext

@Composable fun Test(full: Full, inline: Inline) {
    Content()
    Content(Full(3))
    Content(inline = Inline(4))
    Content(full, inline)
    Show(Full(7), Inline(8))
    Show(full, inline)
}

object UnitApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) {}
    override fun insertBottomUp(index: Int, instance: Unit) {}
    override fun remove(index: Int, count: Int) {}
    override fun move(from: Int, to: Int, count: Int) {}
    override fun onClear() {}
}

fun box(): String {
    val composition = Composition(UnitApplier, Recomposer(EmptyCoroutineContext))
    composition.setContent { Test(Full(5), Inline(6)) }

    val expected = listOf(
        "Content(Full(i=1), Inline(i=2))",
        "Content(Full(i=3), Inline(i=2))",
        "Content(Full(i=1), Inline(i=4))",
        "Content(Full(i=5), Inline(i=6))",
        "Show(Full(i=7), Inline(i=8))",
        "Show(Full(i=5), Inline(i=6))",
    )
    return if (log == expected) "OK" else "Fail: $log"
}
