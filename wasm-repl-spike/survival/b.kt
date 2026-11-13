import kotlin.wasm.WasmExport

// "Generation B" — same class shape as a.kt, compiled completely separately.



fun readPtB(p: Pt): Int = p.a * 100 + p.b



fun sumPtB(p: Pt): Int = p.sum() // virtual dispatch on an object created by module A

open class Pt(val a: Int, val b: Int) {
    open fun sum(): Int = a + b
}

fun isPt(x: Any?): Boolean = x is Pt
