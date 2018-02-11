package test

/**
 * KDoc comment.
 */
@Suppress("UNRESOLVED_REFERENCE")
internal class Simple {
    @MyAnnotation
    fun myMethod() {
        // do nothing
    }

    fun heavyMethod(): Int {
        return if (true) 5 else 6
    }
}

/*
Multi
line
comment
 */
internal annotation class MyAnnotation

// Small comment
internal enum class EnumClass {
    BLACK, WHITE
}


internal enum class EnumClass2 private constructor(private val blah: String) {
    WHITE("A"), RED("B")
}