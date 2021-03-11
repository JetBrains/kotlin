/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.openapi.util.registry.Registry
import com.sun.jdi.Field
import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.CancellableContinuationImpl
import org.jetbrains.kotlin.idea.debugger.coroutine.util.findCancellableContinuationImplReferenceType
import org.jetbrains.kotlin.idea.debugger.coroutine.util.findCoroutineMetadataType
import org.jetbrains.kotlin.idea.debugger.coroutine.util.findDispatchedContinuationReferenceType
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class CoroutineNoLibraryProxy(private val executionContext: DefaultExecutionContext) : CoroutineInfoProvider {
    private val log by logger
    private val debugMetadataKtType = executionContext.findCoroutineMetadataType()
    private val holder = ContinuationHolder.instance(executionContext)

    override fun dumpCoroutinesInfo(): List<CoroutineInfoData> {
        val vm = executionContext.vm
        val resultList = mutableListOf<CoroutineInfoData>()
        if (vm.virtualMachine.canGetInstanceInfo()) {
            when (coroutineSwitch()) {
                "DISPATCHED_CONTINUATION" -> dispatchedContinuation(resultList)
                "CANCELLABLE_CONTINUATION" -> cancellableContinuation(resultList)
                else -> dispatchedContinuation(resultList)
            }
        } else
            log.warn("Remote JVM doesn't support canGetInstanceInfo capability (perhaps JDK-8197943).")
        return resultList
    }

    private fun cancellableContinuation(resultList: MutableList<CoroutineInfoData>): Boolean {
        val dcClassTypeList = executionContext.findCancellableContinuationImplReferenceType()
        if (dcClassTypeList?.size == 1) {
            val dcClassType = dcClassTypeList.first()
            val cci = CancellableContinuationImpl(executionContext)

            val continuationList = dcClassType.instances(maxCoroutines())
            for (cancellableContinuation in continuationList) {
                val coroutineInfo = extractCancellableContinuation(cancellableContinuation, cci) ?: continue
                resultList.add(coroutineInfo)
            }
        }
        return false
    }

    private fun extractCancellableContinuation(
        dispatchedContinuation: ObjectReference,
        ccMirrorProvider: CancellableContinuationImpl
    ): CoroutineInfoData? {
        val mirror = ccMirrorProvider.mirror(dispatchedContinuation, executionContext) ?: return null
        val continuation = mirror.delegate?.continuation ?: return null
        return holder.extractCoroutineInfoData(continuation)
    }

    private fun dispatchedContinuation(resultList: MutableList<CoroutineInfoData>): Boolean {
        val dcClassTypeList = executionContext.findDispatchedContinuationReferenceType()
        if (dcClassTypeList?.size == 1) {
            val dcClassType = dcClassTypeList.first()
            val continuationField = dcClassType.fieldByName("continuation") ?: return true
            val continuationList = dcClassType.instances(maxCoroutines())
            for (dispatchedContinuation in continuationList) {
                val coroutineInfo = extractDispatchedContinuation(dispatchedContinuation, continuationField) ?: continue
                resultList.add(coroutineInfo)
            }
        }
        return false
    }

    private fun extractDispatchedContinuation(dispatchedContinuation: ObjectReference, continuation: Field): CoroutineInfoData? {
        debugMetadataKtType ?: return null
        val initialContinuation = dispatchedContinuation.getValue(continuation) as ObjectReference
        return holder.extractCoroutineInfoData(initialContinuation)
    }
}

fun maxCoroutines() = Registry.intValue("kotlin.debugger.coroutines.max", 1000).toLong()

fun coroutineSwitch() = Registry.stringValue("kotlin.debugger.coroutines.switch")
