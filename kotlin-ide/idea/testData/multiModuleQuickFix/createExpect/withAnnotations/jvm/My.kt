// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

annotation class PlatformAnnotation

actual class <caret>My {
    @PlatformAnnotation
    actual tailrec fun foo(arg: Int): Int {
        if (arg <= 1) return 1
        return foo(arg - 1)
    }

    // Here we will have an error (lateinit is not supported on both sides)
    actual lateinit var some: Boolean

    @CommonAnnotation
    actual fun initialize() {
        some = true
    }
}