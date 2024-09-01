// KJS_WITH_FULL_RUNTIME
package foo


fun box(): String {
    val a = ArrayList<Int>();
    return if (a.size == 0) "OK" else "fail: ${a.size}"
}