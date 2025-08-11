// DUMP_KT_IR

// MODULE: lib
// FILE: Test.kt
value class Test private constructor(val i: Int) {
    companion object {
        fun default() = Test(0)
    }
}

// MODULE: main(lib)
// FILE: main.kt
import androidx.compose.runtime.Composable

@Composable fun Content(test: Test = Test.default()) {}

fun box() = "OK"
