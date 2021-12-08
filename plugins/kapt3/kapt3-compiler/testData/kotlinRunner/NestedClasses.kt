// IGNORE_BACKEND: JVM_IR

package test

internal class Simple {
    @MyAnnotation
    fun myMethod() {}

    class NestedClass {
        class NestedNestedClass
    }

    inner class InnerClass
    companion object
}

internal annotation class MyAnnotation
