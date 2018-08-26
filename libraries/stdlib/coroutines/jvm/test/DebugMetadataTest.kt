/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package test.kotlin.coroutines

import org.junit.Test
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.jvm.internal.*
import kotlin.test.assertEquals

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@DebugMetadata(
    sourceFiles = ["test.kt", "test1.kt", "test.kt"],
    lineNumbers = [10, 2, 11],
    indexToLabel = [0, 0, 1, 1, 2],
    localNames = ["a", "b", "b", "c", "c"],
    spilled = ["L$1", "L$2", "L$1", "L$2", "L$1"],
    methodName = "testMethod",
    className = "SomeClass"
)
private class MyContinuation : BaseContinuationImpl(null) {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    var label = 0

    override fun invokeSuspend(result: SuccessOrFailure<Any?>): Any? = null
}

class DebugMetadataTest {
    @Test
    fun testRuntimeDebugMetadata() {
        val myContinuation = MyContinuation()

        myContinuation.label = 1
        assertEquals(
            StackTraceElement("SomeClass", "testMethod", "test.kt", 10),
            myContinuation.getStackTraceElement()
        )
        assertEquals(listOf("L$1", "a", "L$2", "b"), myContinuation.getSpilledVariableFieldMapping()!!.toList())
        myContinuation.label = 2
        assertEquals(
            StackTraceElement("SomeClass", "testMethod", "test1.kt", 2),
            myContinuation.getStackTraceElement()
        )
        assertEquals(listOf("L$1", "b", "L$2", "c"), myContinuation.getSpilledVariableFieldMapping()!!.toList())
        myContinuation.label = 3
        assertEquals(
            StackTraceElement("SomeClass", "testMethod", "test.kt", 11),
            myContinuation.getStackTraceElement()
        )
        assertEquals(listOf("L$1", "c"), myContinuation.getSpilledVariableFieldMapping()!!.toList())
    }
}
