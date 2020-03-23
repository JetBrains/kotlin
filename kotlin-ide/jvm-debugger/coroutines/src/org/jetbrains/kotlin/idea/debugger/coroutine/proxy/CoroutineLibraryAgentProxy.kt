/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.jdi.ClassesByNameProvider
import com.intellij.debugger.jdi.GeneratedLocation
import com.intellij.util.containers.ContainerUtil
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.data.*
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.CoroutineContext
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.CoroutineInfo
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.DebugProbesImpl
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.JavaLangMirror
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class CoroutineLibraryAgentProxy(private val debugProbesClsRef: ClassType, private val executionContext: DefaultExecutionContext) :
    CoroutineInfoProvider {
    private val coroutineContextReference: JavaLangMirror = JavaLangMirror(executionContext)

    private val debugProbesImplClsRef = executionContext.findClass("$DEBUG_PACKAGE.internal.DebugProbesImpl") as ClassType
    private val debugProbesImplInstance = with(debugProbesImplClsRef) { getValue(fieldByName("INSTANCE")) as ObjectReference }
    private val enhanceStackTraceWithThreadDumpRef: Method = debugProbesImplClsRef
        .methodsByName("enhanceStackTraceWithThreadDump").single()

    private val dumpMethod: Method = debugProbesClsRef.concreteMethodByName("dumpCoroutinesInfo", "()Ljava/util/List;")
    val instance = with(debugProbesClsRef) { getValue(fieldByName("INSTANCE")) as ObjectReference }

    // CoroutineInfo
    private val coroutineInfoClsRef = executionContext.findClass("$DEBUG_PACKAGE.CoroutineInfo") as ClassType

    private val getStateRef: Method = coroutineInfoClsRef.concreteMethodByName("getState", "()Lkotlinx/coroutines/debug/State;")
    private val getContextRef: Method = coroutineInfoClsRef.concreteMethodByName("getContext", "()Lkotlin/coroutines/CoroutineContext;")
    private val lastObservedStackTraceRef: Method = coroutineInfoClsRef.methodsByName("lastObservedStackTrace").single()

    private val sequenceNumberFieldRef: Field = coroutineInfoClsRef.fieldByName("sequenceNumber")
    private val lastObservedThreadFieldRef: Field = coroutineInfoClsRef.fieldByName("lastObservedThread")
    private val lastObservedFrameFieldRef: Field = coroutineInfoClsRef.fieldByName("lastObservedFrame") // continuation

    // value
    private val vm = executionContext.vm
    private val classesByName = ClassesByNameProvider.createCache(vm.allClasses())


    private val coroutineContext: CoroutineContext = CoroutineContext(executionContext)

    @Synchronized
    @Suppress("unused")
    fun install() =
        executionContext.invokeMethodAsVoid(instance, "install")

    @Synchronized
    @Suppress("unused")
    fun uninstall() =
        executionContext.invokeMethodAsVoid(instance, "uninstall")

    override fun dumpCoroutinesInfo(): List<CoroutineInfoData> {
        val coroutinesInfo = executionContext.invokeMethodAsObject(instance, dumpMethod) ?: return emptyList()
        executionContext.keepReference(coroutinesInfo)
        val size = coroutineContextReference.sizeOf(coroutinesInfo, executionContext)

        return MutableList(size) {
            val elem = coroutineContextReference.elementFromList(coroutinesInfo, it, executionContext)
            fetchCoroutineState(elem)
        }
    }

    private fun fetchCoroutineState(instance: ObjectReference): CoroutineInfoData {
        val name = getName(instance)
        val state = getState(instance)
        val thread = getLastObservedThread(instance, lastObservedThreadFieldRef)
        val lastObservedFrameFieldRef = instance.getValue(lastObservedFrameFieldRef) as? ObjectReference
        val stackTrace = getStackTrace(instance)
        val creationFrameSeparatorIndex = findCreationFrameIndex(stackTrace)
        val coroutineStackTrace = stackTrace.take(creationFrameSeparatorIndex)

        val coroutineStackTraceFrameItems = coroutineStackTrace.map {
            SuspendCoroutineStackFrameItem(it, createLocation(it))
        }
        val creationStackTrace = stackTrace.subList(creationFrameSeparatorIndex + 1, stackTrace.size)
        val creationStackTraceFrameItems = creationStackTrace.map {
            CreationCoroutineStackFrameItem(it, createLocation(it))
        }
        val key = CoroutineNameIdState(name, "", State.valueOf(state), "")

        return CoroutineInfoData(
            key,
            coroutineStackTraceFrameItems.toMutableList(),
            creationStackTraceFrameItems,
            thread,
            lastObservedFrameFieldRef
        )
    }


    private fun createLocation(stackTraceElement: StackTraceElement): Location = findLocation(
        ContainerUtil.getFirstItem(classesByName[stackTraceElement.className]),
        stackTraceElement.methodName,
        stackTraceElement.lineNumber
    )

    private fun findLocation(
        type: ReferenceType?,
        methodName: String,
        line: Int
    ): Location {
        if (type != null && line >= 0) {
            try {
                val location = type.locationsOfLine(DebugProcess.JAVA_STRATUM, null, line).stream()
                    .filter { l: Location -> l.method().name() == methodName }
                    .findFirst().orElse(null)
                if (location != null) {
                    return location
                }
            } catch (ignored: AbsentInformationException) {
            }
        }
        return GeneratedLocation(executionContext.debugProcess, type, methodName, line)
    }

    /**
     * Tries to find creation frame separator if any, returns last index if none found
     */
    private fun findCreationFrameIndex(frames: List<StackTraceElement>): Int {
        val index = frames.indexOfFirst { it.isCreationSeparatorFrame() }
        return if (index < 0)
            frames.lastIndex
        else
            index
    }

    private fun getName(
        info: ObjectReference // CoroutineInfo instance
    ): String {
        // equals to `coroutineInfo.context.get(CoroutineName).name`
        val coroutineContextInst = executionContext.invokeMethod(
            info,
            getContextRef,
            emptyList()
        ) as? ObjectReference ?: throw IllegalArgumentException("Coroutine context must not be null")
        val context = coroutineContext.mirror(coroutineContextInst, executionContext)
        val name = context?.name ?: "coroutine"
        val id = (info.getValue(sequenceNumberFieldRef) as LongValue).value()
        return "$name#$id"
    }

    private fun getState(
        info: ObjectReference // CoroutineInfo instance
    ): String {
        // equals to `stringState = coroutineInfo.state.toString()`
        val state = executionContext.invokeMethod(info, getStateRef, emptyList()) as ObjectReference
        return coroutineContextReference.string(state, executionContext)
    }

    private fun getLastObservedThread(
        info: ObjectReference, // CoroutineInfo instance
        threadRef: Field // reference to lastObservedThread
    ): ThreadReference? = info.getValue(threadRef) as? ThreadReference

    /**
     * Returns list of stackTraceElements for the given CoroutineInfo's [ObjectReference]
     */
    private fun getStackTrace(
        info: ObjectReference
    ): List<StackTraceElement> {
        val frameList = lastObservedStackTrace(info)
//        val tmpList = mutableListOf<StackTraceElement>()
//        val sizeOfFrameList = coroutineContextReference.sizeOf(frameList, executionContext)
//        for (it in 0 until sizeOfFrameList) {
//            val frame = coroutineContextReference.elementFromList(frameList, it, executionContext)
//            val ste = coroutineContextReference.stackTraceElement(frame)
//            tmpList.add(ste)
//        }
        val mergedFrameList = enhanceStackTraceWithThreadDump(listOf(info, frameList))
        val sizeOfMergedFrameList = coroutineContextReference.sizeOf(mergedFrameList, executionContext)

        val list = mutableListOf<StackTraceElement>()

        for (it in 0 until sizeOfMergedFrameList) {
            val frame = coroutineContextReference.elementFromList(mergedFrameList, it, executionContext)
            val ste = coroutineContextReference.stackTraceElement(frame)
            list.add(// 0, // add in the beginning // @TODO what's the point?
                ste
            )
        }
        return list
    }

    private fun lastObservedStackTrace(instance: ObjectReference) =
        executionContext.invokeMethod(instance, lastObservedStackTraceRef, emptyList()) as ObjectReference

    private fun enhanceStackTraceWithThreadDump(args: List<ObjectReference>) =
        executionContext.invokeMethod(
            debugProbesImplInstance,
            enhanceStackTraceWithThreadDumpRef, args
        ) as ObjectReference

    companion object {
        private const val DEBUG_PACKAGE = "kotlinx.coroutines.debug"

        fun instance(executionContext: DefaultExecutionContext): CoroutineLibraryAgentProxy? {
            try {
                val debugProbesClsRef = executionContext.findClass("$DEBUG_PACKAGE.DebugProbes") ?: return null
                if (debugProbesClsRef is ClassType) {
                    val instanceField = debugProbesClsRef.fieldByName("INSTANCE")
                    val debugProbes = debugProbesClsRef.getValue(instanceField) as ObjectReference
                    val f = debugProbes.referenceType().fieldByName("isInstalled")
                    val debugProbesActivated = if (f != null) (debugProbes.getValue(f) as BooleanValue).value() else true
                    if (debugProbesActivated)
                        return CoroutineLibraryAgentProxy(debugProbesClsRef, executionContext)
                }
            } catch (e: EvaluateException) {
            }
            return null
        }
    }

}
