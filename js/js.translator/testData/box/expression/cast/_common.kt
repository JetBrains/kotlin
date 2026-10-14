package kotlin

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

fun failsNullPointer(message: String, fn: ()->Unit) {
    try {
        fn()
    }
    catch (e: NullPointerException) {
        return
    }

    throw Exception("Expected NullPointerException to be thrown: message=$message")
}
