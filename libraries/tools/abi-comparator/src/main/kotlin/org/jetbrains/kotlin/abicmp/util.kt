@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.abicmp

inline fun <reified T> Any?.cast() =
    this as T

inline fun <reified T> Any?.safeCast() =
    this as? T

inline fun <reified T : Any> List<Any?>?.listOfNotNull() =
    orEmpty().filterIsInstance<T>()