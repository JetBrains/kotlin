/*
 * Copyright in here? call @mirromutth on github
 */

@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin.collections

@kotlin.internal.InlineOnly
public actual inline fun <reified T : Enum<T>> enumSetOf(): EnumSet<T> = java.util.EnumSet.noneOf(T::class.java)

@kotlin.internal.InlineOnly
public actual inline fun <reified T : Enum<T>> enumSetOf(vararg elements: T): EnumSet<T> = elements.toCollection(java.util.EnumSet.noneOf(T::class.java))

@kotlin.internal.InlineOnly
public actual inline fun <reified T : Enum<T>> enumSetAllOf(): EnumSet<T> = java.util.EnumSet.allOf(T::class.java)
