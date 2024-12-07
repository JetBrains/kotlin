/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.enums

@SinceKotlin("1.9")
public sealed interface EnumEntries<E : Enum<E>>

@PublishedApi
internal external fun <T : Enum<T>> enumEntriesIntrinsic(): EnumEntries<T>

@PublishedApi
@SinceKotlin("1.8")
internal fun <E : Enum<E>> enumEntries(entries: Array<E>): EnumEntries<E> = EnumEntriesList(entries)

@SinceKotlin("1.8")
private class EnumEntriesList<E : Enum<E>>(val entries: Array<E>) : EnumEntries<E>