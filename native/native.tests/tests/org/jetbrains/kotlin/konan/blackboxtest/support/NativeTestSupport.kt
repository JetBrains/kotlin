/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support

import org.jetbrains.kotlin.konan.blackboxtest.support.NativeTestSupport.createSimpleTestRunSettings
import org.jetbrains.kotlin.konan.blackboxtest.support.NativeTestSupport.createTestRunSettings
import org.jetbrains.kotlin.konan.blackboxtest.support.NativeTestSupport.getOrCreateSimpleTestRunProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.NativeTestSupport.getOrCreateTestRunProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.group.TestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.SimpleTestRunProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.CacheMode
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.jetbrains.kotlin.konan.target.Distribution
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
        System.setProperty("kotlin.native.home", settings.get<KotlinNativeHome>().dir.path)

        // Inject the required properties to test instance.
        with(settings.get<BlackBoxTestInstances>().enclosingTestInstance) {
            testRunSettings = settings
            testRunProvider = getOrCreateTestRunProvider()
        }
    }
}

class NativeSimpleTestSupport : BeforeEachCallback {
    override fun beforeEach(extensionContext: ExtensionContext): Unit = with(extensionContext) {
        val settings = createSimpleTestRunSettings()

        // Set the essential compiler property.
        System.setProperty("kotlin.native.home", settings.get<KotlinNativeHome>().dir.path)

        // Inject the required properties to test instance.
        with(settings.get<SimpleTestInstances>().enclosingTestInstance) {
            testRunSettings = settings
            testRunProvider = getOrCreateSimpleTestRunProvider()
        }
    }
}

private object NativeTestSupport {
    private val NAMESPACE = ExtensionContext.Namespace.create(NativeBlackBoxTestSupport::class.java.simpleName)

    /*************** Test process settings ***************/

    fun ExtensionContext.getOrCreateTestProcessSettings(): TestProcessSettings =
        root.getStore(NAMESPACE).getOrComputeIfAbsent(TestProcessSettings::class.java.name) {
            TestProcessSettings(
                computeNativeHome(),
                computeNativeClassLoader(),
                computeBaseDirs()
            )
        }.cast()

    private fun computeNativeHome(): KotlinNativeHome = KotlinNativeHome(File(ProcessLevelProperty.KOTLIN_NATIVE_HOME.readValue()))

    private fun computeNativeClassLoader(): KotlinNativeClassLoader = KotlinNativeClassLoader(
        lazy {
            val nativeClassPath = ProcessLevelProperty.COMPILER_CLASSPATH.readValue()
                .split(':', ';')
                .map { File(it).toURI().toURL() }
                .toTypedArray()

            URLClassLoader(nativeClassPath, /* no parent class loader */ null).apply { setDefaultAssertionStatus(true) }
        }
    )

    private fun computeBaseDirs(): BaseDirs {
        val testBuildDir = File(EnvironmentVariable.PROJECT_BUILD_DIR.readValue()).resolve("t")
        testBuildDir.mkdirs() // Make sure it exists. Don't clean up.

        return BaseDirs(testBuildDir)
    }

    /*************** Test class settings (common part) ***************/

    private fun ExtensionContext.addCommonTestClassSettingsTo(
        enclosingTestClass: Class<*>,
        output: MutableCollection<Any>
    ): KotlinNativeTargets {
        val enforcedProperties = EnforcedProperties(enclosingTestClass)

        val optimizationMode = computeOptimizationMode(enforcedProperties)
        val memoryModel = computeMemoryModel(enforcedProperties)

        val threadStateChecker = computeThreadStateChecker(enforcedProperties)
        if (threadStateChecker == ThreadStateChecker.ENABLED) {
            assertEquals(MemoryModel.EXPERIMENTAL, memoryModel) {
                "Thread state checker can be enabled only with experimental memory model"
            }
            assertEquals(OptimizationMode.DEBUG, optimizationMode) {
                "Thread state checker can be enabled only with debug optimization mode"
            }
        }

        val gcType = computeGCType(enforcedProperties)
        if (gcType != GCType.UNSPECIFIED) {
            assertEquals(MemoryModel.EXPERIMENTAL, memoryModel) {
                "GC type can be specified only with experimental memory model"
            }
        }

        val gcScheduler = computeGCScheduler(enforcedProperties)
        if (gcScheduler != GCScheduler.UNSPECIFIED) {
            assertEquals(MemoryModel.EXPERIMENTAL, memoryModel) {
                "GC scheduler can be specified only with experimental memory model"
            }
        }

        val nativeHome = getOrCreateTestProcessSettings().get<KotlinNativeHome>()

        val hostManager = HostManager(distribution = Distribution(nativeHome.dir.path), experimental = false)
        val nativeTargets = computeNativeTargets(enforcedProperties, hostManager)

        output += optimizationMode
        output += memoryModel
        output += threadStateChecker
        output += gcType
        output += gcScheduler
        output += nativeTargets
        output += CacheMode::class to computeCacheMode(enforcedProperties, nativeHome, nativeTargets, optimizationMode)
        output += computeTestMode(enforcedProperties)
        output += computeTimeouts(enforcedProperties)

        return nativeTargets
    }

