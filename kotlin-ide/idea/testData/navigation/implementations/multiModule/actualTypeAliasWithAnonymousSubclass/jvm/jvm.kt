package test

actual interface Closable {
    fun close()
}

actual typealias MyStream = MyImpl

fun foo(): Any? = object : MyStream() {}
