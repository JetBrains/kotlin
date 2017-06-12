// EXPECTED_REACHABLE_NODES: 491
package foo

// test String template must have one or more entries.
public class Fe {
    fun open(method: String, url: String, async: Boolean = true, user: String = "", password: String = "") = "$method $url $async $user $password"
}

fun box(): String {
    val a = "abc"
    val b = "def"
    val message = "a = $a, b = $b"

    if (message != "a = abc, b = def") return "fail"

    val v1 = null
    if ("returns null null" != "returns $v1 ${null}") return "fail"

    return if (Fe().open("22", "33") == "22 33 true  ") "OK" else "fail"
}