/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.robolectric

import org.jetbrains.kotlin.parcelize.test.robolectric.shared.RobolectricBoxSwitchboard
import org.junit.Test
import org.junit.runner.JUnitCore
import org.junit.runner.RunWith
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.util.Base64
import kotlin.system.exitProcess

/**
 * The Android API version of Robolectric's `android-all`, see the `robolectricDependency` dependency of the module.
 */
const val ANDROID_API_VERSION = 36

/**
 * The main class of the worker JVM. Referenced by name, as the JVM that runs the tests doesn't have Robolectric.
 */
const val ROBOLECTRIC_BOX_WORKER_CLASS_NAME = "org.jetbrains.kotlin.parcelize.test.robolectric.RobolectricBoxWorker"

/**
 * Runs `box` of Parcelize tests in a Robolectric sandbox, one test per line of the standard input.
 *
 * A request is the class path of a test, separated by [File.pathSeparator]. A response is a line with [KEEP_WORKER] or [RETIRE_WORKER],
 * a space, and the Base64 encoding of `OK` or of the failure of the test, followed by its output. Starting a JVM and a sandbox takes
 * seconds, while `box` in a ready sandbox usually takes milliseconds, so the worker keeps one sandbox for all tests, see
 * [RobolectricBoxSwitchboard]. After a test that made the sandbox load its classes, the worker asks to be retired and exits.
 */
object RobolectricBoxWorker {
    const val KEEP_WORKER = "keep"
    const val RETIRE_WORKER = "retire"

    @JvmStatic
    fun main(args: Array<String>) {
        val responses = PrintStream(FileOutputStream(FileDescriptor.out), true, "UTF-8")
        val testOutput = ByteArrayOutputStream()
        PrintStream(testOutput, true, "UTF-8").let {
            System.setOut(it)
            System.setErr(it)
        }

        // The sandbox class loader takes the URLs of the context class loader and its parent.
        val classPath = System.getProperty("java.class.path").split(File.pathSeparator).map { File(it).toURI().toURL() }
        Thread.currentThread().contextClassLoader =
            URLClassLoader(classPath.toTypedArray(), RobolectricBoxSwitchboard.RoutingClassLoader(javaClass.classLoader))

        val requests = System.`in`.bufferedReader()
        while (true) {
            val request = requests.readLine() ?: break
            RobolectricBoxSwitchboard.testClassPath = request.split(File.pathSeparator).map { File(it).toURI().toURL() }.toTypedArray()
            testOutput.reset()
            val result = JUnitCore().run(RobolectricBoxTest::class.java)
            val response = if (result.wasSuccessful()) {
                "OK"
            } else {
                result.failures.joinToString("\n") { it.trace } + testOutput.toString("UTF-8")
            }
            val status = if (RobolectricBoxSwitchboard.sandboxLoadedTestClass) RETIRE_WORKER else KEEP_WORKER
            responses.println(status + " " + Base64.getEncoder().encodeToString(response.toByteArray()))
            if (status == RETIRE_WORKER) break
        }
        // Robolectric leaves non-daemon threads behind.
        exitProcess(0)
    }
}

/**
 * Runs `box` of the current test inside the sandbox, which is kept between the runs of this class.
 */
@RunWith(RobolectricBoxTestRunner::class)
@Config(sdk = [ANDROID_API_VERSION], manifest = Config.NONE)
class RobolectricBoxTest {
    @Test
    fun box() {
        val testClassLoader = RobolectricBoxSwitchboard.TestClassLoader(RobolectricBoxSwitchboard.testClassPath, javaClass.classLoader)
        RobolectricBoxSwitchboard.currentTestClassLoader = testClassLoader
        try {
            val result = try {
                Class.forName("test.TestKt", true, testClassLoader).getMethod("box").invoke(null)
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
            if (result != "OK") throw AssertionError(result)
        } finally {
            RobolectricBoxSwitchboard.currentTestClassLoader = null
            clearParcelableCreatorCaches()
            testClassLoader.close()
        }
    }

    /**
     * `Parcel` caches the `Parcelable.Creator`s of classes by name in static maps, which Robolectric doesn't reset between tests. The next
     * test would get the creators of this test's classes.
     */
    private fun clearParcelableCreatorCaches() {
        val parcelClass = Class.forName("android.os.Parcel", false, javaClass.classLoader)
        for (field in parcelClass.declaredFields) {
            if (field.name != "mCreators" && field.name != "sPairedCreators") continue
            field.isAccessible = true
            val creators = field.get(null) as MutableMap<*, *>
            synchronized(creators) { creators.clear() }
        }
    }
}

/**
 * Keeps the classes of tests and the [RobolectricBoxSwitchboard] out of the sandbox, see [RobolectricBoxSwitchboard].
 */
class RobolectricBoxTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {
    override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration =
        InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
            .doNotAcquirePackage(RobolectricBoxSwitchboard.TEST_PACKAGE_PREFIX)
            .doNotAcquirePackage(RobolectricBoxSwitchboard.PACKAGE_PREFIX)
            .build()
}
