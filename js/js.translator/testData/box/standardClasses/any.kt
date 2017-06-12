// EXPECTED_REACHABLE_NODES: 493
// CHECK_CALLED_IN_SCOPE: function=isType scope=box
package foo

class A : Any()

fun Any?.asAny() = this

fun box(): String {
    val x = Any().asAny()
    if (x !is Any) return "fail1"
    if (x.asDynamic().constructor !== js("Object")) return "fail1a"

    if (A().asAny() !is Any) return "fail2"

    if (arrayOf(1, 2, 3).asAny() !is Any) return "fail3"

    if (createNakedObject() is Any) return "fail4"

    if (({ }).asAny() !is Any) return "fail5"

    if ((23).asAny() !is Any) return "fail6"

    if ((3.14).asAny() !is Any) return "fail7"

    if (false.asAny() !is Any) return "fail8"

    if ("bar".asAny() !is Any) return "fail9"

    return "OK"
}

fun createNakedObject(): Any? = js("Object.create(null)")