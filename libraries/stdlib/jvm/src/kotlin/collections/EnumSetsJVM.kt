/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmName("EnumSetsKt")

package kotlin.collections

@kotlin.internal.InlineOnly
public actual inline fun <reified E : Enum<E>> enumSetOf(): EnumSet<E> = java.util.EnumSet.noneOf(E::class.java)

@kotlin.internal.InlineOnly
public actual inline fun <reified E : Enum<E>> enumSetOf(element: E): EnumSet<E> = java.util.EnumSet.of(element)

@kotlin.internal.InlineOnly
public actual inline fun <reified E : Enum<E>> enumSetOf(vararg elements: E): EnumSet<E> =
    elements.toCollection(java.util.EnumSet.noneOf(E::class.java))

@kotlin.internal.InlineOnly
public actual inline fun <reified E : Enum<E>> enumSetAllOf(): EnumSet<E> = java.util.EnumSet.allOf(E::class.java)
