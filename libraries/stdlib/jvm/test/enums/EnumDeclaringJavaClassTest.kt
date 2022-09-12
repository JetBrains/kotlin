/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.enums

import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.DurationUnit

class EnumDeclaringJavaClassTest {

    private enum class TestEnum {
        E
    }

    @Test
    fun testDeclaringClass() {
        // This file
        assertEquals(TestEnum::class.java, TestEnum.E.declaringJavaClass)
        // From Java
        assertEquals(TimeUnit::class.java, TimeUnit.MILLISECONDS.declaringJavaClass)
        // From Kotlin
        assertEquals(DurationUnit::class.java, DurationUnit.MILLISECONDS.declaringJavaClass)
    }

    private inline fun <reified E : Enum<E>> E.declaring() = declaringJavaClass

    @Test
    fun testReified() {
        // This file
        assertEquals(TestEnum::class.java, TestEnum.E.declaring())
        // From Java
        assertEquals(TimeUnit::class.java, TimeUnit.MILLISECONDS.declaring())
    }

    @Test
    fun testEnumSet() {
        val set = EnumSet.noneOf(TestEnum.E.declaringJavaClass)
        set.addAll(TestEnum.E.declaringJavaClass.enumConstants.toList())
        assertEquals(EnumSet.of(TestEnum.E), set)
    }

    @Test
    fun worksOnGenericEnum() {
        fun <T : Enum<T>> check(e: Enum<T>) {
            assertEquals<Class<*>>(e.declaringJavaClass, TestEnum::class.java)
        }
        check(TestEnum.E)
    }
}
