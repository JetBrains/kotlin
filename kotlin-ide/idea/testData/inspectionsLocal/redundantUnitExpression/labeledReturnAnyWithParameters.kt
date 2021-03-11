// PROBLEM: none

fun foo(f: (Unit, Int, String) -> Any) {}

fun test() {
    foo { _, _, _ ->
        return@foo Unit<caret>
    }
}