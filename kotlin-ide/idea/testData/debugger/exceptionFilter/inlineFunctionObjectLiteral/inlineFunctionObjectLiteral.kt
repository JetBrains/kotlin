package inlineFunctionObjectLiteral

fun box() {
    t {
        val a = object {
            fun test() {
                foo {
                    val b = 1
                }
            }
        }
        a.test()
    }
}

inline fun t(f: () -> Unit) {
    f()
}

// MAIN_CLASS: inlineFunctionObjectLiteral.InlineFunctionObjectLiteralKt
// FILE: inlineFunctionFile.kt
// LINE: 4