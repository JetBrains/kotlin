actual fun commonMainExpect(): String = "Hello from Kotlin/Native: ${nativeMainExpect()}"

expect fun nativeMainExpect(): String
