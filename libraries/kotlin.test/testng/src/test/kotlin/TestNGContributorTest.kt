/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.testng.tests

import org.testng.Assert
import kotlin.test.*
import java.util.concurrent.*
import kotlin.test.testng.TestNGAsserter

class TestNGContributorTest {

    @Test
    fun smokeTest() {
        assertSame(TestNGAsserter, asserter)
        Assert.assertEquals(TestNGAsserter::class.java.simpleName, kotlin.test.asserter.javaClass.simpleName)
    }

    @Test
    fun parallelThreadGetsTheSameAsserter() {
        val q = ArrayBlockingQueue<Any>(1)

        Thread {
            q.put(asserter)
        }.start()

        assertSame(asserter, q.take())
    }

}