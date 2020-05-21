/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.util

import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.debugger.coroutine.data.SuspendExitMode
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.invokeLater
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode

const val CREATION_STACK_TRACE_SEPARATOR = "\b\b\b" // the "\b\b\b" is used as creation stacktrace separator in kotlinx.coroutines

fun Method.isInvokeSuspend(): Boolean =
    name() == "invokeSuspend" && signature() == "(Ljava/lang/Object;)Ljava/lang/Object;"

fun Method.isInvoke(): Boolean =
    name() == "invoke" && signature().contains("Ljava/lang/Object;)Ljava/lang/Object;")

fun Method.isSuspendLambda() =
    isInvokeSuspend() && declaringType().isSuspendLambda()

fun Method.hasContinuationParameter() =
    signature().contains("Lkotlin/coroutines/Continuation;)")

fun Method.isResumeWith() =
    name() == "resumeWith" && signature() == "(Ljava/lang/Object;)V" && (declaringType().isSuspendLambda() || declaringType().isContinuation())

fun Location.isPreFlight(): SuspendExitMode {
    val method = safeMethod() ?: return SuspendExitMode.NONE
    if (method.isSuspendLambda())
        return SuspendExitMode.SUSPEND_LAMBDA
    else if (method.hasContinuationParameter())
        return SuspendExitMode.SUSPEND_METHOD_PARAMETER
    else if ((method.isInvokeSuspend() || method.isInvoke()) && safeCoroutineExitPointLineNumber())
        return SuspendExitMode.SUSPEND_METHOD
    return SuspendExitMode.NONE
}

fun Location.safeCoroutineExitPointLineNumber() =
    wrapIllegalArgumentException { DebuggerUtilsEx.getLineNumber(this, false) } ?: -2 == -1

fun ReferenceType.isContinuation() =
    isBaseContinuationImpl() || isSubtype("kotlin.coroutines.Continuation")

fun Type.isBaseContinuationImpl() =
    isSubtype("kotlin.coroutines.jvm.internal.BaseContinuationImpl")

fun Type.isAbstractCoroutine() =
    isSubtype("kotlinx.coroutines.AbstractCoroutine")

fun Type.isSubTypeOrSame(className: String) =
    name() == className || isSubtype(className)

fun ReferenceType.isSuspendLambda() =
    SUSPEND_LAMBDA_CLASSES.any { isSubtype(it) }

fun Location.isInvokeSuspend() =
    safeMethod()?.isInvokeSuspend() ?: false

fun Location.isInvokeSuspendWithNegativeLineNumber() =
    isInvokeSuspend() && safeLineNumber() < 0

fun Location.isFilteredInvokeSuspend() =
    isInvokeSuspend() || isInvokeSuspendWithNegativeLineNumber()

fun StackFrameProxyImpl.variableValue(variableName: String): ObjectReference? {
    val continuationVariable = safeVisibleVariableByName(variableName) ?: return null
    return getValue(continuationVariable) as? ObjectReference ?: return null
}

fun StackFrameProxyImpl.continuationVariableValue(): ObjectReference? =
    variableValue("\$continuation")

fun StackFrameProxyImpl.thisVariableValue(): ObjectReference? =
    this.thisObject()

private fun Method.isGetCoroutineSuspended() =
    signature() == "()Ljava/lang/Object;" && name() == "getCOROUTINE_SUSPENDED" && declaringType().name() == "kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsKt"

fun DefaultExecutionContext.findCoroutineMetadataType() =
    debugProcess.invokeInManagerThread { findClassSafe("kotlin.coroutines.jvm.internal.DebugMetadataKt") }

fun DefaultExecutionContext.findDispatchedContinuationReferenceType(): List<ReferenceType>? =
    vm.classesByName("kotlinx.coroutines.DispatchedContinuation")

fun DefaultExecutionContext.findCancellableContinuationImplReferenceType(): List<ReferenceType>? =
    vm.classesByName("kotlinx.coroutines.CancellableContinuationImpl")

fun hasGetCoroutineSuspended(frames: List<StackFrameProxyImpl>) =
    frames.indexOfFirst { it.safeLocation()?.safeMethod()?.isGetCoroutineSuspended() == true }

fun StackTraceElement.isCreationSeparatorFrame() =
    className.startsWith(CREATION_STACK_TRACE_SEPARATOR)

fun StackTraceElement.findPosition(project: Project): XSourcePosition? =
    getPosition(project, className, lineNumber)

fun Location.findPosition(project: Project) =
    readAction {
        getPosition(project, declaringType().name(), lineNumber())
    }

private fun getPosition(project: Project, className: String, lineNumber: Int): XSourcePosition? {
    val psiFacade = JavaPsiFacade.getInstance(project)
    val psiClass = psiFacade.findClass(
        className.substringBefore("$"), // find outer class, for which psi exists TODO
        GlobalSearchScope.everythingScope(project)
    )
    val classFile = psiClass?.containingFile?.virtualFile
    // to convert to 0-based line number or '-1' to do not move
    val localLineNumber = if (lineNumber > 0) lineNumber - 1 else return null
    return XDebuggerUtil.getInstance().createPosition(classFile, localLineNumber)
}

fun SuspendContextImpl.executionContext() =
    invokeInManagerThread { DefaultExecutionContext(EvaluationContextImpl(this, this.frameProxy)) }

fun <T : Any> SuspendContextImpl.invokeInManagerThread(f: () -> T?): T? =
    debugProcess.invokeInManagerThread { f() }

fun ThreadReferenceProxyImpl.supportsEvaluation(): Boolean =
    threadReference?.isSuspended ?: false

fun SuspendContextImpl.supportsEvaluation() =
    this.debugProcess.canRunEvaluation || isUnitTestMode()

fun XDebugSession.suspendContextImpl() =
    suspendContext as SuspendContextImpl

fun threadAndContextSupportsEvaluation(suspendContext: SuspendContextImpl, frameProxy: StackFrameProxyImpl?) =
    suspendContext.invokeInManagerThread {
        suspendContext.supportsEvaluation() && frameProxy?.threadProxy()?.supportsEvaluation() ?: false
    } ?: false


fun Location.sameLineAndMethod(location: Location?): Boolean =
    location != null && location.safeMethod() == safeMethod() && location.safeLineNumber() == safeLineNumber()

fun Location.isFilterFromTop(location: Location?): Boolean =
    isFilteredInvokeSuspend() || sameLineAndMethod(location) || location?.safeMethod() == safeMethod()

fun Location.isFilterFromBottom(location: Location?): Boolean =
    sameLineAndMethod(location)