// EXPECTED_REACHABLE_NODES: 492
package foo

external fun run(i:Int, s:String, funRef: (Int, String) -> String): String = definedExternally

fun funTopLevel(i:Int, s:String): String = "funTopLevel $i $s"

fun box(): String {
    fun funLocal(i:Int, s:String): String = "funLocal $i $s"

    // Check for lambda
    var r = run(4, "boo") { i, s -> "$i $s"}
    if (r != "4 boo") return r

    r = run(4, "boo", ::funTopLevel)
    if (r != "funTopLevel 4 boo") return r

    r = run(4, "boo", ::funLocal)
    if (r != "funLocal 4 boo") return r

    return "OK"
}