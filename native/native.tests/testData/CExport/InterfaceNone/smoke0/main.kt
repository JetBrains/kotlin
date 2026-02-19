import kotlin.native.internal.ExportedBridge

@ExportedBridge("my_function")
fun myFunction(value: Int): Int = value