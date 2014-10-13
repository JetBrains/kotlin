package foo

val EXPECTED = """Hello, World

***
####
"""

val EXPECTED_NEWLINE_FOR_EACH = """Hello
, World



***
##
##

"""

native
fun eval(code: String): Any? = noImpl

native
var buffer: String = noImpl

fun test(expected: String, initCode: String, getResult: () -> String) {
    buffer = ""

    eval("Kotlin.out = new $initCode")

    print("Hello")
    print(", World")
    print("\n")
    println()
    println("***")
    print("##")
    print("##")
    println()

    val actual = getResult()

    assertEquals(expected, actual, initCode)
}

fun box(): String {
    test(EXPECTED, "Kotlin.NodeJsOutput(outputStream)") {
        buffer
    }

    test(EXPECTED_NEWLINE_FOR_EACH, "Kotlin.OutputToConsoleLog()") {
        buffer
    }

    test(EXPECTED, "Kotlin.BufferedOutput()") {
        eval("Kotlin.out.buffer") as String
    }

    test(EXPECTED, "Kotlin.BufferedOutputToConsoleLog()") {
        buffer
    }

    return "OK"
}