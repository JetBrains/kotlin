data class SomeName(val a: Int, val b: Int, val c: String)

fun foo(f: SomeName) {
    f.<caret>copy(2, c = "")
}