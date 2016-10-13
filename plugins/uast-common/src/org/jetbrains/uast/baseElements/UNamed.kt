package org.jetbrains.uast

/**
 * An interface for the [UElement] which has a name.
 */
interface UNamed {
    /**
     * Returns the element name.
     */
    val name: String?

    /**
     * Checks if the element name is equal to the passed name.
     *
     * @param name the name to check against
     * @return true if the element name is equal to [name], false otherwise.
     */
    fun matchesName(name: String) = this.name == name
}