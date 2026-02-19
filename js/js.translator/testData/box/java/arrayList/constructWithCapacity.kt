// KJS_WITH_FULL_RUNTIME
package foo


fun box(): String {
    val al = ArrayList<Int>(10)
    return if (al.size == 0) "OK" else "fail: ${al.size}"
}