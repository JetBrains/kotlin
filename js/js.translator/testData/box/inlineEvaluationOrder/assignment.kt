// EXPECTED_REACHABLE_NODES: 1120
package foo

//class A(var x: Int) {
//    override fun toString(): String = "A($x)"
//}

inline fun foo(block: () -> String) = block() + "0"

inline fun run(block: () -> Unit) {
    block()
}

fun box(): String {

    var x = 2

    var s = ""

//    foo {
//        while (true) {
//            s = "$s$x"
//            x = x - 1
//            if (x <= 0) return@foo
//        }
//    }

    s = foo {
        "${x}1"
    }

    if (s != "210") return "fail"

//    loop@ do {
//        if (x-- == 5) {
//            continue@loop
//        }
//
//    } while (x-- > 0)


//    val a = A(10)
//    fizz(a).x = buzz(20)
//    assertEquals(20, a.x)
//    assertEquals("fizz(A(10));buzz(20);", pullLog())


    return "OK"
}