// WITH_STDLIB
// DUMP_IR
// ISSUE: KT-84935
import org.jetbrains.kotlin.plugin.sandbox.AllOpen2

@AllOpen2
enum class Direction {
    NORTH, SOUTH;
}

@AllOpen2
class Outer {
    enum class Nested {
        ENTRY;
    }
}

fun box(): String {
    if (Direction.entries != listOf(Direction.NORTH, Direction.SOUTH)) return "Fail: Direction"
    if (Outer.Nested.entries.single() != Outer.Nested.ENTRY) return "Fail: Nested"
    return "OK"
}
