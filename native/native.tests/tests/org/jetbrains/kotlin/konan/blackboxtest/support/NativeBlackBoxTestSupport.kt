/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support

import org.jetbrains.kotlin.konan.blackboxtest.AbstractNativeBlackBoxTest
import org.jetbrains.kotlin.konan.blackboxtest.support.group.NativeBlackBoxTestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.group.StandardTestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.group.TestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import java.io.File
import kotlin.reflect.KClass

class NativeBlackBoxTestSupport : BeforeEachCallback {
    /**
     * Note: [BeforeEachCallback.beforeEach] allows accessing test instances while [BeforeAllCallback.beforeAll] which may look
     * more preferable here does not allow it because it is called at the time when test instances are not created yet.
     * Also, [TestInstancePostProcessor.postProcessTestInstance] allows accessing only the currently created test instance and does
     * not allow accessing its parent test instance in case there are inner test classes in the generated test suite.
     */
    override fun beforeEach(extensionContext: ExtensionContext): Unit = with(extensionContext) {
        enclosingTestInstance.testRunProvider = getOrCreateTestRunProvider()
        enclosingTestInstance.onRunProviderSet()

        // Set the essential compiler property.
        System.setProperty("kotlin.native.home", getOrCreateGlobalEnvironment().kotlinNativeHome.path)
    }

    companion object {
        private val NAMESPACE = ExtensionContext.Namespace.create(NativeBlackBoxTestSupport::class.java.simpleName)

        /** Creates a single instance of [TestRunProvider] per test class. */
        private fun ExtensionContext.getOrCreateTestRunProvider(): TestRunProvider {
            val enclosingTestClass = enclosingTestClass

            return root.getStore(NAMESPACE).getOrComputeIfAbsent(enclosingTestClass.sanitizedName) {
                val globalEnvironment = getOrCreateGlobalEnvironment()

                val testRoots = computeTestRoots()

                val uniqueEnclosingClassDirName = globalEnvironment.target.compressedName + "_" + enclosingTestClass.compressedSimpleName

                val testSourcesDir = globalEnvironment.baseBuildDir
                    .resolve("bbtest.src")
                    .resolve(uniqueEnclosingClassDirName)
                    .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale generated sources.

                val sharedSourcesDir = testSourcesDir
                    .resolve("__shared_modules__")
                    .ensureExistsAndIsEmptyDirectory()

                val testBinariesDir = globalEnvironment.baseBuildDir
                    .resolve("bbtest.bin")
                    .resolve(uniqueEnclosingClassDirName)
                    .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale artifacts.

                val sharedBinariesDir = testBinariesDir
                    .resolve("__shared_modules__")
                    .ensureExistsAndIsEmptyDirectory()

                val environment = TestEnvironment(
                    globalEnvironment = globalEnvironment,
                    testRoots = testRoots,
                    testSourcesDir = testSourcesDir,
                    sharedSourcesDir = sharedSourcesDir,
                    testBinariesDir = testBinariesDir,
                    sharedBinariesDir = sharedBinariesDir
                )

                val testCaseGroupProvider = requiredTestClass.createTestCaseGroupProvider(environment)

                TestRunProvider(environment, testCaseGroupProvider)
            }.cast()
        }

        private fun ExtensionContext.getOrCreateGlobalEnvironment(): GlobalTestEnvironment =
            root.getStore(NAMESPACE).getOrComputeIfAbsent(GlobalTestEnvironment::class.java.sanitizedName) {
                // Create with the default settings.
                GlobalTestEnvironment()
            }.cast()

        private val ExtensionContext.enclosingTestInstance: AbstractNativeBlackBoxTest
            get() = requiredTestInstances.allInstances.firstOrNull().cast()

        private val ExtensionContext.enclosingTestClass: Class<*>
            get() = generateSequence(requiredTestClass) { it.enclosingClass }.last()

        private fun ExtensionContext.computeTestRoots(): TestRoots {
            val enclosingTestClass = enclosingTestClass

            val testRoots: Set<File> = when (val outermostTestMetadata = enclosingTestClass.getAnnotation(TestMetadata::class.java)) {
                null -> {
                    enclosingTestClass.declaredClasses.mapNotNullToSet { nestedClass ->
                        nestedClass.getAnnotation(TestMetadata::class.java)?.testRoot
                    }
                }
                else -> setOf(outermostTestMetadata.testRoot)
            }

            val baseDir: File = when (testRoots.size) {
                0 -> fail { "No test roots found for $enclosingTestClass test class." }
                1 -> testRoots.first().parentFile
                else -> {
                    val baseDirs = testRoots.mapToSet { it.parentFile }
                    assertEquals(1, baseDirs.size) {
                        "Controversial base directories computed for test roots for $enclosingTestClass test class: $baseDirs"
                    }

                    baseDirs.first()
                }
            }

            return TestRoots(testRoots, baseDir)
        }

        private fun Class<*>.createTestCaseGroupProvider(environment: TestEnvironment): TestCaseGroupProvider {
            val providerClass = getAnnotation(NativeBlackBoxTestCaseGroupProvider::class.java)?.value
                ?: return StandardTestCaseGroupProvider(environment)

            val constructor = providerClass.constructors.firstOrNull { constructor ->
                val singleParameter = constructor.parameters.singleOrNull() ?: return@firstOrNull false
                (singleParameter.type.classifier as? KClass<*>)?.qualifiedName == TestEnvironment::class.qualifiedName
            } ?: fail { "No suitable constructor for $providerClass" }

            return constructor.call(environment).cast()
        }

        private val TestMetadata.testRoot: File get() = getAbsoluteFile(localPath = value)
    }
}
