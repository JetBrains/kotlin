// DUMP_KT_IR

// MODULE: lib
// FILE: Test.kt
import androidx.compose.runtime.Composable

value class Test private constructor(val i: Int) {
    companion object {
        fun default() = Test(0)
    }
}

@Composable fun Content(test: Test = Test.default()) {}

// MODULE: main(lib)
// FILE: main.kt
import androidx.compose.runtime.Composable

@Composable fun Test() {
    Content()
}

fun box() = "OK"
