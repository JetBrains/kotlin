// MODULE: lib
import org.jetbrains.kotlin.plugin.sandbox.EmitMetadata

@EmitMetadata(5)
class Some

// MODULE: main(lib)
import org.jetbrains.kotlin.plugin.sandbox.GenerateBodyUsingEmittedMetadata

@GenerateBodyUsingEmittedMetadata
fun test(s: Some): Int = -1

fun box(): String {
    val x = test(Some())
    return when (x) {
        5 -> "OK"
        else -> "Error: $x"
    }
}
