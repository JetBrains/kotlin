// EXPECTED_REACHABLE_NODES: 490
package foo

external fun nativeFun(i:Int, s:String): String = definedExternally

fun bar(funRef: (Int, String) -> String): String = funRef(4, "boo")

fun box(): String {
    var r = nativeFun(4, "boo")
    if (r != "nativeFun 4 boo") return r

    r = bar(::nativeFun)
    if (r != "nativeFun 4 boo") return r

    r = (::nativeFun)(4, "boo")
    if (r != "nativeFun 4 boo") return r

    return "OK"
}
