// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/jsCode/noJavaScriptProduced.fir.kt
// WITH_STDLIB
fun test() {
    js("")
    js(" ")
    js("""
               """)

    val empty = ""
    js(empty)

    val whitespace = "  "
    js(whitespace)

    val multiline = """
    """
    js(multiline)
}


fun box() = "OK"
