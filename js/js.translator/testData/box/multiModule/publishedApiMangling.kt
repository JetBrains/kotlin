// EXPECTED_REACHABLE_NODES: 1480
// MODULE: lib
// FILE: lib.kt
class A {
    @PublishedApi
    internal fun published(x: String) = "${x}K"

    fun template(x: String): String = TODO("")
}

@PublishedApi
internal fun publishedTopLevel(x: String) = "${x}K"

fun templateTopLevel(x: String): String = TODO("")

interface I {
    fun test(): String
}

@PublishedApi
internal class B(val x: String) : I {
    override fun test() = x + "K"
}

// MODULE: main(lib)
// FILE: main.kt
import kotlin.text.Regex

inline fun templates() {
    A().template("X")
    templateTopLevel("X")
}

fun box(): String {
    val testFunctionName = "published" + extractSuffix("template")
    val a = A()
    var result = a.asDynamic()[testFunctionName].call(a, "O")
    if (result != "OK") return "fail1: $result"

    val topLevelName = "publishedTopLevel" + extractSuffix("templateTopLevel")
    result = js("lib")[topLevelName]("O")
    if (result != "OK") return "fail2: $result"

    val b: I = js("new lib.B('O')")
    result = b.test()
    if (result != "OK") return "fail3: $result"

    return "OK"
}

private fun extractSuffix(prefix: String): String {
    val functionBody: String = js("_").templates.toString()
    val regex = Regex(prefix + "(_[\\\$a-zA-Z0-9]+)")
    return regex.find(functionBody)!!.groupValues[1]
}