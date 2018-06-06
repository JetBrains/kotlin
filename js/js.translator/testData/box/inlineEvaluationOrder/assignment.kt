// EXPECTED_REACHABLE_NODES: 1120
package foo

//class A(var x: Int) {
//    override fun toString(): String = "A($x)"
//}

inline fun foo(block: () -> String) = block() + "0"

inline fun run(block: () -> Unit) {
    block()
}

//fun bar(x: Int) = x
//
//var s = foo { "1" }
//
//inline var b
//    get() = a * 10
//    set(c) {
//        a = c * 20
//    }

fun box(): String {

//    val q = b
//    b = 1
//
    var x = 2
//
    var s = ""
//
    run {
        while (true) {
            s = "$s$x"
            x = x - 1
            if (x <= 0) return@run
        }
    }

//    s = foo {
//        "${x}1"
//    }

    if (s != "21") return "fail: $s"

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