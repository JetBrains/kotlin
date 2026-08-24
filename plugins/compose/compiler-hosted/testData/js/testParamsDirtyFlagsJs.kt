// DUMP_KT_IR

// MODULE: lib
// FILE: lib.kt
import androidx.compose.runtime.Composable

@Composable
fun Leaf(a: Int, b: String, c: Double = 0.0) {
    println("$a $b $c")
}

// MODULE: main(lib)
// FILE: main.kt
import androidx.compose.runtime.Composable

@Composable
fun App(a: Int, b: String) {
    Leaf(a, b)
}

fun box(): String = "OK"
