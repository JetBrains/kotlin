/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package test.coroutines

import com.google.gson.Gson
import kotlin.test.Test
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.jvm.internal.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@DebugMetadata(
    sourceFile = "test.kt",
    lineNumbers = [10, 122, 11],
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

    override fun invokeSuspend(result: Result<Any?>): Any? = null
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
            StackTraceElement("SomeClass", "testMethod", "test.kt", 122),
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

    @Test
    fun testGetStackTraceInfoAsJsonAndReferences() {
        val myContinuation = MyContinuation()
        val stackTraceInfo = myContinuation.getStackTraceInfoAsJsonAndReferences()
        assertEquals(stackTraceInfo.size, 4)

        val continuationRefs = stackTraceInfo[0]
        val stackTraceElementsAsJson = stackTraceInfo[1]
        val spilledVariableFieldMappingsAsJson = stackTraceInfo[2]
        val nextContinuationRefs = stackTraceInfo[3]
        assertTrue(continuationRefs is Array<*>)
        assertTrue(stackTraceElementsAsJson is String)
        assertTrue(spilledVariableFieldMappingsAsJson is String)
        assertTrue(nextContinuationRefs is Array<*>)

        val gson = Gson()
        val stackTraceElements = gson.fromJson(stackTraceElementsAsJson, Array<StackTraceElementInfo>::class.java)
        val spilledVariableFieldMappings = gson.fromJson(
            spilledVariableFieldMappingsAsJson, Array<Array<FieldVariable>>::class.java
        )

        var i = 0
        var continuation: BaseContinuationImpl? = myContinuation
        while (continuation != null) {
            val nextContinuation = continuation.completion as? BaseContinuationImpl
            val element = continuation.getStackTraceElement()
            if (element != null) {
                assertEquals(continuation, continuationRefs[i])
                assertEquals(nextContinuation, nextContinuationRefs[i])
                checkStackTraceElements(element, stackTraceElements[i])
                checkSpilledVariableFieldMapping(continuation, spilledVariableFieldMappings[i])
            }

            continuation = nextContinuation
            i++
        }

        assertEquals(i, continuationRefs.size)
        assertEquals(i, stackTraceElements.size)
        assertEquals(i, spilledVariableFieldMappings.size)
        assertEquals(i, nextContinuationRefs.size)
    }

    private fun checkStackTraceElements(element: StackTraceElement, elementInfo: StackTraceElementInfo) {
        assertEquals(element.className, elementInfo.className)
        assertEquals(element.methodName, elementInfo.methodName)
        assertEquals(element.fileName, elementInfo.fileName)
        assertEquals(element.lineNumber, elementInfo.lineNumber)
    }

    private fun checkSpilledVariableFieldMapping(continuation: BaseContinuationImpl, mapping: Array<FieldVariable>) {
        val currentMapping = continuation.getSpilledVariableFieldMapping()!!
        val length = currentMapping.size / 2
        assertEquals(length, mapping.size)
        for (i in 0 until length) {
            assertEquals(currentMapping[2 * i], mapping[i].fieldName)
            assertEquals(currentMapping[2 * i + 1], mapping[i].variableName)
        }
    }

    private data class FieldVariable(val fieldName: String, val variableName: String)

    private data class StackTraceElementInfo(
        val className: String,
        val methodName: String,
        val fileName: String?,
        val lineNumber: Int?
    )
}
