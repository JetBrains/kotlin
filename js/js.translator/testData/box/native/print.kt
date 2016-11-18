// SKIP_NODE_JS
package foo

val EXPECTED = """Hello, World
^^
^^
^^
***
####
"""

val EXPECTED_NEWLINE_FOR_EACH = """Hello
, World

^^
^^
^^

***
##
##

"""

@native
var buffer: String = noImpl

fun test(expected: String, initCode: String, getResult: () -> String) {
    buffer = ""

    eval("kotlin.out = new $initCode")

    print("Hello")
    print(", World")
    print("\n^^\n^^\n^^")
    println()
    println("***")
    print("##")
    print("##")
    println()

    val actual = getResult()

    assertEquals(expected, actual, initCode)
}

fun box(): String {
    test(EXPECTED, "kotlin.NodeJsOutput(outputStream)") {
        buffer
    }

    test(EXPECTED_NEWLINE_FOR_EACH, "kotlin.OutputToConsoleLog()") {
        buffer
    }

    test(EXPECTED, "kotlin.BufferedOutput()") {
        eval("kotlin.out.buffer") as String
    }

    test(EXPECTED, "kotlin.BufferedOutputToConsoleLog()") {
        buffer
    }

    return "OK"
}