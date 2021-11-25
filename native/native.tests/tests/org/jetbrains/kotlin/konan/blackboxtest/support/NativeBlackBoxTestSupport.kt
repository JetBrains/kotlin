/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support

import org.jetbrains.kotlin.konan.blackboxtest.AbstractNativeBlackBoxTest
import org.jetbrains.kotlin.konan.blackboxtest.support.group.TestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.GlobalSettings
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Settings
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.TestRoots
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.TestSettings
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
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation

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
        System.setProperty("kotlin.native.home", getOrCreateGlobalSettings().kotlinNativeHome.path)
    }

    companion object {
        private val NAMESPACE = ExtensionContext.Namespace.create(NativeBlackBoxTestSupport::class.java.simpleName)

        /** Creates a single instance of [TestRunProvider] per test class. */
        private fun ExtensionContext.getOrCreateTestRunProvider(): TestRunProvider {
            val enclosingTestClass = enclosingTestClass

            return root.getStore(NAMESPACE).getOrComputeIfAbsent(enclosingTestClass.sanitizedName) {
                val globalSettings = getOrCreateGlobalSettings()
                val (testSettings, testSettingsAnnotation) = computeTestSettings(enclosingTestClass)

                val testRoots = computeTestRoots(enclosingTestClass)

                val uniqueEnclosingClassDirName = globalSettings.target.compressedName + "_" + enclosingTestClass.compressedSimpleName

                val testSourcesDir = globalSettings.baseBuildDir
                    .resolve("bbtest.src")
                    .resolve(uniqueEnclosingClassDirName)
                    .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale generated sources.

                val sharedSourcesDir = testSourcesDir
                    .resolve("__shared_modules__")
                    .ensureExistsAndIsEmptyDirectory()

                val testBinariesDir = globalSettings.baseBuildDir
                    .resolve("bbtest.bin")
                    .resolve(uniqueEnclosingClassDirName)
                    .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale artifacts.

                val sharedBinariesDir = testBinariesDir
                    .resolve("__shared_modules__")
                    .ensureExistsAndIsEmptyDirectory()

                val settings = Settings(
                    global = globalSettings,
                    testRoots = testRoots,
                    testSourcesDir = testSourcesDir,
                    sharedSourcesDir = sharedSourcesDir,
                    testBinariesDir = testBinariesDir,
                    sharedBinariesDir = sharedBinariesDir
                )

                val testCaseGroupProvider = createTestCaseGroupProvider(settings, testSettings, testSettingsAnnotation)

                TestRunProvider(settings, testCaseGroupProvider)
            }.cast()
        }

        private fun ExtensionContext.getOrCreateGlobalSettings(): GlobalSettings =
            root.getStore(NAMESPACE).getOrComputeIfAbsent(GlobalSettings::class.java.sanitizedName) {
                // Create with the default settings.
                GlobalSettings()
            }.cast()

        private val ExtensionContext.enclosingTestInstance: AbstractNativeBlackBoxTest
            get() = requiredTestInstances.allInstances.firstOrNull().cast()

        private val ExtensionContext.enclosingTestClass: Class<*>
            get() = generateSequence(requiredTestClass) { it.enclosingClass }.last()

        private fun computeTestRoots(enclosingTestClass: Class<*>): TestRoots {
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

        private fun computeTestSettings(enclosingTestClass: Class<*>): Pair<TestSettings, Annotation?> {
            val findTestSettings: Class<*>.() -> Pair<TestSettings, Annotation>? = {
                annotations.asSequence().mapNotNull { annotation ->
                    val testSettings = annotation.annotationClass.findAnnotation<TestSettings>() ?: return@mapNotNull null
                    testSettings to annotation
                }.firstOrNull()
            }

            return enclosingTestClass.findTestSettings()
                ?: enclosingTestClass.declaredClasses.firstNotNullOfOrNull { it.findTestSettings() }
                ?: (TestSettings.DEFAULT to null) // Fallback if there is no annotation.
        }

        private fun createTestCaseGroupProvider(
            settings: Settings,
            testSettings: TestSettings,
            testSettingsAnnotation: Annotation?
        ): TestCaseGroupProvider {
            // First, try to find two-argument constructor.
            if (testSettingsAnnotation != null) {
                val testSettingsAnnotationClass = testSettingsAnnotation.annotationClass

                testSettings.providerClass.constructors.asSequence()
                    .forEach { c ->
                        val (p1, p2) = c.parameters.takeIf { it.size == 2 } ?: return@forEach
                        @Suppress("Reformat") val provider = when {
                            p1.hasTypeOf<Settings>() && p2.hasTypeOf(testSettingsAnnotationClass) -> c.call(settings, testSettingsAnnotation)
                            p1.hasTypeOf(testSettingsAnnotationClass) && p2.hasTypeOf<Settings>() -> c.call(testSettingsAnnotation, settings)
                            else -> return@forEach
                        }
                        return provider.cast()
                    }
            }

            // Next, try to find a single-argument constructor.
            testSettings.providerClass.constructors.asSequence()
                .forEach { c ->
                    val p = c.parameters.singleOrNull() ?: return@forEach
                    if (p.hasTypeOf<Settings>()) return c.call(settings).cast()
                }

            fail { "No suitable constructor for ${testSettings.providerClass}" }
        }

        private inline fun <reified T : Any> KParameter.hasTypeOf(): Boolean = hasTypeOf(T::class)
        private fun KParameter.hasTypeOf(clazz: KClass<*>): Boolean = (type.classifier as? KClass<*>)?.qualifiedName == clazz.qualifiedName

        private val TestMetadata.testRoot: File get() = getAbsoluteFile(localPath = value)
    }
}
