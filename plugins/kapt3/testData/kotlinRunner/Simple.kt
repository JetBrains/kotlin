package test

internal class Simple {
    @MyAnnotation
    fun myMethod() {
        // do nothing
    }
}

internal annotation class MyAnnotation

internal enum class EnumClass {
    BLACK, WHITE
}


internal enum class EnumClass2 private constructor(private val blah: String) {
    WHITE("A"), RED("B")
}