/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import kotlin.test.*

class ExtrasPropertyTest {


    class Subject : HasMutableExtras {
        override val extras: MutableExtras = mutableExtrasOf()
    }

    class Dummy

    private val keyA = extrasKeyOf<Int>("a")
    private val keyB = extrasKeyOf<Int>("b")

    private val Subject.readA: Int? by keyA.readProperty
    private val Subject.readB: Int? by keyB.readProperty

    private var Subject.readWriteA: Int? by keyA.readWriteProperty
    private var Subject.readWriteB: Int? by keyB.readWriteProperty

    private val Subject.notNullReadA: Int by keyA.readProperty.notNull(1)
    private val Subject.notNullReadB: Int by keyB.readProperty.notNull(2)

    private var Subject.notNullReadWriteA: Int by keyA.readWriteProperty.notNull(3)
    private var Subject.notNullReadWriteB: Int by keyB.readWriteProperty.notNull(4)

    private val keyList = extrasKeyOf<MutableList<Dummy>>()
    private val Subject.factoryList: MutableList<Dummy> by keyList.factoryProperty { mutableListOf() }

    private val keySubjectList = extrasKeyOf<MutableList<Subject>>()
    private val Subject.lazyList: MutableList<Subject> by keySubjectList.lazyProperty { mutableListOf(this) }

    private val lazyNullStringInvocations = mutableListOf<Subject>()
    private val Subject.lazyNullString: String? by extrasNullableLazyProperty("null") {
        lazyNullStringInvocations.add(this)
        null
    }

    private val lazyNullableStringInvocations = mutableListOf<Subject>()
    private val Subject.lazyNullableString: String? by extrasNullableLazyProperty("not-null") {
        lazyNullableStringInvocations.add(this)
        "OK"
    }

    @Test
    fun `test - readOnlyProperty`() {
        val subject = Subject()
        assertNull(subject.readA)
        assertNull(subject.readB)

        subject.readWriteA = 1
        assertEquals(1, subject.readA)
        assertNull(subject.readB)

        subject.readWriteB = 2
        assertEquals(1, subject.readA)
        assertEquals(2, subject.readB)
    }

    @Test
    fun `test - readWriteProperty`() {
        val subject = Subject()
        assertNull(subject.readWriteA)
        assertNull(subject.readWriteB)

        subject.readWriteA = 1
        assertEquals(1, subject.readWriteA)
        assertNull(subject.readB)

        subject.readWriteB = 2
        assertEquals(1, subject.readWriteA)
        assertEquals(2, subject.readWriteB)
    }

    @Test
    fun `test - readOnlyProperty - notNull`() {
        val subject = Subject()
        assertEquals(1, subject.notNullReadA)
        assertEquals(2, subject.notNullReadB)

        subject.readWriteA = -1
        assertEquals(-1, subject.notNullReadA)
        assertEquals(2, subject.notNullReadB)

        subject.readWriteB = -2
        assertEquals(-1, subject.notNullReadA)
        assertEquals(-2, subject.notNullReadB)
    }

    @Test
    fun `test - readWriteProperty - notNull`() {
        val subject = Subject()
        assertEquals(3, subject.notNullReadWriteA)
        assertEquals(4, subject.notNullReadWriteB)

        subject.notNullReadWriteA = -1
        assertEquals(-1, subject.notNullReadWriteA)
        assertEquals(4, subject.notNullReadWriteB)

        subject.notNullReadWriteB = -2
        assertEquals(-1, subject.notNullReadWriteA)
        assertEquals(-2, subject.notNullReadWriteB)
    }

    @Test
    fun `test - factoryProperty`() {
        run {
            val subject = Subject()
            assertNotNull(subject.factoryList)
            assertSame(subject.factoryList, subject.factoryList)
            assertSame(subject.extras[keyList], subject.factoryList)
        }

        run {
            val subject = Subject()
            val list = mutableListOf(Dummy())
            subject.extras[keyList] = list
            assertSame(list, subject.factoryList)
        }
    }


    @Test
    fun `test - lazyProperty`() {
        run {
            val subject = Subject()
            assertNotNull(subject.lazyList)
            assertSame(subject.lazyList, subject.lazyList)
            assertSame(subject.extras[keySubjectList], subject.lazyList)
            assertSame(subject, subject.lazyList.firstOrNull())

            val subject2 = Subject()
            assertSame(subject2, subject2.lazyList.firstOrNull())
            assertNotSame(subject.lazyList.firstOrNull(), subject2.lazyList.firstOrNull())
        }

        run {
            val subject = Subject()
            val list = mutableListOf<Subject>()
            subject.extras[keySubjectList] = list
            assertSame(list, subject.lazyList)
        }
    }

    @Test
    fun `test - lazyNullableProperty`() {
        val subject1 = Subject()
        val subject2 = Subject()

        assertNull(subject1.lazyNullString)
        assertNull(subject1.lazyNullString)
        assertEquals(listOf(subject1), lazyNullStringInvocations)

        assertNull(subject2.lazyNullString)
        assertNull(subject2.lazyNullString)
        assertEquals(listOf(subject1, subject2), lazyNullStringInvocations)


        assertEquals("OK", subject1.lazyNullableString)
        assertEquals("OK", subject1.lazyNullableString)
        assertEquals(listOf(subject1), lazyNullableStringInvocations)

        assertEquals("OK", subject2.lazyNullableString)
        assertEquals("OK", subject2.lazyNullableString)
        assertEquals(listOf(subject1, subject2), lazyNullableStringInvocations)
    }
}
