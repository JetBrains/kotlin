package org.jetbrains.kotlin.objcexport.testUtils

internal fun <T> List<T>.second(): T {
    if (size < 2)
        throw NoSuchElementException("No second element in List")
    return this[1]
}