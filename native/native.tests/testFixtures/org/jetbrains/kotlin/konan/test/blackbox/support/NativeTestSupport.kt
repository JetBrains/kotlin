/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.config.nativeBinaryOptions.GC
import org.jetbrains.kotlin.config.nativeBinaryOptions.GCSchedulerType
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeBlackBoxTest
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.computeBinariesForBlackBoxTests
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.computeBinariesForSimpleTests
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.computeBlackBoxTestInstances
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createSimpleTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.getOrCreateSimpleTestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.getOrCreateTestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.group.*
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.SimpleTestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.util.*
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.time.Duration

const val KLIB_IR_INLINER = "klibIrInliner"

class NativeBlackBoxTestSupport : BeforeEachCallback {
    /**
     * Note: [BeforeEachCallback.beforeEach] allows accessing test instances while [BeforeAllCallback.beforeAll] which may look
     * more preferable here does not allow it because it is called at the time when test instances are not created yet.
     * Also, [TestInstancePostProcessor.postProcessTestInstance] allows accessing only the currently created test instance and does
     * not allow accessing its parent test instance in case there are inner test classes in the generated test suite.
     */
    override fun beforeEach(extensionContext: ExtensionContext): Unit = with(extensionContext) {
        val settings = createTestRunSettings(computeBlackBoxTestInstances())

        // Inject the required properties to test instance.
        with(settings.get<NativeTestInstances<AbstractNativeBlackBoxTest>>().enclosingTestInstance) {
            testRunSettings = settings
            testRunProvider = getOrCreateTestRunProvider()
        }
    }
}

class NativeSimpleTestSupport : BeforeEachCallback {
    override fun beforeEach(extensionContext: ExtensionContext): Unit = with(extensionContext) {
        val settings = createSimpleTestRunSettings()

        // Inject the required properties to test instance.
        with(settings.get<NativeTestInstances<AbstractNativeSimpleTest>>().enclosingTestInstance) {
            testRunSettings = settings
            testRunProvider = getOrCreateSimpleTestRunProvider()
        }
    }
}

internal object CastCompatibleKotlinNativeClassLoader {
    val kotlinNativeClassLoader = NativeTestSupport.computeNativeClassLoader(this::class.java.classLoader)
}

object RegularKotlinNativeClassLoader {
    val kotlinNativeClassLoader = NativeTestSupport.computeNativeClassLoader()
}

fun copyNativeHomeProperty() {
    System.setProperty("kotlin.native.home", ProcessLevelProperty.KOTLIN_NATIVE_HOME.readValue())
}

object NativeTestSupport {
    private val NAMESPACE = ExtensionContext.Namespace.create(NativeTestSupport::class.java.simpleName)

    /*************** Test process settings ***************/

    fun ExtensionContext.getOrCreateTestProcessSettings(): TestProcessSettings =
        root.getStore(NAMESPACE).getOrComputeIfAbsent(TestProcessSettings::class.java.name) {
            val nativeHome = computeNativeHome()

            // Apply the necessary process-wide settings:
            copyNativeHomeProperty() // Set the essential compiler property.
            setUpMemoryTracking() // Set up memory tracking and reporting.

            TestProcessSettings(
                nativeHome,
                RegularKotlinNativeClassLoader.kotlinNativeClassLoader,
                computeBaseDirs(),
                LLDB(nativeHome),
                computeCustomNativeCompiler()
            )
        } as TestProcessSettings

    private fun computeNativeHome(): KotlinNativeHome = KotlinNativeHome(File(ProcessLevelProperty.KOTLIN_NATIVE_HOME.readValue()))

