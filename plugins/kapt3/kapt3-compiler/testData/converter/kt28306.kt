// CORRECT_ERROR_TYPES
// NO_VALIDATION

@file:Suppress("UNRESOLVED_REFERENCE")
package foo

interface InterfaceWithDefaults<T> : Error1, Error2 {
    fun foo() {}

    interface Nested<S> : Error1, Error2 {
        fun bar() {}
    }
}

interface SubInterface<T> : InterfaceWithDefaults<T>
