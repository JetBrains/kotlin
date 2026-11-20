/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.test

import org.jetbrains.kotlin.kapt.base.util.MemoryLeak
import org.jetbrains.kotlin.kapt.base.util.MemoryLeakDetector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.function.Supplier
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

class MemoryLeakDetectorTest {
    object LeakHolder {
        @JvmStatic
        var leaked: Any? = null
    }

    private class TrackingClassLoader : ClassLoader(), Supplier<Collection<Class<*>>> {
        override fun get(): Collection<Class<*>> = listOf(LeakHolder::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    private fun trackedClasses(classLoader: ClassLoader): Collection<Class<*>> =
        (classLoader as Supplier<Collection<Class<*>>>).get()

    // The detector is a global object, so only the leaks of this test are checked.
    private fun Set<MemoryLeak>.ofLeakHolder(): List<String> =
        filter { it.className == LeakHolder::class.java.name }.map { it.fieldName }

    @Test
    fun testClassLoaderIsInspectedOnlyOnce() {
        LeakHolder.leaked = object : AbstractProcessor() {
            override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean = false
        }
        try {
            val classLoader = TrackingClassLoader()

            MemoryLeakDetector.add(classLoader, ::trackedClasses)
            assertEquals(listOf("leaked"), MemoryLeakDetector.process(isParanoid = true).ofLeakHolder())

            // A long-lived classloader (e.g. a cached one) is registered again on the next execution.
            MemoryLeakDetector.add(classLoader, ::trackedClasses)
            assertEquals(emptyList<String>(), MemoryLeakDetector.process(isParanoid = true).ofLeakHolder())
        } finally {
            LeakHolder.leaked = null
        }
    }
}