    /**
     * - For Codegen native tests, no parent classloader should be provided,
     * since compiler `class K2Native: CLICompiler` is invoked via reflection with kotlinNativeClassLoader,
     * so both classes K2Native and CLICompiler need to be loaded with same nativeclassloader.
     * This way, `K2Native.doExecute()` method would define abstract method `CLICompiler.doExecute()`.
     * With parent classloader, these two classes would be loaded by different classloaders and AbstractMethodError is thrown
     * - For irText tests, classloaded mangler instance is passed to generate mangles to IR dumps files.
     * For this, a cast of mangler object(within K/N classloader) to mangler interface(within app classloader) is needed,
     * which is possible when app classloader is provided as parent.
     */
    internal fun computeNativeClassLoader(parent: ClassLoader? = null): KotlinNativeClassLoader = KotlinNativeClassLoader(
        lazy {
            val nativeClassPath = ProcessLevelProperty.COMPILER_CLASSPATH.readValue()
                .split(File.pathSeparatorChar)
                .map { File(it).toURI().toURL() }
                .toTypedArray()

            URLClassLoader(nativeClassPath, parent).apply { setDefaultAssertionStatus(true) }
        }
    )

    private fun computeBaseDirs(): BaseDirs {
        val testBuildDir = File(EnvironmentVariable.PROJECT_BUILD_DIR.readValue()).resolve("t")
        testBuildDir.mkdirs() // Make sure it exists. Don't clean up.

        return BaseDirs(testBuildDir)
    }

    private fun computeCustomNativeCompiler() = CustomNativeCompiler(
        lazy {
            val parentDirectory = File(ProcessLevelProperty.CUSTOM_KOTLIN_NATIVE_HOME.readValue())
            val nativePrebuilt = findNativePrebuilt(parentDirectory)
            KotlinNativeHome(nativePrebuilt)
        }
    )

    private fun findNativePrebuilt(parentDir: File): File {
        val filesInParentDir = parentDir.listFiles()
        require(filesInParentDir != null) { "Parent directory for the custom compiler not found: $parentDir" }
        return filesInParentDir.single { it.name.contains("kotlin-native-prebuilt") }
    }

    private fun ExtensionContext.setUpMemoryTracking() {
        if (ProcessLevelProperty.TEAMCITY.readValue().toBoolean())
            return // Don't track memory when running at TeamCity. It tracks memory by itself.

        TestLogger.initialize() // Initialize special logging (directly to Gradle's console).

        val gradleTaskName = EnvironmentVariable.GRADLE_TASK_NAME.readValue()
        fun Long.toMBs() = (this / 1024 / 1024)

        // Set up memory tracking and reporting:
        MemoryTracker.startTracking(intervalMillis = 1000) { memoryMark ->
            TestLogger.log(
                buildString {
                    append(memoryMark.timestamp).append(' ').append(gradleTaskName)
                    append(" Memory usage (MB): ")
                    append("used=").append(memoryMark.usedMemory.toMBs())
                    append(", free=").append(memoryMark.freeMemory.toMBs())
                    append(", total=").append(memoryMark.totalMemory.toMBs())
                    append(", max=").append(memoryMark.maxMemory.toMBs())
                }
            )
        }

        // Stop tracking memory when all tests are finished:
        root.getStore(NAMESPACE).put(
            testClassKeyFor<MemoryTracker>(),
            ExtensionContext.Store.CloseableResource { MemoryTracker.stopTracking() }
        )
    }

    /*************** Test class settings (common part) ***************/

