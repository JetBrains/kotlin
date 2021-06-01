fun <T> test1() = null as T

fun <T> test2(): T {
    val a : Any? = null
    return a as T
}

fun <T: Any> test3() = null as T

fun <T> castToString(t: T) {
    t as String
}

fun box(): String {
    if (test1<Int?>() != null) return "fail: test1"
    if (test2<Int?>() != null) return "fail: test2"
    var result3 = "fail"
    try {
        test3<Int>()
    }
    catch(e: NullPointerException) {
        result3 = "OK"
    }
    if (result3 != "OK") return "fail: test3"

    try {
        castToString<Any?>(null)
    } catch (e: Exception) {
        return "OK"
    }
    return "Fail"
}
