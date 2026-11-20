package org.jetbrains.kdoc

/**
 * KDoc of a class from a dependency.
 */
class DocumentedClass {
    /**
     * KDoc of a member function from a dependency.
     */
    fun documentedFunction() = Unit

    /**
     * KDoc of a member property from a dependency.
     */
    val documentedProperty: Int = 42
}

enum class DocumentedEnum {
    /**
     * KDoc of an enum entry from a dependency.
     */
    ENTRY
}

interface DocumentedInterface

var DocumentedInterface.documentedExtension: Int
    /**
     * KDoc of a getter from a dependency.
     */
    get() = 42
    /**
     * KDoc of a setter from a dependency.
     */
    set(value) {}
