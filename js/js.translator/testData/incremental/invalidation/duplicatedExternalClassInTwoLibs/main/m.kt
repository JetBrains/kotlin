// KT-91856: lib1 and lib2 both declare the external class p.Event; both libs become dirty in STEP 1
import p.*

fun box(stepId: Int, isWasm: Boolean): String {
    val e: Event? = null
    if (!aUse(null)) return "Fail: aUse"
    if (!bUse(null)) return "Fail: bUse"
    if (e != null) return "Fail: e"
    return "OK"
}
