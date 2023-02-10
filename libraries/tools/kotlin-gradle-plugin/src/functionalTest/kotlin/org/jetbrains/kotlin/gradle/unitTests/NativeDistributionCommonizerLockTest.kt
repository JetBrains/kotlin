/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "INVISIBLE_REFERENCE")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionCommonizerLock
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.net.URLClassLoader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier
import kotlin.reflect.full.declaredFunctions
import kotlin.test.fail

class NativeDistributionCommonizerLockTest {

    @get:Rule
    val temporaryFolderRule = TemporaryFolder()

    @Test
    fun `test - isolated classpath`() {
        val temporaryFolder = temporaryFolderRule.newFolder()

        /* Create the lock in two isolated classpaths */
        val classes = System.getProperty("java.class.path")
            .split(System.getProperty("path.separator"))
            .map(::File).map { it.toURI().toURL() }
            .toTypedArray()

        val isolatedClassLoader1 = URLClassLoader(classes, null)
        val isolatedClassLoader2 = URLClassLoader(classes, null)

        val isolatedLock1 = IsolatedLock(isolatedClassLoader1, temporaryFolder)
        val isolatedLock2 = IsolatedLock(isolatedClassLoader2, temporaryFolder)

        if (isolatedLock1.instance.javaClass === isolatedLock2.instance.javaClass)
            fail("Classpath isolation failed")

        val executor1 = Executors.newSingleThreadExecutor()
        val executor2 = Executors.newSingleThreadExecutor()

        val threadStateInitial = 0
        val threadStateLocked = 1
        val threadStateLeftLock = 2
        val threadState = AtomicInteger(threadStateInitial)

        val executorAction1 = Supplier<Unit> {
            isolatedLock1.withLock {
                threadState.set(threadStateLocked)
                Thread.sleep(510)
                threadState.set(threadStateLeftLock)
            }
        }

        val executorAction2 = Supplier<Unit> {
            while (true) {
                /* Collaborate with the timeout of the test */
                if (Thread.interrupted()) {
                    break
                }

                if (threadState.get() == threadStateLocked) {
                    /* Try to also capture the lock */
                    isolatedLock2.withLock {
                        /* If we captured the lock without it already being unlocked already, fail */
                        if (threadState.get() != threadStateLeftLock) {
                            fail("NativeDistributionCommonizerLock was entered in parallel")
                        }
                    }
                    break
                }
            }
        }
        try {
            val result1 = CompletableFuture.supplyAsync(executorAction1, executor1)
            val result2 = CompletableFuture.supplyAsync(executorAction2, executor2)
            CompletableFuture.allOf(result1, result2).get(10, TimeUnit.SECONDS)
        } finally {
            executor1.shutdownNow()
            executor2.shutdownNow()
        }
    }

    private class IsolatedLock private constructor(private val classLoader: ClassLoader, val instance: Any) {
        constructor(classLoader: ClassLoader, folder: File) : this(
            classLoader,
            classLoader.loadClass(NativeDistributionCommonizerLock::class.java.name).declaredConstructors.first().newInstance(folder)
        )

        fun withLock(action: () -> Unit) {
            val actionProxy = Proxy.newProxyInstance(
                classLoader,
                arrayOf(classLoader.loadClass(kotlin.jvm.functions.Function1::class.java.name)),
                InvocationHandler { _, _, _ -> action() })
            val withLockMethod = instance::class.declaredFunctions.first { it.name == "withLock" }
            withLockMethod.call(instance, actionProxy)
        }
    }
}
