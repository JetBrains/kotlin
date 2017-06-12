// SKIP_MINIFICATION
// This test uses eval
// SKIP_NODE_JS
package foo

val EXPECTED = """Hello, World
^^
^^
^^
***
##null23##
"""

val EXPECTED_NEWLINE_FOR_EACH = """Hello
, World

^^
^^
^^

***
##
null
23
##

"""

external var buffer: String = definedExternally

fun test(expected: String, initCode: String, getResult: () -> String) {
    buffer = ""

    eval("kotlin.kotlin.io.output = new $initCode")

    print("Hello")
    print(", World")
    print("\n^^\n^^\n^^")
    println()
    println("***")
    print("##")
    print(null)
    print(23)
    print("##")
    println()

    val actual = getResult()

    assertEquals(expected, actual, initCode)
}

fun box(): String {
    test(EXPECTED, "kotlin.kotlin.io.NodeJsOutput(outputStream)") {
        buffer
    }

    test(EXPECTED_NEWLINE_FOR_EACH, "kotlin.kotlin.io.OutputToConsoleLog()") {
        buffer
    }

    test(EXPECTED, "kotlin.kotlin.io.BufferedOutput()") {
        eval("kotlin.kotlin.io.output.buffer") as String
    }

    test(EXPECTED, "kotlin.kotlin.io.BufferedOutputToConsoleLog()") {
        buffer
    }

    return "OK"
}