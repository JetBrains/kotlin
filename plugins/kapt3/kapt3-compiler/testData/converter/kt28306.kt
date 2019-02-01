// CORRECT_ERROR_TYPES

package foo

interface InterfaceWithDefaults<T> {
    fun foo() {}
}

interface SubInterface<T> : InterfaceWithDefaults<T>