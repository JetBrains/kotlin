package kotlin

// CHECK_NOT_REFERENCED: Kotlin.isInstanceOf
// CHECK_NOT_REFERENCED: Kotlin.isTypeOf
// CHECK_NOT_REFERENCED: Kotlin.orNull

fun success(message: String, fn: ()->Unit) {
    try {
        fn()
    }
    catch (e: Exception) {
        throw Exception("Exception was thrown: message=$message, exception=$e")
    }
}

fun failsClassCast(message: String, fn: ()->Unit) {
    try {
        fn()
    }
    catch (e: ClassCastException) {
        return
    }

    throw Exception("Expected ClassCastException to be thrown: message=$message")
}