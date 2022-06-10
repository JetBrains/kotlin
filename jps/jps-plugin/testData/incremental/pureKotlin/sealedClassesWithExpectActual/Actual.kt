package test

actual class ExpectClass {
    actual fun doSmth() = ""
}

expect class ExpectClass2 {
    fun doSmth(): String
}