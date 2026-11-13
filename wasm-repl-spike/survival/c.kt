// Shape-changed Pt (v2: extra field). Same class NAME, different rec group.
// Does an old (v1) instance pass `is Pt` here? Does it pass the typed boundary?

open class Pt(val a: Int, val b: Int, val c: Int) {
    open fun sum(): Int = a + b + c
}

fun isPtV2(x: Any?): Boolean = x is Pt

fun readPtV2(p: Pt): Int = p.a * 1000 + p.c

fun isAnySafe(x: Any?): Boolean = x is Any
