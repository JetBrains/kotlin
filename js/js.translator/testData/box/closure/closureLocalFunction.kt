// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1113
package foo


class closureBox<T>(var v: T)



//fun run(r: () -> Any) = r()

fun <T1, T2, T3> wrapper(t1:T1, t2:T2, i1:Int, block: (t1:T1, t2:T2, i1:Int) -> T3): T3 {
    val f = { tt1:T1, tt2:T2, ii:Int -> block(tt1, tt2, ii) }
    return f(t1, t2, i1)
}

fun test(ii: Int, jj: Int, kk: Int): String {

    var i = ii
    var j = jj
    val k = kk
    var ggg: Int

    fun f(): String = "OK"

//    fun f() {}
    val funVal = {
        k
    }

//    run(funVal)

    val funLit = { x:Int, l:Int, a:Int, b:Int, c:Int, d:Int, g:Int -> i += 1
        j += k
        ggg = 333 + x
        f() }
    val ret = funLit(50, 77, 1, 2, 3, 4, 5)
//    funLit(50, 77L)
    if (i != 1 || j != 6 || k != 4) return "fail"
    return ret
//    return "OK"
}

//fun test(aa:Int, bb:Int, cc:Int) : Int {
//    val c = cc
//    var d = 0
//    val f = { a:Int,b:Int -> d = a*b+c}
//    f(aa, bb)
//    return d
//}

fun box(): String {
//    val i_box = closureBox<Int>(99)
    return test(1, 2, 3).toString()
}