// COMPILER_ARGUMENTS: -Xopt-in=kotlin.RequiresOptIn
@file:OptIn(Experimental::class)

@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class Experimental

object MyObject

@Experimental
operator fun MyObject.invoke(closure: () -> Unit) {}

fun d() = <caret>MyObject {
}