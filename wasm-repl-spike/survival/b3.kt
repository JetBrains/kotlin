// Test: does Pt (declared identically) still unify with module A's Pt when this
// module declares MANY additional unrelated types (a REPL that accumulates
// class declarations grows its type section every generation)?

import kotlin.wasm.WasmImport

open class Pt(val a: Int, val b: Int) {
    open fun sum(): Int = a + b
}

// Extra, unrelated declarations to perturb the type section:
data class Alpha(val s: String)
class Beta<T>(val t: T)
interface Gamma { fun g(): Int }
class Delta : Gamma { override fun g() = 42 }
enum class Eps { A, B, C }
sealed class Zet { class Z1 : Zet(); class Z2 : Zet() }
class More1(val x: Int)
class More2(val x: Int, val y: Int)
class More3(val x: Int, val y: Int, val z: Int)
class More4(val x: Long)
class More5(val x: String)

fun readPtB(p: Pt): Int = p.a * 100 + p.b

fun sumPtB(p: Pt): Int = p.sum() // virtual dispatch on foreign object

fun isPt(x: Any?): Boolean = x is Pt

fun noise(): Int = More5("z").x.length + Delta().g() + Eps.C.ordinal + More3(1,2,3).z + Beta(1).t
