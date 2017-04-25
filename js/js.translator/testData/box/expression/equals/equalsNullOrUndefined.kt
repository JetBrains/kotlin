// EXPECTED_REACHABLE_NODES: 488
package foo

fun box(): String {
    val a: Int? = null
    val r = a == null
    if (!r || a != null)
        return "wrong result on simple nullable check"

    //force using Kotlin.equals
    val t = null
    if (t != undefined)
        return "wrong result when compare null and undefined using Kotlin.equals"

    var i = 0;
    fun foo(): Int? = ++i;
    if (foo() == null)
        return "wrong result on nullable check with side effects"

    if (i != 1)
        return "wrong affects when using nullable check with side effects"

    return "OK"
}