    private fun computeOptimizationMode(enforcedProperties: EnforcedProperties): OptimizationMode =
        ClassLevelProperty.OPTIMIZATION_MODE.readValue(
            enforcedProperties,
            OptimizationMode.values(),
            default = OptimizationMode.DEBUG
        )

    private fun computeMemoryModel(enforcedProperties: EnforcedProperties): MemoryModel =
        ClassLevelProperty.MEMORY_MODEL.readValue(enforcedProperties, MemoryModel.values(), default = MemoryModel.DEFAULT)

    private fun computeThreadStateChecker(enforcedProperties: EnforcedProperties): ThreadStateChecker {
        val useThreadStateChecker =
            ClassLevelProperty.USE_THREAD_STATE_CHECKER.readValue(enforcedProperties, String::toBooleanStrictOrNull, default = false)
        return if (useThreadStateChecker) ThreadStateChecker.ENABLED else ThreadStateChecker.DISABLED
    }

    private fun computeGCType(enforcedProperties: EnforcedProperties): GCType =
        ClassLevelProperty.GC_TYPE.readValue(enforcedProperties, GCType.values(), default = GCType.UNSPECIFIED)

    private fun computeGCScheduler(enforcedProperties: EnforcedProperties): GCScheduler =
        ClassLevelProperty.GC_SCHEDULER.readValue(enforcedProperties, GCScheduler.values(), default = GCScheduler.UNSPECIFIED)

    private fun computeNativeTargets(enforcedProperties: EnforcedProperties, hostManager: HostManager): KotlinNativeTargets {
        val hostTarget = HostManager.host
        return KotlinNativeTargets(
            testTarget = ClassLevelProperty.TEST_TARGET.readValue(
                enforcedProperties,
                hostManager::targetByName,
                default = hostTarget
            ),
            hostTarget = hostTarget
        )
    }

    private fun computeCacheMode(
        enforcedProperties: EnforcedProperties,
        kotlinNativeHome: KotlinNativeHome,
        kotlinNativeTargets: KotlinNativeTargets,
        optimizationMode: OptimizationMode
    ): CacheMode {
        // TODO: legacy, need to remove it
        val legacyUseCache = System.getProperty("kotlin.internal.native.test.useCache")?.let(String::toBooleanStrictOrNull)
        if (legacyUseCache != null) {
            return if (legacyUseCache)
                CacheMode.WithStaticCache(kotlinNativeHome, kotlinNativeTargets, optimizationMode, false)
            else
                CacheMode.WithoutCache
        }

        val cacheMode = ClassLevelProperty.CACHE_MODE.readValue(
            enforcedProperties,
            CacheMode.Alias.values(),
            default = CacheMode.Alias.STATIC_ONLY_DIST
        )
        val staticCacheRequiredForEveryLibrary = when (cacheMode) {
            CacheMode.Alias.NO -> return CacheMode.WithoutCache
            CacheMode.Alias.STATIC_ONLY_DIST -> false
            CacheMode.Alias.STATIC_EVERYWHERE -> true
        }

        return CacheMode.WithStaticCache(kotlinNativeHome, kotlinNativeTargets, optimizationMode, staticCacheRequiredForEveryLibrary)
    }

    private fun computeTestMode(enforcedProperties: EnforcedProperties): TestMode =
        ClassLevelProperty.TEST_MODE.readValue(enforcedProperties, TestMode.values(), default = TestMode.WITH_MODULES)

    private fun computeTimeouts(enforcedProperties: EnforcedProperties): Timeouts {
        val executionTimeout = ClassLevelProperty.EXECUTION_TIMEOUT.readValue(
            enforcedProperties,
            { it.toLongOrNull()?.milliseconds },
            default = 10.seconds
        )
        return Timeouts(executionTimeout)
    }

    /*************** Test class settings (for black box tests only) ***************/

