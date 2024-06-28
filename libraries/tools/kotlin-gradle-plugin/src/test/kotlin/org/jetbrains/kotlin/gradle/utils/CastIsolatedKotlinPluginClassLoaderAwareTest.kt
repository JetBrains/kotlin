/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.jetbrains.kotlin.gradle.plugin.MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING
import org.jetbrains.kotlin.util.assertThrows
import org.junit.AssumptionViolatedException
import java.net.URLClassLoader
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CastIsolatedKotlinPluginClassLoaderAwareTest {
    interface A
    interface B : A

    private val isolatedClassLoader by lazy {
        val thisClassLoader = this::class.java.classLoader as? URLClassLoader
            ?: throw AssumptionViolatedException(
                "Test required to load classes with ${URLClassLoader::class.java.name}. " +
                        "Found ${this::class.java.classLoader.javaClass.name}"
            )

        URLClassLoader(thisClassLoader.urLs, ClassLoader.getSystemClassLoader().parent)
    }

    @Test
    fun `test downcast`() {
        val b = object : B {}
        val a = b.castIsolatedKotlinPluginClassLoaderAware<A>()
        assertSame(b, a)
    }

    @Test
    fun `test impossible cast`() {
        val a = object : A {}
        assertNull(
            a.castIsolatedKotlinPluginClassLoaderAware<B?>(),
            "Expected impossible cast to return null since the type parameter is marked nullable"
        )

        val exception = assertThrows<ClassCastException>(
            "Expected impossible cast to throw 'ClassCastException' because type parameter is not marked nullable"
        ) { a.castIsolatedKotlinPluginClassLoaderAware<B>() }

        assertTrue(
            MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING !in exception.message.orEmpty(),
            "Expected no classpath warning in the error message, since this cast was not failing because of isolated classloaders"
        )
    }

    @Test
    fun `test downcast - with isolated classpath`() {
        class BImpl : B

        val isolatedBInstance = isolatedClassLoader.loadClass(BImpl::class.java.name).constructors.single().newInstance()

        run {
            val exception = assertThrows<IsolatedKotlinClasspathClassCastException>(
                "Expected 'ClassCastException' because of classloader isolation"
            ) { isolatedBInstance.castIsolatedKotlinPluginClassLoaderAware<A>() }

            assertTrue(
                MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING in exception.message.orEmpty(),
                "Expected classpath warning in the error message"
            )
        }

        run {
            val exception = assertThrows<IsolatedKotlinClasspathClassCastException> {
                isolatedBInstance.castIsolatedKotlinPluginClassLoaderAware<A?>()
            }

            assertTrue(MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING in exception.message.orEmpty())
        }
    }

    @Test
    fun `test impossible cast - with isolated classpath`() {
        class AImpl : A

        val isolatedAInstance = isolatedClassLoader.loadClass(AImpl::class.java.name).constructors.single().newInstance()

        run {
            val exception = assertThrows<IsolatedKotlinClasspathClassCastException>(
                "Expected 'ClassCastException' because of classloader isolation"
            ) { isolatedAInstance.castIsolatedKotlinPluginClassLoaderAware<B>() }

            assertTrue(
                MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING in exception.message.orEmpty(),
                "Expected classpath warning in the error message"
            )
        }

        assertNull(
            isolatedAInstance.castIsolatedKotlinPluginClassLoaderAware<B?>(),
            "Expected 'null', since the type parameter was marked optional and the " +
                    "isolated classpath was able to detect the impossible cast"
        )
    }
}
