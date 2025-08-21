/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.jvm.internal

import java.lang.reflect.Method

@Target(AnnotationTarget.CLASS)
@SinceKotlin("1.3")
@PublishedApi
internal annotation class DebugMetadata(
    @get:JvmName("v")
    val version: Int = COROUTINES_DEBUG_METADATA_VERSION_2_2,
    @get:JvmName("f")
    val sourceFile: String = "",
    @get:JvmName("l")
    val lineNumbers: IntArray = [],
    @get:JvmName("n")
    val localNames: Array<String> = [],
    @get:JvmName("s")
    val spilled: Array<String> = [],
    @get:JvmName("i")
    val indexToLabel: IntArray = [],
    @get:JvmName("m")
    val methodName: String = "",
    @get:JvmName("c")
    val className: String = "",
    @SinceKotlin("2.2")
    @get:JvmName("nl")
    val nextLineNumbers: IntArray = [],
)

/**
 * Returns [StackTraceElement] containing file name and line number of current coroutine's suspension point.
 * The coroutine can be either running coroutine, that calls the function on its continuation and obtaining
 * the information about current file and line number, or, more likely, the function is called to produce accurate stack traces of
 * suspended coroutine.
 *
 * The result is `null` when debug metadata is not available.
 */
@SinceKotlin("1.3")
@JvmName("getStackTraceElement")
@PublishedApi
internal fun BaseContinuationImpl.getStackTraceElementImpl(): StackTraceElement? {
    val debugMetadata = getDebugMetadataAnnotation() ?: return null
    if (debugMetadata.version < COROUTINES_DEBUG_METADATA_VERSION_1_3) return null
    val label = getLabel()
    val lineNumber = if (label < 0) -1 else debugMetadata.lineNumbers[label]
    val moduleName = ModuleNameRetriever.getModuleName(this)
    val moduleAndClass = if (moduleName == null) debugMetadata.className else "$moduleName/${debugMetadata.className}"
    return StackTraceElement(moduleAndClass, debugMetadata.methodName, debugMetadata.sourceFile, lineNumber)
}

private object ModuleNameRetriever {
    private class Cache(
        @JvmField
        val getModuleMethod: Method?,
        @JvmField
        val getDescriptorMethod: Method?,
        @JvmField
        val nameMethod: Method?
    )

    private val notOnJava9 = Cache(null, null, null)

    private var cache: Cache? = null

    fun getModuleName(continuation: BaseContinuationImpl): String? {
        val cache = this.cache ?: buildCache(continuation)
        if (cache === notOnJava9) {
            return null
        }
        val module = cache.getModuleMethod?.invoke(continuation.javaClass) ?: return null
        val descriptor = cache.getDescriptorMethod?.invoke(module) ?: return null
        return cache.nameMethod?.invoke(descriptor) as? String
    }

    private fun buildCache(continuation: BaseContinuationImpl): Cache {
        try {
            val getModuleMethod = Class::class.java.getDeclaredMethod("getModule")
            val methodClass = continuation.javaClass.classLoader.loadClass("java.lang.Module")
            val getDescriptorMethod = methodClass.getDeclaredMethod("getDescriptor")
            val moduleDescriptorClass = continuation.javaClass.classLoader.loadClass("java.lang.module.ModuleDescriptor")
            val nameMethod = moduleDescriptorClass.getDeclaredMethod("name")
            return Cache(getModuleMethod, getDescriptorMethod, nameMethod).also { cache = it }
        } catch (ignored: Exception) {
            return notOnJava9.also { cache = it }
        }
    }
}

private fun BaseContinuationImpl.getDebugMetadataAnnotation(): DebugMetadata? =
    javaClass.getAnnotation(DebugMetadata::class.java)

private fun BaseContinuationImpl.getLabel(): Int =
    try {
        val field = javaClass.getDeclaredField("label")
        field.isAccessible = true
        (field.get(this) as? Int ?: 0) - 1
    } catch (e: Exception) { // NoSuchFieldException, SecurityException, or IllegalAccessException
        -1
    }

/**
 * Returns an array of spilled variable names and continuation's field names where the variable has been spilled.
 * The structure is the following:
 * - field names take 2*k'th indices
 * - corresponding variable names take (2*k + 1)'th indices.
 *
 * The function is for debugger to use, thus it returns simplest data type possible.
 * This function should only be called on suspended coroutines to get accurate mapping.
 *
 * The result is `null` when debug metadata is not available.
 */
@SinceKotlin("1.3")
@JvmName("getSpilledVariableFieldMapping")
@PublishedApi
internal fun BaseContinuationImpl.getSpilledVariableFieldMapping(): Array<String>? {
    val debugMetadata = getDebugMetadataAnnotation() ?: return null
    if (debugMetadata.version < COROUTINES_DEBUG_METADATA_VERSION_1_3) return null
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

private const val COROUTINES_DEBUG_METADATA_VERSION_1_3 = 1
private const val COROUTINES_DEBUG_METADATA_VERSION_2_2 = 2

/**
 * Returns next line number after current suspension point.
 *
 * In order to properly support step-over functionality, the debugger need to know which statement is next
 * after a suspension point. So, when user presses step-over, the debugger sets a breakpoint at that location.
 *
 * When the breakpoint is hit and coroutine is the same, then the execution has stepped over the suspension point,
 * regardless, whether the suspension point suspended or not.
 *
 * @return -1 when debug metadata is not available, or it has no mapping label->next line number.
 */
@SinceKotlin("2.2")
@PublishedApi
internal fun BaseContinuationImpl.getNextLineNumber(): Int {
    val debugMetadata = getDebugMetadataAnnotation() ?: return -1
    if (debugMetadata.version < COROUTINES_DEBUG_METADATA_VERSION_2_2) return -1
    val label = getLabel()
    if (label < 0) return -1
    if (label >= debugMetadata.nextLineNumbers.size) return -1
    return debugMetadata.nextLineNumbers[label]
}
