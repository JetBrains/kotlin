/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE") // for building kotlin-stdlib-jvm-minimal-for-test

package kotlin.enums

import java.io.Serializable

@Suppress("UNCHECKED_CAST", "unused")
internal class EnumEntriesSerializationProxy<E : Enum<E>>(entries: Array<E>) : Serializable {
    private val c: Class<E> = entries.javaClass.componentType!! as Class<E>

    private companion object {
        private const val serialVersionUID: Long = 0L
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun readResolve(): Any {
        return enumEntries(c.enumConstants)
    }
}
