// IGNORE_BACKEND: JVM_IR

package test

internal annotation class MyAnnotation

@MyAnnotation
internal class State @JvmOverloads constructor(
        val someInt: Int,
        val someLong: Long,
        val someString: String = ""
)
