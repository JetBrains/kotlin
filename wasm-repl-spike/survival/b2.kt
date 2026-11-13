import kotlin.wasm.WasmExport

// "Generation B" — same class shape as a.kt, compiled completely separately.



fun readPtB(p: Pt): Int = p.b



fun sumPtB(p: Pt): Long = p.a + p.b

open class Pt(val a: Long, val b: Int) {
    open fun sum(): Long = a + b
}
