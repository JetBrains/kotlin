/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.enums

@ExperimentalStdlibApi
@SinceKotlin("1.8")
public sealed interface EnumEntries<E : Enum<E>>

@PublishedApi
@ExperimentalStdlibApi
@SinceKotlin("1.8")
internal fun <E : Enum<E>> enumEntries(entriesProvider: () -> Array<E>): EnumEntries<E> = EnumEntriesList(entriesProvider)

@SinceKotlin("1.8")
@ExperimentalStdlibApi
private class EnumEntriesList<E : Enum<E>>(val entriesProvider: () -> Array<E>) : EnumEntries<E>
