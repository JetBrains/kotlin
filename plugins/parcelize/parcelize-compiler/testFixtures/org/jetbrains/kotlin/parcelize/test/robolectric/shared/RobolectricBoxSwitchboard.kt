/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.robolectric.shared

import java.net.URL
import java.net.URLClassLoader

/**
 * The state that a Robolectric box worker shares with the Robolectric sandbox in which it runs tests.
 *
 * All tests run in one sandbox, so that its Android classes are loaded only once, but the classes of each test are loaded by its own
 * [TestClassLoader]: tests declare the same classes in the [TEST_PACKAGE_PREFIX] package. The sandbox doesn't load these classes itself
 * and delegates them to its parent, a [RoutingClassLoader], which finds them in the class loader of the current test. Robolectric doesn't
 * load this package in the sandbox either, so the worker and the sandbox see the same object.
 *
 * When Android code looks up a class of a test by name, e.g., to unparcel it without a class loader, the JVM remembers the found class
 * for the sandbox class loader for good. A later test with a class of the same name would get the class of this test, so a worker that
 * served such a lookup doesn't run other tests, see [sandboxLoadedTestClass].
 */
object RobolectricBoxSwitchboard {
    const val TEST_PACKAGE_PREFIX = "test."
    const val PACKAGE_PREFIX = "org.jetbrains.kotlin.parcelize.test.robolectric.shared."

    @Volatile
    @JvmStatic
    var testClassPath: Array<URL> = emptyArray()

    @Volatile
    @JvmStatic
    var currentTestClassLoader: ClassLoader? = null

    /**
     * Whether the sandbox class loader was asked for a class of a test, and so could return it to later tests.
     */
    @Volatile
    @JvmStatic
    var sandboxLoadedTestClass: Boolean = false

    /**
     * The parent of the sandbox class loader. Delegates the classes of tests to the class loader of the current test.
     */
    class RoutingClassLoader(parent: ClassLoader) : ClassLoader(parent) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (!name.startsWith(TEST_PACKAGE_PREFIX)) return super.loadClass(name, resolve)
            sandboxLoadedTestClass = true
            val testClassLoader = currentTestClassLoader ?: throw ClassNotFoundException(name)
            return testClassLoader.loadClass(name)
        }
    }

    /**
     * Loads the classes of one test from [urls] and everything else from the sandbox.
     */
    class TestClassLoader(urls: Array<URL>, sandboxClassLoader: ClassLoader) : URLClassLoader(urls, sandboxClassLoader) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (!name.startsWith(TEST_PACKAGE_PREFIX)) return super.loadClass(name, resolve)
            synchronized(getClassLoadingLock(name)) {
                val loadedClass = findLoadedClass(name) ?: findClass(name)
                if (resolve) resolveClass(loadedClass)
                return loadedClass
            }
        }
    }
}
