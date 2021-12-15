/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support

import org.jetbrains.kotlin.konan.blackboxtest.support.group.TestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class NativeBlackBoxTestSupport : BeforeEachCallback {
    /**
     * Note: [BeforeEachCallback.beforeEach] allows accessing test instances while [BeforeAllCallback.beforeAll] which may look
     * more preferable here does not allow it because it is called at the time when test instances are not created yet.
     * Also, [TestInstancePostProcessor.postProcessTestInstance] allows accessing only the currently created test instance and does
     * not allow accessing its parent test instance in case there are inner test classes in the generated test suite.
     */
    override fun beforeEach(extensionContext: ExtensionContext): Unit = with(extensionContext) {
        val settings = createTestRunSettings()

        // Set the essential compiler property.
        System.setProperty("kotlin.native.home", settings.get<KotlinNativeHome>().path)

        // Inject the required properties to test instance.
        with(settings.get<TestInstances>().enclosingTestInstance) {
            testRunSettings = settings
            testRunProvider = getOrCreateTestRunProvider()
        }
    }

    companion object {

        /*************** Test process settings ***************/

        private fun ExtensionContext.getOrCreateTestProcessSettings(): TestProcessSettings =
            root.getStore(NAMESPACE).getOrComputeIfAbsent(TestProcessSettings::class.java.name) {
                TestProcessSettings(
                    computeNativeTargets(),
                    computeNativeHome(),
                    computeNativeClassLoader(),
                    computeTestMode(),
                    CacheKind::class to computeCacheKind(),
                    computeBaseDirs(),
                    computeTimeouts()
                )
            }.cast()

        private fun computeNativeTargets(): KotlinNativeTargets {
            val hostTarget = HostManager.host
            return KotlinNativeTargets(testTarget = hostTarget, hostTarget = hostTarget)
        }

        private fun computeNativeHome(): KotlinNativeHome = KotlinNativeHome(File(requiredSystemProperty(KOTLIN_NATIVE_HOME)))

        private fun computeNativeClassLoader(): KotlinNativeClassLoader = KotlinNativeClassLoader(
            lazy {
                val nativeClassPath = requiredSystemProperty(COMPILER_CLASSPATH)
                    .split(':', ';')
                    .map { File(it).toURI().toURL() }
                    .toTypedArray()

                URLClassLoader(nativeClassPath, /* no parent class loader */ null).apply { setDefaultAssertionStatus(true) }
            }
        )

        private fun computeTestMode(): TestMode = systemProperty(
            name = TEST_MODE,
            transform = { testModeName ->
                TestMode.values().firstOrNull { it.name == testModeName } ?: fail {
                    buildString {
                        appendLine("Unknown test mode name $testModeName.")
                        appendLine("One of the following test modes should be passed through $TEST_MODE system property:")
                        TestMode.values().forEach { testMode ->
                            appendLine("- ${testMode.name}: ${testMode.description}")
                        }
                    }
                }

            },
            default = TestMode.WITH_MODULES
        )

        private fun computeCacheKind(): CacheKind {
            val useCache = systemProperty(
                name = USE_CACHE,
                transform = { useCacheValue ->
                    useCacheValue.toBooleanStrictOrNull() ?: fail { "Invalid value for $USE_CACHE system property: $useCacheValue" }
                },
                default = true
            )

            return if (useCache) CacheKind.WithStaticCache else CacheKind.WithoutCache
        }

        private fun computeBaseDirs(): BaseDirs = BaseDirs(File(requiredEnvironmentVariable(PROJECT_BUILD_DIR)))

        private fun computeTimeouts(): Timeouts {
            val executionTimeout = systemProperty(
                name = EXECUTION_TIMEOUT,
                transform = { executionTimeoutValue ->
                    executionTimeoutValue.toLongOrNull()?.milliseconds
                        ?: fail { "Invalid value for $EXECUTION_TIMEOUT system property: $executionTimeoutValue" }
                },
                default = 10.seconds
            )

            return Timeouts(executionTimeout)
        }

        private fun requiredSystemProperty(name: String): String =
            System.getProperty(name) ?: fail { "Unspecified $name system property" }

        private fun <T> systemProperty(name: String, transform: (String) -> T, default: T): T =
            System.getProperty(name)?.let(transform) ?: default

        private fun requiredEnvironmentVariable(name: String): String =
            System.getenv(name) ?: fail { "Unspecified $name environment variable" }

        private val NAMESPACE = ExtensionContext.Namespace.create(NativeBlackBoxTestSupport::class.java.simpleName)

        private const val KOTLIN_NATIVE_HOME = "kotlin.internal.native.test.nativeHome"
        private const val COMPILER_CLASSPATH = "kotlin.internal.native.test.compilerClasspath"
        private const val TEST_MODE = "kotlin.internal.native.test.mode"
        private const val USE_CACHE = "kotlin.internal.native.test.useCache"
        private const val EXECUTION_TIMEOUT = "kotlin.internal.native.test.executionTimeout"
        private const val PROJECT_BUILD_DIR = "PROJECT_BUILD_DIR"

        /*************** Test class settings ***************/

        private fun ExtensionContext.getOrCreateTestClassSettings(): TestClassSettings =
            root.getStore(NAMESPACE).getOrComputeIfAbsent(testClassKeyFor<TestClassSettings>()) {
                val enclosingTestClass = enclosingTestClass

                val testProcessSettings = getOrCreateTestProcessSettings()
                val computedTestConfiguration = computeTestConfiguration(enclosingTestClass)

                /// Put settings that are always required:
                val settings = mutableListOf(
                    computedTestConfiguration,
                    computeBinariesDirs(testProcessSettings.get(), testProcessSettings.get(), enclosingTestClass)
                )

                // Add custom settings:
                computedTestConfiguration.configuration.requiredSettings.mapTo(settings) { clazz ->
                    when (clazz) {
                        TestRoots::class -> computeTestRoots(enclosingTestClass)
                        GeneratedSources::class -> computeGeneratedSourceDirs(
                            testProcessSettings.get(),
                            testProcessSettings.get(),
                            enclosingTestClass
                        )
                        else -> fail { "Unknown test class setting type: $clazz" }
                    }
                }

                TestClassSettings(parent = testProcessSettings, settings)
            }.cast()

        private fun computeTestConfiguration(enclosingTestClass: Class<*>): ComputedTestConfiguration {
            val findTestConfiguration: Class<*>.() -> ComputedTestConfiguration? = {
                annotations.asSequence().mapNotNull { annotation ->
                    val testConfiguration = annotation.annotationClass.findAnnotation<TestConfiguration>() ?: return@mapNotNull null
                    ComputedTestConfiguration(testConfiguration, annotation)
                }.firstOrNull()
            }

            return enclosingTestClass.findTestConfiguration()
                ?: enclosingTestClass.declaredClasses.firstNotNullOfOrNull { it.findTestConfiguration() }
                ?: fail { "No @${TestConfiguration::class.simpleName} annotation found on test classes" }
        }

        private fun computeTestRoots(enclosingTestClass: Class<*>): TestRoots {
            fun TestMetadata.testRoot() = getAbsoluteFile(localPath = value)

            val testRoots: Set<File> = when (val outermostTestMetadata = enclosingTestClass.getAnnotation(TestMetadata::class.java)) {
                null -> {
                    enclosingTestClass.declaredClasses.mapNotNullToSet { nestedClass ->
                        nestedClass.getAnnotation(TestMetadata::class.java)?.testRoot()
                    }
                }
                else -> setOf(outermostTestMetadata.testRoot())
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

        private fun computeGeneratedSourceDirs(
            baseDirs: BaseDirs,
            targets: KotlinNativeTargets,
            enclosingTestClass: Class<*>
        ): GeneratedSources {
            val testSourcesDir = baseDirs.buildDir
                .resolve("bbtest.src")
                .resolve("${targets.testTarget.compressedName}_${enclosingTestClass.compressedSimpleName}")
                .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale generated sources.

            val sharedSourcesDir = testSourcesDir
                .resolve("__shared_modules__")
                .ensureExistsAndIsEmptyDirectory()

            return GeneratedSources(testSourcesDir, sharedSourcesDir)
        }

        private fun computeBinariesDirs(baseDirs: BaseDirs, targets: KotlinNativeTargets, enclosingTestClass: Class<*>): Binaries {
            val testBinariesDir = baseDirs.buildDir
                .resolve("bbtest.bin")
                .resolve("${targets.testTarget.compressedName}_${enclosingTestClass.compressedSimpleName}")
                .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale artifacts.

            val sharedBinariesDir = testBinariesDir
                .resolve("__shared_modules__")
                .ensureExistsAndIsEmptyDirectory()

            return Binaries(testBinariesDir, sharedBinariesDir)
        }

        /*************** Test run settings ***************/

        // Note: TestRunSettings is not cached!
        private fun ExtensionContext.createTestRunSettings(): TestRunSettings {
            val testInstances = computeTestInstances()

            return TestRunSettings(
                parent = getOrCreateTestClassSettings(),
                listOfNotNull(
                    testInstances,
                    ExternalSourceTransformersProvider::class to testInstances.enclosingTestInstance.safeAs<ExternalSourceTransformersProvider>()
                )
            )
        }

        private fun ExtensionContext.computeTestInstances(): TestInstances = TestInstances(requiredTestInstances.allInstances)

        /*************** Test run provider ***************/

        private fun ExtensionContext.getOrCreateTestRunProvider(): TestRunProvider =
            root.getStore(NAMESPACE).getOrComputeIfAbsent(testClassKeyFor<TestRunProvider>()) {
                val testCaseGroupProvider = createTestCaseGroupProvider(getOrCreateTestClassSettings().get())
                TestRunProvider(testCaseGroupProvider)
            }.cast()

        private fun createTestCaseGroupProvider(computedTestConfiguration: ComputedTestConfiguration): TestCaseGroupProvider {
            val (testConfiguration: TestConfiguration, testConfigurationAnnotation: Annotation) = computedTestConfiguration
            val providerClass: KClass<out TestCaseGroupProvider> = testConfiguration.providerClass

            // Assumption: For simplicity’s sake TestCaseGroupProvider has just one constructor.
            val constructor = providerClass.constructors.singleOrNull()
                ?: fail { "No or multiple constructors found for $providerClass" }

            val testConfigurationAnnotationClass: KClass<out Annotation> = testConfigurationAnnotation.annotationClass

            val arguments = constructor.parameters.map { parameter ->
                when {
                    parameter.hasTypeOf(testConfigurationAnnotationClass) -> testConfigurationAnnotation
                    // maybe add other arguments???
                    else -> fail { "Can't provide all arguments for $constructor" }
                }
            }

            return constructor.call(*arguments.toTypedArray()).cast()
        }

        private fun KParameter.hasTypeOf(clazz: KClass<*>): Boolean = (type.classifier as? KClass<*>)?.qualifiedName == clazz.qualifiedName

        /*************** Common ***************/

        private val ExtensionContext.enclosingTestClass: Class<*>
            get() = generateSequence(requiredTestClass) { it.enclosingClass }.last()

        private inline fun <reified T : Any> ExtensionContext.testClassKeyFor(): String =
            enclosingTestClass.name + "#" + T::class.java.name
    }
}
