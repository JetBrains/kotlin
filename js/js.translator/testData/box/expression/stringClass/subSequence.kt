// EXPECTED_REACHABLE_NODES: 500
package foo

class CC(val s: CharSequence) : CharSequence by s, MyCharSequence {}

interface MyCharSequence {
    val length: Int

    operator fun get(index: Int): Char

    fun subSequence(startIndex: Int, endIndex: Int): CharSequence
}

fun box(): String {
    val kotlin: String = "kotlin"

    if (kotlin.subSequence(0, kotlin.length) != kotlin) return "Fail 0"

    val kot: CharSequence = kotlin.subSequence(0, 3)
    if (kot.toString() != "kot") return "Fail 1: $kot"

    val tlin = (kotlin as CharSequence).subSequence(2, 6)
    if (tlin.toString() != "tlin") return "Fail 2: $tlin"

    val cc: CharSequence = CC(kotlin)
    if (cc.length != 6) return "Fail 3: ${cc.length}"
    if (cc.subSequence(0, 3) != kot) return "Fail 4"
    if (cc[2] != 't') return "Fail 5: ${cc[2]}"

    val mcc: MyCharSequence = CC(kotlin)
    if (mcc.length != 6) return "Fail 6: ${mcc.length}"
    if (mcc.subSequence(0, 3) != kot) return "Fail 7"
    if (mcc[2] != 't') return "Fail 8: ${mcc[2]}"

    val ccc = CC(cc)
    if (ccc.length != 6) return "Fail 6: ${ccc.length}"
    if (ccc.subSequence(0, 3) != kot) return "Fail 7"
    if (ccc[2] != 't') return "Fail 8: ${ccc[2]}"

    return "OK"
}