    private fun ExtensionContext.addCommonTestClassSettingsTo(
        enclosingTestClass: Class<*>,
        output: MutableCollection<Any>,
    ): KotlinNativeTargets {
        val enforcedProperties = EnforcedProperties(enclosingTestClass)

        val optimizationMode = computeOptimizationMode(enforcedProperties)

        val threadStateChecker = computeThreadStateChecker(enforcedProperties)
        if (threadStateChecker == ThreadStateChecker.ENABLED) {
            assertEquals(OptimizationMode.DEBUG, optimizationMode) {
                "Thread state checker can be enabled only with debug optimization mode"
            }
        }
        val sanitizer = computeSanitizer(enforcedProperties)

        val gcType = computeGCType(enforcedProperties)

        val gcScheduler = computeGCScheduler(enforcedProperties)

        val binaryOptions = computeBinaryOptions(enforcedProperties)

        val allocator = computeAllocator(enforcedProperties)

        val nativeHome = getOrCreateTestProcessSettings().get<KotlinNativeHome>()

        val distribution = Distribution(nativeHome.dir.path)
        val hostManager = HostManager()
        val nativeTargets = computeNativeTargets(enforcedProperties, hostManager)

        val cacheMode = computeCacheMode(enforcedProperties, distribution, nativeTargets, optimizationMode)

        output += optimizationMode
        output += threadStateChecker
        output += gcType
        output += gcScheduler
        output += binaryOptions
        output += allocator
        output += nativeTargets
        output += sanitizer
        output += CacheMode::class to cacheMode
        output += computeTestMode(enforcedProperties)
        output += computeCompilerPlugins(enforcedProperties)
        output += computeCustomKlibs(enforcedProperties)
        output += computeTestKind(enforcedProperties)
        output += computeForcedNoopTestRunner(enforcedProperties)
        output += computeSharedExecutionTestRunner(enforcedProperties)
        // Parse annotations of current class, since there's no way to put annotations to upper-level enclosing class
        output += computePipelineType(enforcedProperties, testClass.get())
        output += computeUsedPartialLinkageConfig(enclosingTestClass)
        output += computeCompilerOutputInterceptor(enforcedProperties)
        output += computeBinaryLibraryKind(enforcedProperties)
        output += computeCInterfaceMode(enforcedProperties)
        output += computeXCTestRunner(enforcedProperties, nativeTargets)
        output += computeKlibIrInlinerMode(tags)

        // Compute tests timeouts with regard to already calculated properties that may affect execution time
        output += computeTimeouts(enforcedProperties, output)

        return nativeTargets
    }

    private fun computeOptimizationMode(enforcedProperties: EnforcedProperties): OptimizationMode =
        ClassLevelProperty.OPTIMIZATION_MODE.readValue(
            enforcedProperties,
            OptimizationMode.values(),
            default = OptimizationMode.DEBUG
        )

    private fun computeThreadStateChecker(enforcedProperties: EnforcedProperties): ThreadStateChecker {
        val useThreadStateChecker =
            ClassLevelProperty.USE_THREAD_STATE_CHECKER.readValue(enforcedProperties, String::toBooleanStrictOrNull, default = false)
        return if (useThreadStateChecker) ThreadStateChecker.ENABLED else ThreadStateChecker.DISABLED
    }

    private fun computeSanitizer(enforcedProperties: EnforcedProperties): Sanitizer =
        ClassLevelProperty.SANITIZER.readValue(enforcedProperties, Sanitizer.values(), default = Sanitizer.NONE)

    private fun computeCompilerOutputInterceptor(enforcedProperties: EnforcedProperties): CompilerOutputInterceptor =
        ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR.readValue(
            enforcedProperties,
            CompilerOutputInterceptor.values(),
            default = CompilerOutputInterceptor.DEFAULT
        )

    private fun computeKlibIrInlinerMode(tags: Set<String>): KlibIrInlinerMode =
        if (tags.contains(KLIB_IR_INLINER))
            KlibIrInlinerMode.ON
        else
            KlibIrInlinerMode.OFF

    private fun computeGCType(enforcedProperties: EnforcedProperties): GCType =
        ClassLevelProperty.GC_TYPE
            .readValue(
                enforcedProperties,
                transform = { str -> GC.entries.firstOrNull { it.shortcut == str.lowercase() } },
                default = GC.PARALLEL_MARK_CONCURRENT_SWEEP
            ).let { GCType(it) }

    private fun computeGCScheduler(enforcedProperties: EnforcedProperties): GCScheduler =
        ClassLevelProperty.GC_SCHEDULER.readValueOrNull(enforcedProperties, GCSchedulerType.values()).let { GCScheduler(it) }

    private fun computeBinaryOptions(enforcedProperties: EnforcedProperties): ExplicitBinaryOptions =
        ClassLevelProperty.BINARY_OPTIONS.readValue(
            enforcedProperties, { it.split(",") }, emptyList()
        ).let(::ExplicitBinaryOptions)

