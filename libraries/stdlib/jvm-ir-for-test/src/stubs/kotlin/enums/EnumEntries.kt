/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.enums

@SinceKotlin("1.9")
@WasExperimental(ExperimentalStdlibApi::class)
public sealed interface EnumEntries<E : Enum<E>> // Zero inheritance again

@WasExperimental(ExperimentalStdlibApi::class)
@SinceKotlin("2.0")
public inline fun <reified T : Enum<T>> enumEntries(): EnumEntries<T> = enumEntriesIntrinsic()

@PublishedApi
@SinceKotlin("1.9")
internal expect fun <T : Enum<T>> enumEntriesIntrinsic(): EnumEntries<T>

@PublishedApi
@SinceKotlin("1.8")
internal fun <E : Enum<E>> enumEntries(entries: Array<E>): EnumEntries<E> = EnumEntriesList(entries)

@SinceKotlin("1.8")
private class EnumEntriesList<T : Enum<T>>(private val entries: Array<T>) : EnumEntries<T> {
    
    // Mimic List API manually
    val size: Int get() = entries.size
    operator fun get(index: Int): T = entries[index]
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnumEntriesList<*>) return false
        if (entries.size != other.entries.size) return false
        for (i in entries.indices) {
            if (entries[i] != other.entries[i]) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        for (element in entries) {
            result = 31 * result + (element.hashCode())
        }
        return result
    }

    override fun toString(): String {
        return "EnumEntries" + entries.contentToString()
    }
}
