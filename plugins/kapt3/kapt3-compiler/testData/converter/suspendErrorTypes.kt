// IGNORE_BACKEND: JVM_IR
// CORRECT_ERROR_TYPES
// NO_VALIDATION
// WITH_STDLIB

@file:Suppress("UNRESOLVED_REFERENCE")
class Foo {
    suspend fun a(): ABC = TODO()

    suspend fun b(): Result<ABC> = TODO()
}
