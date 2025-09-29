fun foo(): Int = 1

fun main() {
    println("Hello from Wasi")
}

@WasmExport
fun dummy() {}