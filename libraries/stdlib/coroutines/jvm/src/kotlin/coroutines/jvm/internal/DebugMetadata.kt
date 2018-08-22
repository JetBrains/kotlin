/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.jvm.internal

import kotlin.coroutines.Continuation

// TODO: Uncomment when KT-25372 is fixed
@Target(AnnotationTarget.CLASS)
@SinceKotlin("1.3")
internal annotation class DebugMetadata(
    // @JvmName("r")
    val sourceFiles: Array<String>,
    // @JvmName("l")
    val lineNumbers: IntArray,
    // @JvmName("n")
    val localNames: Array<String>,
    // @JvmName("s")
    val spilled: Array<String>,
    // @JvmName("i")
    val indexToLabel: IntArray,
    // @JvmName("m")
    val methodName: String,
    // @JvmName("c")
    val className: String
)

/**
 * Returns [StackTraceElement] containing file name and line number of current coroutine's suspension point.
 * The coroutine can be either running coroutine, that calls the function on its continuation and obtaining
 * the information about current file and line number, or, more likely, the function is called to produce accurate stack traces of
 * suspended coroutine.
 */
@SinceKotlin("1.3")
@JvmName("getStackTraceElement")
internal fun BaseContinuationImpl.getStackTraceElement(): StackTraceElement? {
    val debugMetadata = getDebugMetadataAnnotation()
    val label = getLabel()
    val fileName = if (label < 0) "" else debugMetadata.sourceFiles[label]
    val lineNumber = if (label < 0) -1 else debugMetadata.lineNumbers[label]
    return StackTraceElement(debugMetadata.className, debugMetadata.methodName, fileName, lineNumber)
}

private fun BaseContinuationImpl.getDebugMetadataAnnotation(): DebugMetadata {
    return javaClass.annotations.filterIsInstance<DebugMetadata>()[0]
}

private fun BaseContinuationImpl.getLabel(): Int {
    val field = javaClass.getDeclaredField("label") ?: return -1
    field.isAccessible = true
    return (field.get(this) as? Int ?: 0) - 1
}

/**
 * Returns an array of spilled variable names and continuation's field names where the variable has been spilled.
 * The structure is the following:
 * - field names take 2*k'th indices
 * - corresponding variable names take (2*k + 1)'th indices.
 *
 * The function is for debugger to use, thus it returns simplest data type possible.
 * This function should only be called on suspended coroutines to get accurate mapping.
 */
@SinceKotlin("1.3")
internal fun BaseContinuationImpl.getSpilledVariableFieldMapping(): Array<String> {
    val debugMetadata = getDebugMetadataAnnotation()
    val res = arrayListOf<String>()
    val label = getLabel()
    for ((i, labelOfIndex) in debugMetadata.indexToLabel.withIndex()) {
        if (labelOfIndex == label) {
            res.add(debugMetadata.spilled[i])
            res.add(debugMetadata.localNames[i])
        }
    }
    return res.toTypedArray()
}