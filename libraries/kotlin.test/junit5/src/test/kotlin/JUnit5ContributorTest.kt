/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.junit5.tests

import org.junit.jupiter.api.Assertions
import kotlin.test.*
import java.util.concurrent.*
import kotlin.test.junit5.JUnit5Asserter

class JUnit5ContributorTest {
    @Test
    fun smokeTest() {
        assertSame(JUnit5Asserter, kotlin.test.asserter)
        Assertions.assertEquals(JUnit5Asserter::class.java.simpleName, kotlin.test.asserter.javaClass.simpleName)
    }

    @Test
    fun parallelThreadGetsTheSameAsserter() {
        val q = ArrayBlockingQueue<Any>(1)

        Thread {
            q.put(asserter)
        }.start()

        assertSame(kotlin.test.asserter, q.take())
    }

}
