// LANGUAGE: +FullValueClasses
// DUMP_KT_IR

// MODULE: lib
// FILE: Test.kt
import androidx.compose.runtime.Composable

value class FullName(val s: String)

inline value class InlineName(val s: String)

value class FullBox<T>(val value: T)

inline value class InlineBox<T>(val value: T)

value class FullPrivate private constructor(val i: Int) {
    companion object {
        fun default() = FullPrivate(0)
    }
}

inline value class InlinePrivate private constructor(val i: Int) {
    companion object {
        fun default() = InlinePrivate(0)
    }
}

value class FullUnstable(val list: MutableList<Int>)

inline value class InlineUnstable(val list: MutableList<Int>)

val log = mutableListOf<String>()

@Composable fun Names(full: FullName = FullName("a"), inline: InlineName = InlineName("b")) {
    log += "Names($full, $inline)"
}

@Composable fun Nullable(full: FullName? = null, inline: InlineName? = null) {
    log += "Nullable($full, $inline)"
}

@Composable fun Boxes(full: FullBox<Int> = FullBox(1), inline: InlineBox<Int> = InlineBox(2)) {
    log += "Boxes($full, $inline)"
}

@Composable fun Private(full: FullPrivate = FullPrivate.default(), inline: InlinePrivate = InlinePrivate.default()) {
    log += "Private($full, $inline)"
}

@Composable fun Unstable(full: FullUnstable, inline: InlineUnstable) {
    log += "Unstable($full, $inline)"
}

// MODULE: main(lib)
// FILE: main.kt
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlin.coroutines.EmptyCoroutineContext

@Composable fun Test(full: FullName, inline: InlineName) {
    Names()
    Names(full, inline)
    Nullable()
    Nullable(full, inline)
    Boxes()
    Boxes(FullBox(3), InlineBox(4))
    Private()
    Unstable(FullUnstable(mutableListOf(5)), InlineUnstable(mutableListOf(6)))
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
    composition.setContent { Test(FullName("c"), InlineName("d")) }

    val expected = listOf(
        "Names(FullName(s=a), InlineName(s=b))",
        "Names(FullName(s=c), InlineName(s=d))",
        "Nullable(null, null)",
        "Nullable(FullName(s=c), InlineName(s=d))",
        "Boxes(FullBox(value=1), InlineBox(value=2))",
        "Boxes(FullBox(value=3), InlineBox(value=4))",
        "Private(FullPrivate(i=0), InlinePrivate(i=0))",
        "Unstable(FullUnstable(list=[5]), InlineUnstable(list=[6]))",
    )
    return if (log == expected) "OK" else "Fail: $log"
}
