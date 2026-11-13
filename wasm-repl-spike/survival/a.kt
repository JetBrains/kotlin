import kotlin.wasm.WasmExport

// "Generation A" — identical class declaration text as b.kt (modulo exported fn names).



fun makePt(a: Int, b: Int): Pt = Pt(a, b)



fun readPtA(p: Pt): Int = p.a * 100 + p.b



fun sumPtA(p: Pt): Int = p.sum()

open class Pt(val a: Int, val b: Int) {
    open fun sum(): Int = a + b
}