    private fun ExtensionContext.getOrCreateTestClassSettings(): TestClassSettings =
        root.getStore(NAMESPACE).getOrComputeIfAbsent(testClassKeyFor<TestClassSettings>()) {
            val enclosingTestClass = enclosingTestClass

            val testProcessSettings = getOrCreateTestProcessSettings()
            val computedTestConfiguration = computeTestConfiguration(enclosingTestClass)

            val settings = buildList {
                // Put common settings:
                val nativeTargets = addCommonTestClassSettingsTo(enclosingTestClass, this)

                // Put settings that are always required:
                this += computedTestConfiguration
                this += computeBinariesDirs(testProcessSettings.get(), nativeTargets, enclosingTestClass)

                // Add custom settings:
                computedTestConfiguration.configuration.requiredSettings.forEach { clazz ->
                    this += when (clazz) {
                        TestRoots::class -> computeTestRoots(enclosingTestClass)
                        GeneratedSources::class -> computeGeneratedSourceDirs(testProcessSettings.get(), nativeTargets, enclosingTestClass)
                        else -> fail { "Unknown test class setting type: $clazz" }
                    }
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
        val testSourcesDir = baseDirs.testBuildDir
            .resolve("bb.src") // "bb" for black box
            .resolve("${targets.testTarget.compressedName}_${enclosingTestClass.compressedSimpleName}")
            .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale generated sources.

        val sharedSourcesDir = testSourcesDir
            .resolve("__shared_modules__")
            .ensureExistsAndIsEmptyDirectory()

        return GeneratedSources(testSourcesDir, sharedSourcesDir)
    }

    private fun computeBinariesDirs(baseDirs: BaseDirs, targets: KotlinNativeTargets, enclosingTestClass: Class<*>): Binaries {
        val testBinariesDir = baseDirs.testBuildDir
            .resolve("bb.out") // "bb" for black box
            .resolve("${targets.testTarget.compressedName}_${enclosingTestClass.compressedSimpleName}")
            .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale artifacts.

        val sharedBinariesDir = testBinariesDir
            .resolve("__shared_modules__")
            .ensureExistsAndIsEmptyDirectory()

        return Binaries(testBinariesDir, sharedBinariesDir)
    }

    /*************** Test class settings (simplified) ***************/

    private fun ExtensionContext.getOrCreateSimpleTestClassSettings(): SimpleTestClassSettings =
        root.getStore(NAMESPACE).getOrComputeIfAbsent(testClassKeyFor<SimpleTestClassSettings>()) {
            SimpleTestClassSettings(
                parent = getOrCreateTestProcessSettings(),
                buildList { addCommonTestClassSettingsTo(enclosingTestClass, this) }
            )
        }.cast()

    /*************** Test run settings (for black box tests only) ***************/

    // Note: TestRunSettings is not cached!
    fun ExtensionContext.createTestRunSettings(): TestRunSettings {
        val testInstances = computeBlackBoxTestInstances()

        return TestRunSettings(
            parent = getOrCreateTestClassSettings(),
            listOfNotNull(
                testInstances,
                ExternalSourceTransformersProvider::class to testInstances.enclosingTestInstance.safeAs<ExternalSourceTransformersProvider>()
            )
        )
    }

    private fun ExtensionContext.computeBlackBoxTestInstances(): BlackBoxTestInstances =
        BlackBoxTestInstances(requiredTestInstances.allInstances)

    /*************** Test run settings (simplified) ***************/

    // Note: SimpleTestRunSettings is not cached!
    fun ExtensionContext.createSimpleTestRunSettings(): SimpleTestRunSettings {
        val testClassSettings = getOrCreateSimpleTestClassSettings()

        return SimpleTestRunSettings(
            parent = testClassSettings,
            listOf(
                computeSimpleTestInstances(),
                computeSimpleTestDirectories(testClassSettings.get(), testClassSettings.get())
            )
        )
    }

    private fun ExtensionContext.computeSimpleTestInstances(): SimpleTestInstances = SimpleTestInstances(requiredTestInstances.allInstances)

    private fun ExtensionContext.computeSimpleTestDirectories(baseDirs: BaseDirs, targets: KotlinNativeTargets): SimpleTestDirectories {
        val compressedClassNames = testClasses.map(Class<*>::compressedSimpleName).joinToString(separator = "_")

        val testBuildDir = baseDirs.testBuildDir
            .resolve("s") // "s" for simple
            .resolve("${targets.testTarget.compressedName}_$compressedClassNames")
            .resolve(requiredTestMethod.name)
            .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale artifacts.

        return SimpleTestDirectories(testBuildDir)
    }

    /*************** Test run provider (for black box tests only) ***************/

    fun ExtensionContext.getOrCreateTestRunProvider(): TestRunProvider =
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

    /*************** Test run provider (for black box tests only) ***************/

    // Currently, SimpleTestRunProvider is an object, so it does not need to be cached.
    fun getOrCreateSimpleTestRunProvider(): SimpleTestRunProvider = SimpleTestRunProvider

    /*************** Common ***************/

    private val ExtensionContext.testClasses: Sequence<Class<*>>
        get() = generateSequence(requiredTestClass) { it.enclosingClass }

    private val ExtensionContext.enclosingTestClass: Class<*>
        get() = testClasses.last()

    private inline fun <reified T : Any> ExtensionContext.testClassKeyFor(): String =
        enclosingTestClass.name + "#" + T::class.java.name
}
