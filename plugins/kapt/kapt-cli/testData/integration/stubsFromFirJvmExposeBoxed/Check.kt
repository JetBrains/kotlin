import java.io.File

fun main() {
    // Only the JVM backend generates the boxed variant; the FIR generator writes no stub for the mangled function.
    val stub = File("output/stubs/test/TestKt.java").readText()
    require("topLevel(" in stub) { "No boxed variant of topLevel in the stub:\n$stub" }
}