    private fun computeAllocator(enforcedProperties: EnforcedProperties): Allocator =
        ClassLevelProperty.ALLOCATOR.readValue(enforcedProperties, Allocator.values(), default = Allocator.UNSPECIFIED)

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
        distribution: Distribution,
        kotlinNativeTargets: KotlinNativeTargets,
        optimizationMode: OptimizationMode,
    ): CacheMode {
        val defaultCache = CacheMode.defaultForTestTarget(distribution, kotlinNativeTargets)
        val cacheMode = ClassLevelProperty.CACHE_MODE.readValue(
            enforcedProperties,
            CacheMode.Alias.values(),
            default = if (optimizationMode != OptimizationMode.OPT) defaultCache
            else CacheMode.Alias.NO,
        )
        if (kotlinNativeTargets.testTarget == KonanTarget.MINGW_X64) {
            if (!(cacheMode == CacheMode.Alias.NO || cacheMode == CacheMode.Alias.STATIC_ONLY_DIST)) {
                fail { "Cache mode $cacheMode is not supported for MinGW x64 target." }
            }
        }
        val useStaticCacheForUserLibraries = when (cacheMode) {
            CacheMode.Alias.NO -> return CacheMode.WithoutCache
            CacheMode.Alias.STATIC_ONLY_DIST -> false
            CacheMode.Alias.STATIC_EVERYWHERE -> true
            CacheMode.Alias.STATIC_PER_FILE_EVERYWHERE -> true
            CacheMode.Alias.STATIC_USE_HEADERS_EVERYWHERE -> true
        }
        val makePerFileCaches = cacheMode == CacheMode.Alias.STATIC_PER_FILE_EVERYWHERE
        val useHeaders = cacheMode == CacheMode.Alias.STATIC_USE_HEADERS_EVERYWHERE

        return if (defaultCache == CacheMode.Alias.NO)
            CacheMode.WithoutCache
        else CacheMode.WithStaticCache(
            optimizationMode,
            useStaticCacheForUserLibraries,
            makePerFileCaches,
            useHeaders,
            cacheMode
        )
    }

    private fun computeTestMode(enforcedProperties: EnforcedProperties): TestMode =
        ClassLevelProperty.TEST_MODE.readValue(enforcedProperties, TestMode.values(), default = TestMode.TWO_STAGE_MULTI_MODULE)

    private fun computeCompilerPlugins(enforcedProperties: EnforcedProperties): CompilerPlugins =
        CompilerPlugins(
            ClassLevelProperty.COMPILER_PLUGINS.readValue(
                enforcedProperties,
                { it.split(File.pathSeparatorChar).mapToSet(::File) },
                default = emptySet()
            )
        )

    private fun computeCustomKlibs(enforcedProperties: EnforcedProperties): CustomKlibs =
        CustomKlibs(
            ClassLevelProperty.CUSTOM_KLIBS.readValue(
                enforcedProperties,
                { it.split(File.pathSeparatorChar).mapToSet(::File) },
                default = emptySet()
            )
        )

    private fun computeTestKind(enforcedProperties: EnforcedProperties): TestKind =
        ClassLevelProperty.TEST_KIND.readValue(
            enforcedProperties,
            TestKind.values(),
            default = TestKind.REGULAR
        )

    private fun computeForcedNoopTestRunner(enforcedProperties: EnforcedProperties): ForcedNoopTestRunner =
        ForcedNoopTestRunner(
            ClassLevelProperty.COMPILE_ONLY.readValue(
                enforcedProperties,
                String::toBooleanStrictOrNull,
                default = false
            )
        )

    private fun computeSharedExecutionTestRunner(enforcedProperties: EnforcedProperties): SharedExecutionTestRunner =
        SharedExecutionTestRunner(
            ClassLevelProperty.SHARED_TEST_EXECUTION.readValue(
                enforcedProperties,
                String::toBooleanStrictOrNull,
                default = false
            )
        )

    private fun computeTimeouts(enforcedProperties: EnforcedProperties, output: MutableCollection<Any>): Timeouts {
        var executionTimeout = ClassLevelProperty.EXECUTION_TIMEOUT.readValue(
            enforcedProperties,
            { Duration.parseOrNull(it) },
            default = Timeouts.DEFAULT_EXECUTION_TIMEOUT
        )

        // Aggressively adjust timeout in case of an aggressive scheduler
        val scheduler = output.filterIsInstance<GCScheduler>().firstOrNull()
        if (scheduler?.scheduler == GCSchedulerType.AGGRESSIVE) {
            executionTimeout *= 2
        }

        return Timeouts(executionTimeout)
    }

    private fun computeXCTestRunner(enforcedProperties: EnforcedProperties, nativeTargets: KotlinNativeTargets) = XCTestRunner(
        ClassLevelProperty.XCTEST_FRAMEWORK.readValue(
            enforcedProperties,
            String::toBooleanStrictOrNull,
            default = false
        ),
        nativeTargets
    )

    /*************** Test class settings (for black box tests only) ***************/

    private fun ExtensionContext.getOrCreateTestClassSettings(): TestClassSettings =
        root.getStore(NAMESPACE).getOrComputeIfAbsent(testClassKeyFor<TestClassSettings>()) {
            val enclosingTestClass = enclosingTestClass

            val testProcessSettings = getOrCreateTestProcessSettings()
            val computedTestConfiguration = computeTestConfiguration(enclosingTestClass).run {
                if (TestGroupCreation.getFromProperty() == TestGroupCreation.EAGER &&
                    configuration.providerClass == ExtTestCaseGroupProvider::class
                ) {
                    val annotation = UseEagerExtTestCaseGroupProvider()
                    val testConfiguration = annotation.annotationClass.findAnnotation<TestConfiguration>()
                        ?: error("Unable to find annotation for Eager test group creation")
                    ComputedTestConfiguration(testConfiguration, annotation)
                } else {
                    this
                }
            }

            val settings = buildList {
                // Put common settings:
                val nativeTargets = addCommonTestClassSettingsTo(enclosingTestClass, this)

                // Put settings that are always required:
                this += computedTestConfiguration
                this += computeBinariesForBlackBoxTests(testProcessSettings.get(), nativeTargets, enclosingTestClass)

                // Add custom settings:
                computedTestConfiguration.configuration.requiredSettings.forEach { clazz ->
                    this += when (clazz) {
                        TestRoots::class -> computeTestRoots(enclosingTestClass)
                        GeneratedSources::class -> computeGeneratedSourceDirs(testProcessSettings.get(), nativeTargets, enclosingTestClass)
                        DisabledTestDataFiles::class -> computeDisabledTestDataFiles(enclosingTestClass)
                        else -> fail { "Unknown test class setting type: $clazz" }
                    }
                }
            }

            TestClassSettings(parent = testProcessSettings, settings)
        } as TestClassSettings

    private fun computeTestConfiguration(enclosingTestClass: Class<*>): ComputedTestConfiguration {
        val findTestConfiguration: Class<*>.() -> ComputedTestConfiguration? = {
            annotations.asSequence().mapNotNull { annotation ->
                val testConfiguration = try {
                    annotation.annotationClass.findAnnotation<TestConfiguration>() ?: return@mapNotNull null
                } catch (e: UnsupportedOperationException) {
                    // For repeatable annotations we can't get the annotations of the annotation class,
                    // this class is actually a synthetic container.
                    return@mapNotNull null
                }
                ComputedTestConfiguration(testConfiguration, annotation)
            }.firstOrNull()
        }

        return enclosingTestClass.findTestConfiguration()
            ?: enclosingTestClass.declaredClasses.firstNotNullOfOrNull { it.findTestConfiguration() }
            ?: fail { "No @${TestConfiguration::class.simpleName} annotation found on test classes" }
    }

    private fun computeDisabledTestDataFiles(enclosingTestClass: Class<*>): DisabledTestDataFiles {
        val filesAndDirectories = buildSet {
            fun contributeSourceLocations(sourceLocations: Array<String>) {
                sourceLocations.forEach { expandGlobTo(getAbsoluteFile(it), this) }
            }

            fun recurse(clazz: Class<*>) {
                clazz.allInheritedAnnotations.forEach { annotation ->
                    when (annotation) {
                        is DisabledTests -> contributeSourceLocations(annotation.sourceLocations)
                        is DisabledTestsIfProperty -> if (System.getProperty(annotation.property.propertyName) == annotation.propertyValue) {
                            contributeSourceLocations(annotation.sourceLocations)
                        }
                    }
                }
                clazz.declaredClasses.forEach(::recurse)
            }

            recurse(enclosingTestClass)
        }

        return DisabledTestDataFiles(filesAndDirectories)
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
        enclosingTestClass: Class<*>,
    ): GeneratedSources {
        val testSourcesDir = baseDirs.testBuildDir
            .resolve("bb.src") // "bb" for black box
            .resolve("${targets.testTarget.compressedName}_${enclosingTestClass.compressedSimpleName}")
            .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale generated sources.

        val sharedSourcesDir = testSourcesDir
            .resolve(SHARED_MODULES_DIR_NAME)
            .ensureExistsAndIsEmptyDirectory()

        return GeneratedSources(testSourcesDir, sharedSourcesDir)
    }

    /** See also [computeBinariesForSimpleTests] */
    private fun computeBinariesForBlackBoxTests(
        baseDirs: BaseDirs,
        targets: KotlinNativeTargets,
        enclosingTestClass: Class<*>,
    ): Binaries {
        val testBinariesDir = baseDirs.testBuildDir
            .resolve("bb.out") // "bb" for black box
            .resolve("${targets.testTarget.compressedName}_${enclosingTestClass.compressedSimpleName}")
            .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale artifacts.

        return Binaries(
            testBinariesDir = testBinariesDir,
            lazySharedBinariesDir = { testBinariesDir.resolve(SHARED_MODULES_DIR_NAME).ensureExistsAndIsEmptyDirectory() },
            lazyGivenBinariesDir = { testBinariesDir.resolve(GIVEN_MODULES_DIR_NAME).ensureExistsAndIsEmptyDirectory() }
        )
    }

    private fun computePipelineType(enforcedProperties: EnforcedProperties, testClass: Class<*>): PipelineType {
        val pipelineTypeFromPipelineAnnotation = if (testClass.annotations.any { it is ClassicPipeline })
            PipelineType.K1
        else PipelineType.K2

        return ClassLevelProperty.PIPELINE_TYPE.readValue(
            enforcedProperties,
            PipelineType.entries.toTypedArray(),
            default = pipelineTypeFromPipelineAnnotation
        )
    }

    private fun computeUsedPartialLinkageConfig(enclosingTestClass: Class<*>): UsedPartialLinkageConfig {
        val findPartialLinkageMode: (Class<*>) -> UsePartialLinkage.Mode? = { clazz ->
            clazz.allInheritedAnnotations.firstIsInstanceOrNull<UsePartialLinkage>()?.mode
        }

        val mode = findPartialLinkageMode(enclosingTestClass)
            ?: enclosingTestClass.declaredClasses.firstNotNullOfOrNull { findPartialLinkageMode(it) }
            ?: UsePartialLinkage.Mode.ENABLED_WITH_ERROR // The default mode.

        val config = when (mode) {
            UsePartialLinkage.Mode.DISABLED -> PartialLinkageConfig(PartialLinkageMode.DISABLE, PartialLinkageLogLevel.ERROR)
            UsePartialLinkage.Mode.DEFAULT -> PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.DEFAULT)
            UsePartialLinkage.Mode.ENABLED_WITH_ERROR -> PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.ERROR)
        }

        return UsedPartialLinkageConfig(config)
    }

    private fun computeBinaryLibraryKind(enforcedProperties: EnforcedProperties): BinaryLibraryKind =
        ClassLevelProperty.BINARY_LIBRARY_KIND.readValue(enforcedProperties, BinaryLibraryKind.values(), BinaryLibraryKind.STATIC)

    private fun computeCInterfaceMode(enforcedProperties: EnforcedProperties): CInterfaceMode =
        ClassLevelProperty.C_INTERFACE_MODE.readValue(enforcedProperties, CInterfaceMode.values(), CInterfaceMode.NONE)

    /*************** Test class settings (simplified) ***************/

    private fun ExtensionContext.getOrCreateSimpleTestClassSettings(): SimpleTestClassSettings =
        root.getStore(NAMESPACE).getOrComputeIfAbsent(testClassKeyFor<SimpleTestClassSettings>()) {
            SimpleTestClassSettings(
                parent = getOrCreateTestProcessSettings(),
                buildList { addCommonTestClassSettingsTo(enclosingTestClass, this) }
            )
        } as SimpleTestClassSettings

    /*************** Test run settings (for black box tests only) ***************/

    // Note: TestRunSettings is not cached!
    fun ExtensionContext.createTestRunSettings(
        testInstances: NativeTestInstances<*>,
        defaultDirectives: ((TestClassSettings) -> RegisteredDirectives) = { RegisteredDirectives.Empty },
    ): TestRunSettings {
        val testClassSettings = getOrCreateTestClassSettings()

        return TestRunSettings(
            parent = testClassSettings,
            listOfNotNull(
                testInstances,
                testInstances.externalSourceTransformersProvider?.let { ExternalSourceTransformersProvider::class to it },
                RegisteredDirectives::class to defaultDirectives(testClassSettings)
            )
        )
    }

    internal fun ExtensionContext.computeBlackBoxTestInstances(): NativeTestInstances<AbstractNativeBlackBoxTest> =
        NativeTestInstances(requiredTestInstances.allInstances)

    /*************** Test run settings (simplified) ***************/

    // Note: SimpleTestRunSettings is not cached!
    fun ExtensionContext.createSimpleTestRunSettings(
        defaultDirectives: ((SimpleTestClassSettings) -> RegisteredDirectives) = { RegisteredDirectives.Empty },
    ): SimpleTestRunSettings {
        val testClassSettings = getOrCreateSimpleTestClassSettings()

        return SimpleTestRunSettings(
            parent = testClassSettings,
            listOf(
                computeSimpleTestInstances(),
                computeBinariesForSimpleTests(testClassSettings.get(), testClassSettings.get()),
                RegisteredDirectives::class to defaultDirectives(testClassSettings)
            )
        )
    }

    private fun ExtensionContext.computeSimpleTestInstances() =
        NativeTestInstances<AbstractNativeSimpleTest>(requiredTestInstances.allInstances)

    /** See also [computeBinariesForBlackBoxTests] */
    private fun ExtensionContext.computeBinariesForSimpleTests(baseDirs: BaseDirs, targets: KotlinNativeTargets): Binaries {
        val compressedClassNames = testClasses.map(Class<*>::compressedSimpleName).joinToString(separator = "_")

        val testBinariesDir = baseDirs.testBuildDir
            .resolve("s") // "s" for simple
            .resolve("${targets.testTarget.compressedName}_$compressedClassNames")
            .resolve(requiredTestMethod.name)
            .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale artifacts.

        return Binaries(
            testBinariesDir = testBinariesDir,
            lazySharedBinariesDir = { testBinariesDir.resolve(SHARED_MODULES_DIR_NAME).ensureExistsAndIsEmptyDirectory() },
            lazyGivenBinariesDir = { testBinariesDir.resolve(GIVEN_MODULES_DIR_NAME).ensureExistsAndIsEmptyDirectory() }
        )
    }

    /*************** Test run provider (for black box tests only) ***************/

    fun ExtensionContext.getOrCreateTestRunProvider(): TestRunProvider =
        root.getStore(NAMESPACE).getOrComputeIfAbsent(testClassKeyFor<TestRunProvider>()) {
            val testCaseGroupProvider = createTestCaseGroupProvider(getOrCreateTestClassSettings().get())
            TestRunProvider(testCaseGroupProvider)
        } as TestRunProvider

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

        return constructor.call(*arguments.toTypedArray())
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
