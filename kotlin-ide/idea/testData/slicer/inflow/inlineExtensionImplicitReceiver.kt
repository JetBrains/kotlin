// FLOW: IN
// RUNTIME_WITH_SOURCES

class C

fun foo() {
    val c = C().apply {
        extensionFun()
    }
}

fun <caret>C.extensionFun() {}
