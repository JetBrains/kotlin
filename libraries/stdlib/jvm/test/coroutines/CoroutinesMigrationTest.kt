/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("ObsoleteExperimentalCoroutines")

package test.coroutines.experimental.migration

import kotlin.coroutines.experimental.buildSequence as experimentalBuildSequence
import kotlin.coroutines.experimental.SequenceBuilder as ExperimentalSequenceBuilder
import kotlin.coroutines.experimental.Continuation as ExperimentalContinuation
import kotlin.coroutines.experimental.CoroutineContext as ExperimentalCoroutineContext
import kotlin.coroutines.experimental.AbstractCoroutineContextElement as ExperimentalAbstractCoroutineContextElement
import kotlin.coroutines.experimental.EmptyCoroutineContext as ExperimentalEmptyCoroutineContext
import kotlin.coroutines.experimental.ContinuationInterceptor as ExperimentalContinuationInterceptor
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED as EXPERIMENTAL_COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.migration.*
import kotlin.coroutines.*
import kotlin.test.*

/**
 * Tests on coroutines migrations utilities.
 */
class CoroutinesMigrationTest {
    @Test
    fun testContextMigration() {
        assertTrue(EmptyCoroutineContext === ExperimentalEmptyCoroutineContext.toCoroutineContext())
        assertTrue(ExperimentalEmptyCoroutineContext === EmptyCoroutineContext.toExperimentalCoroutineContext())
        MyElement().let { e ->
            assertTrue(e === e.toExperimentalCoroutineContext().toCoroutineContext())
            val ee = MyExperimentalElement()
            val ctx = (e.toExperimentalCoroutineContext() + ee).toCoroutineContext()
            assertTrue(e === ctx[MyElement.Key])
            assertTrue(ee === ctx.toExperimentalCoroutineContext()[MyExperimentalElement.Key])
        }
        MyExperimentalElement().let { ee ->
            assertTrue(ee === ee.toCoroutineContext().toExperimentalCoroutineContext())
            val e = MyElement()
            val ctx = (ee.toCoroutineContext() + e).toExperimentalCoroutineContext()
            assertTrue(ee === ctx[MyExperimentalElement.Key])
            assertTrue(e === ctx.toCoroutineContext()[MyElement.Key])
        }
    }

    class MyElement : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<MyElement>
    }

    class MyExperimentalElement : ExperimentalAbstractCoroutineContextElement(Key) {
        companion object Key : ExperimentalCoroutineContext.Key<MyExperimentalElement>
    }
}

