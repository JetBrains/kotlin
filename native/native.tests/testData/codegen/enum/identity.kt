import kotlin.test.*
import kotlin.native.concurrent.*

enum class A {
    A, B
}

data class Foo(val kind: A)

// Enums are shared between threads so identity should be kept.
fun box(): String = withWorker {
    val result = execute(TransferMode.SAFE, { Foo(A.B) }) { input ->
        input.kind === A.B
    }.result
    return if (result) "OK" else "FAIL"
}