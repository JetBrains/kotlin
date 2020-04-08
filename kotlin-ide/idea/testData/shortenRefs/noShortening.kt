interface T : Iterable<String>

abstract class C : T {
    fun foo(c: C) {
        if (<selection>c.any { it.size > 1 }</selection>) {}
    }
}