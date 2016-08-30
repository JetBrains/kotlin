// FILE: enumerable.kt
package foo

@native
fun <T> _enumerate(o: T): T = noImpl

@native
fun <T> _findFirst(o: Any): T = noImpl

class Test() {
    val a: Int = 100
    val b: String = "s"
}

class P() {
    @enumerable
    val a: Int = 100
    val b: String = "s"
}

fun box(): String {
    val test = _enumerate(Test())
    val p = _enumerate(P())
    if (100 != test.a) return "fail1: ${test.a}"
    if ("s" != test.b) return "fail2: ${test.b}"
    if (p.a != 100) return "fail3: ${p.a}"

    val result = _findFirst<Int>(object {
        val test = 100
    })
    if (result != 100) return "fail4: $result"

    return "OK"
}

// FILE: enumerate.js
function _enumerate(o) {
    var r = {};
    for (var p in o) {
        r[p] = o[p];
    }
    return r;
}

function _findFirst(o) {
    for (var p in o) {
        return o[p];
    }
}