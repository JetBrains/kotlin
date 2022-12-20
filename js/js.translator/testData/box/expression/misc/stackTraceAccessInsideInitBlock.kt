// EXPECTED_REACHABLE_NODES: 1462
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JS
// KT-55315

var stackTrace: String = ""

open class MyOwnException1: Throwable("Test message 1") {
    init {
        stackTrace = asDynamic().stack
    }
}

class MyOwnException2: MyOwnException1() {
    init {
        stackTrace = asDynamic().stack
    }
}

fun box(): String {
    try {
        throw MyOwnException1()
    } catch (e: Throwable) {
       if (!stackTrace.contains("MyOwnException1: Test message 1\n")) return "fail"
    }
    try {
        throw MyOwnException2()
    } catch (e: Throwable) {
        if (!stackTrace.contains("MyOwnException2: Test message 1\n")) return "fail"
    }
    return "OK"
}