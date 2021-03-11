// FIR_COMPARISON
import kotlin.internal.Exact

interface Column<out V>

inline fun <reified V : Enum<V>> String.enumeration(name: String): Column<V> = TODO()

fun <T> foo(c: Column<@Exact T>) { }


fun test() {
    foo("".enu<caret>)
}

// EXIST: enumeration
