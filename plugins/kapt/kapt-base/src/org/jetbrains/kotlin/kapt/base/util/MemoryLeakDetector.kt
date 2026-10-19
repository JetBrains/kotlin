/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.util

import org.jetbrains.kotlin.kapt.base.DetectMemoryLeaksMode
import org.jetbrains.kotlin.kapt.base.KaptOptions
import org.jetbrains.kotlin.kapt.base.LoadedProcessors
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.function.Supplier
import javax.annotation.processing.*
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.ConcurrentModificationException

class MemoryLeak(val className: String, val fieldName: String, val description: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryLeak

        if (className != other.className) return false
        if (fieldName != other.fieldName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = className.hashCode()
        result = 31 * result + fieldName.hashCode()
        return result
    }
}

private class ClassLoaderData(classLoader: ClassLoader, val loadedClasses: (ClassLoader) -> Collection<Class<*>>) {
    val ref = WeakReference(classLoader)

    @Volatile
    var age: Int = 0
}

object MemoryLeakDetector {
    private val classLoaderData = mutableListOf<ClassLoaderData>()

    /**
     * [loadedClasses] returns the classes defined by the given classloader. There is no supported way to enumerate them,
     * and `ClassLoader.classes` is filtered from reflection since JDK 12, so the classloader has to track them itself.
     *
     * A classloader that is already registered is not registered again.
     *
     * [loadedClasses] must not capture [classLoader], otherwise the classloader would never be collected.
     */
    fun add(classLoader: ClassLoader, loadedClasses: (ClassLoader) -> Collection<Class<*>>) {
        synchronized(classLoaderData) {
            if (classLoaderData.any { it.ref.get() === classLoader }) return
            classLoaderData.add(ClassLoaderData(classLoader, loadedClasses))
        }
    }

    fun process(isParanoid: Boolean): Set<MemoryLeak> {
        val memoryLeaks = mutableSetOf<MemoryLeak>()

        synchronized(classLoaderData) {
            val newClassLoaderData = mutableListOf<ClassLoaderData>()
            for (data in classLoaderData) {
                val classLoader = data.ref.get() ?: continue
                data.age += 1

                if (isParanoid || data.age >= 5) {
                    // Inspect statics just once.
                    // Note the 'data' is not added to 'nextClassLoaderData' used the next time.
                    memoryLeaks += inspectStatics(classLoader, data.loadedClasses)
                } else {
                    newClassLoaderData += data
                }
            }

            classLoaderData.clear()
            classLoaderData.addAll(newClassLoaderData)
        }

        return memoryLeaks
    }

    private fun inspectStatics(classLoader: ClassLoader, loadedClasses: (ClassLoader) -> Collection<Class<*>>): Set<MemoryLeak> {
        val classes = loadedClasses(classLoader)
        val loadedClassesSet = try {
            classes.mapTo(mutableSetOf()) { it }
        } catch (e: ConcurrentModificationException) {
            Thread.sleep(100)
            return inspectStatics(classLoader, loadedClasses)
        }

        val leaks = mutableSetOf<MemoryLeak>()

        for (clazz in loadedClassesSet) {
            val declaredFields = try {
                clazz.declaredFields
            } catch (e: Throwable) {
                continue
            }

            nextField@ for (field in declaredFields) {
                if (!Modifier.isStatic(field.modifiers)) continue

                val value = field.getSafe(null)
                    ?.takeIf { !it.isPrimitiveOrString() } ?: continue@nextField

                if (value.isJavacComponent()) {
                    leaks += MemoryLeak(clazz.name, field.name, "Field leaks an Annotation Processing component ($value).")
                } else if (value is Class<*> && value in loadedClassesSet) {
                    leaks += MemoryLeak(clazz.name, field.name, "Field leaks a class type from the same ClassLoader (${value.name}).")
                }
            }
        }

        return leaks
    }
}

private fun Any.isJavacComponent(): Boolean {
    @Suppress("Reformat")
    return when (this) {
        is Processor, is ProcessingEnvironment, is RoundEnvironment,
            is Filer, is Messager, is Elements, is Types, is AnnotatedConstruct -> true
        else -> false
    }
}

private fun Any.isPrimitiveOrString(): Boolean {
    @Suppress("Reformat")
    return when (this) {
        is Boolean, is Byte, is Short, is Int, is Long,
            is Char, is Float, is Double, is Void, is String -> true
        else -> false
    }
}

private fun Field.getSafe(obj: Any?): Any? {
    return try {
        val oldIsAccessible = isAccessible

        try {
            isAccessible = true
            get(obj)
        } finally {
            isAccessible = oldIsAccessible
        }
    } catch (e: Throwable) {
        null
    }
}

/**
 * Registers the classloaders [processors] were loaded from with [MemoryLeakDetector] and reports the leaks found.
 *
 * A classloader can be inspected only if it tracks the classes it defines and exposes them as
 * `Supplier<Collection<Class<*>>>`, like [ClassTrackingURLClassLoader]. The supplier interface comes from the JDK, so that
 * classloaders created outside kapt (e.g. by the Gradle classloaders cache) can be inspected too.
 */
fun runMemoryLeakDetection(options: KaptOptions, processors: LoadedProcessors, logger: KaptLogger) {
    if (options.detectMemoryLeaks == DetectMemoryLeaksMode.NONE) return

    val processingClassLoaders = generateSequence(options.processingClassLoader) { it.parent }.takeWhile { it is Supplier<*> }
    for (classLoader in sequenceOf(processors.classLoader) + processingClassLoaders) {
        if (classLoader is Supplier<*>) MemoryLeakDetector.add(classLoader, ::trackedClasses)
    }

    val isParanoid = options.detectMemoryLeaks == DetectMemoryLeaksMode.PARANOID
    val leakDetectionStart = System.currentTimeMillis()
    val leaks = MemoryLeakDetector.process(isParanoid)
    logger.info { "Leak detection took ${System.currentTimeMillis() - leakDetectionStart} ms" }

    for (leak in leaks) {
        logger.warn(buildString {
            appendLine("Memory leak detected!")
            appendLine("Location: '${leak.className}', static field '${leak.fieldName}'")
            append(leak.description)
        })
    }
}

@Suppress("UNCHECKED_CAST")
private fun trackedClasses(classLoader: ClassLoader): Collection<Class<*>> =
    (classLoader as Supplier<Collection<Class<*>>>).get()
