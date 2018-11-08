// CORRECT_ERROR_TYPES
// NO_VALIDATION
// WITH_RUNTIME

@file:Suppress("UNRESOLVED_REFERENCE")
class Foo {
    suspend fun a(): ABC = TODO()

    suspend fun b(): Result<ABC> = TODO()
}