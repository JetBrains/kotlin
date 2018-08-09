/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.collections

import org.junit.Assume
import org.junit.Test
import kotlin.test.*

class IndexOverflowJVMTest {

    @BeforeTest
    fun checkIsNotIgnored() {
        Assume.assumeTrue(System.getProperty("kotlin.stdlib.test.long.sequences")?.toBoolean() ?: false)
    }


    companion object {
        fun <T> repeatCounted(value: T, count: Long = Int.MAX_VALUE + 1L): Sequence<T> = Sequence {
            object : Iterator<T> {
                var counter = count
                override fun hasNext(): Boolean = counter > 0
                override fun next(): T = value.also { counter-- }
            }
        }

        val maxIndexSequence = repeatCounted("k", (Int.MAX_VALUE + 1L) + 1L) // here the last index is one greater than Int.MAX_VALUE
        val maxIndexIterable = maxIndexSequence.asIterable()


        val longCountSequence = Sequence {
            object : Iterator<Long> {
                var counter = 0L
                override fun hasNext(): Boolean = true
                override fun next(): Long = counter++
            }
        }
    }




}