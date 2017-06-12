// EXPECTED_REACHABLE_NODES: 511
package foo

interface A
interface B : A
interface C : A
interface D : B, C
interface E : C

open class CB : B
class CB2 : CB()

class CD : D

fun testPhrase(o: Any): String {
    var s = ""
    s += if (o is A) "Y" else "N"
    s += if (o is B) "Y" else "N"
    s += if (o is C) "Y" else "N"
    s += if (o is D) "Y" else "N"
    s += if (o is E) "Y" else "N"
    return s
}

fun box(): String {
    val b = CB()
    val b2 = CB2()
    val d = CD()
    val e = object : E {
    }

    if (testPhrase(b) != "YYNNN") return "bad b, it: ${testPhrase(b)}"
    if (testPhrase(b2) != "YYNNN") return "bad b2, it: ${testPhrase(b2)}"
    if (testPhrase(d) != "YYYYN") return "bad d, it: ${testPhrase(d)}"
    if (testPhrase(e) != "YNYNY") return "bad e, it: ${testPhrase(e)}"

    return "OK"
}