open class Class {
    fun String.test(): String = "O"
    val String.test: String
        get() = "K"
}

// This case is only relevant for the JS Legacy BE and is not applicable to the JS IR backend,
// as the IR BE can resolve such name collisions.
@Suppress("JS_FAKE_NAME_CLASH")
class MyClass1 : Class()

fun box(): String {
    val s = ""
    return with(MyClass1()) {
        s.test() + s.test
    }
}
