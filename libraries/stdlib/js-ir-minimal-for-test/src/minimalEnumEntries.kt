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
internal fun <E : Enum<E>> enumEntries(entries: Array<E>): EnumEntries<E> = EnumEntriesList(entries)

@SinceKotlin("1.8")
@ExperimentalStdlibApi
private class EnumEntriesList<E : Enum<E>>(val entries: Array<E>) : EnumEntries<E>