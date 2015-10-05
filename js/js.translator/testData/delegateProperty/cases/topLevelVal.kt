package foo

class Delegate {
    fun getValue(t: Any?, p: PropertyMetadata): Int = 1
}

val prop: Int by Delegate()

fun box(): String {
    return if (prop == 1) "OK" else "fail"
}
