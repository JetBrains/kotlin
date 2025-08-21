import TestProperty.*
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.environment
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

private enum class TestProperty(shortName: String) {
    // Use a separate Gradle property to pass Kotlin/Native home to tests: "kotlin.internal.native.test.nativeHome".
    // Don't use "kotlin.native.home" and similar properties for this purpose, as these properties may have undesired
    // effect on other Gradle tasks (ex: :kotlin-native:dist) that might be executed along with test task.
    KOTLIN_NATIVE_HOME("nativeHome"),
    COMPILER_CLASSPATH("compilerClasspath"),
    COMPILER_PLUGINS("compilerPlugins"),
    CUSTOM_KLIBS("customKlibs"),
    CUSTOM_KOTLIN_NATIVE_HOME("customNativeHome"),
    TEST_TARGET("target"),
    TEST_MODE("mode"),
    TEST_KIND("testKind"),
    FORCE_STANDALONE("forceStandalone"), // This is not passed directly into the test infra but transformed into TEST_KIND.
    COMPILE_ONLY("compileOnly"),
    OPTIMIZATION_MODE("optimizationMode"),
    USE_THREAD_STATE_CHECKER("useThreadStateChecker"),
    GC_TYPE("gcType"),
    GC_SCHEDULER("gcScheduler"),
    ALLOCATOR("alloc"),
    CACHE_MODE("cacheMode"),
    EXECUTION_TIMEOUT("executionTimeout"),
    SANITIZER("sanitizer"),
    BINARY_OPTIONS("binaryOptions"),
    SHARED_TEST_EXECUTION("sharedTestExecution"),
    EAGER_GROUP_CREATION("eagerGroupCreation"),
    XCTEST_FRAMEWORK("xctest"),
    TEAMCITY("teamcity"),
    MINIDUMP_ANALYZER("minidumpAnalyzer"),
    ;

    val fullName = "kotlin.internal.native.test.$shortName"

    /**
     * Shorter version to make DX a bit nicer.
     */
    val shortName = "kn.$shortName"
}

private open class NativeArgsProvider @Inject constructor(
    project: Project,
    objects: ObjectFactory,
    providers: ProviderFactory,
    @Internal val requirePlatformLibs: Boolean = false,
) : CommandLineArgumentProvider {
    @get:Input
    @get:Optional
    protected val testTarget = providers.testProperty(TEST_TARGET)

    @get:Input
    @get:Optional
    protected val testMode = providers.testProperty(TEST_MODE)

    @get:Input
    @get:Optional
    protected val forceStandalone = providers.testProperty(FORCE_STANDALONE)

    @get:Input
    @get:Optional
    protected val testKind = providers.testProperty(TEST_KIND).orElse(forceStandalone.map { "STANDALONE" })

    @get:Input
    @get:Optional
    protected val compileOnly = providers.testProperty(COMPILE_ONLY)

    @get:Input
    @get:Optional
    protected val optimizationMode = providers.testProperty(OPTIMIZATION_MODE)

    @get:Input
    @get:Optional
    protected val useThreadStateChecker = providers.testProperty(USE_THREAD_STATE_CHECKER)

    @get:Input
    @get:Optional
    protected val gcType = providers.testProperty(GC_TYPE)

    @get:Input
    @get:Optional
    protected val binaryOptions = providers.testProperty(BINARY_OPTIONS)

    @get:Input
    @get:Optional
    protected val gcScheduler = providers.testProperty(GC_SCHEDULER)

    @get:Input
    @get:Optional
    protected val allocator = providers.testProperty(ALLOCATOR)

    @get:Input
    @get:Optional
    protected val cacheMode = providers.testProperty(CACHE_MODE)

    @get:Input
    @get:Optional
    protected val executionTimeout = providers.testProperty(EXECUTION_TIMEOUT)

    @get:Input
    @get:Optional
    protected val sanitizer = providers.testProperty(SANITIZER)

    @get:Input
    @get:Optional
    protected val sharedTestExecution = providers.testProperty(SHARED_TEST_EXECUTION)

    @get:Input
    @get:Optional
    protected val eagerGroupCreation = providers.testProperty(EAGER_GROUP_CREATION)

    @get:Input
    @get:Optional
    protected val xctestFramework = providers.testProperty(XCTEST_FRAMEWORK)

    @get:Input
    protected val teamcity: Boolean = project.kotlinBuildProperties.isTeamcityBuild

    @get:Internal
    protected val customNativeHome: Provider<String?> = providers.testProperty(KOTLIN_NATIVE_HOME)

    @get:Classpath
    val customCompilerDependencies: ConfigurableFileCollection = objects.fileCollection()

    @get:Classpath
    val compilerPluginDependencies: ConfigurableFileCollection = objects.fileCollection()

    @get:Classpath
    val customTestDependencies: ConfigurableFileCollection = objects.fileCollection()

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    val customCompilerDist: DirectoryProperty = objects.directoryProperty()

    @get:Input
    val testTargetWithDefault = testTarget.orElse(HostManager.hostName)

    @get:Internal
    protected val internalNativeHomeDir: Provider<File> = customNativeHome.map { File(it) }
        .orElse(project.project(":kotlin-native").isolated.projectDirectory.dir("dist").asFile)

    @get:Classpath
    protected val nativeHome: ConfigurableFileCollection = objects.fileCollection().apply {
        if (customNativeHome.isPresent) {
            from(customNativeHome)
        } else {
            val nativeHomeBuiltBy: Provider<List<String>> = testTarget.map {
                listOfNotNull(
                    ":kotlin-native:${it}CrossDist",
                    if (requirePlatformLibs) ":kotlin-native:${it}PlatformLibs" else null,
                )
            }.orElse(
                listOfNotNull(
                    ":kotlin-native:dist",
                    if (requirePlatformLibs) ":kotlin-native:distPlatformLibs" else null,
                )
            )

            val distDir = project.project(":kotlin-native").isolated.projectDirectory.dir("dist")
            if (!requirePlatformLibs) {
                from(distDir.dir("bin/"))
                from(distDir.dir("konan/"))
                from(distDir.dir("tools/"))
                from(distDir.dir("klib/common/"))
                from(distDir.dir("klib/cache/${testTargetWithDefault.get()}-gSTATIC-system/stdlib-cache/"))
            } else {
                from(distDir)
            }
            builtBy(nativeHomeBuiltBy.get())
        }
    }

    @Classpath
    protected val compilerClasspath: ConfigurableFileCollection = objects.fileCollection().apply {
        if (customNativeHome.isPresent) {
            from(customNativeHome.map { File(it, "konan/lib/kotlin-native-compiler-embeddable.jar") })
        } else {
            from(
                project.configurations.detachedConfiguration(
                    project.dependencies.project(":kotlin-native:prepare:kotlin-native-compiler-embeddable"),
                )
            )
        }
        from(customCompilerDependencies)
    }

    @get:Classpath
    val xcTestConfiguration: ConfigurableFileCollection = objects.fileCollection().apply {
        val xcTestEnabled = xctestFramework.map { it == "true" }.orElse(false)
        val isAppleTarget: Provider<Boolean> =
            testTargetWithDefault.map { KonanTarget.predefinedTargets[it]?.family?.isAppleFamily ?: false }.orElse(false)
        if (xcTestEnabled.get() && isAppleTarget.get()) {
            from(project.configurations.create("kotlinTestNativeXCTest") {
                isTransitive = false
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
                    attribute(KotlinNativeTarget.konanTargetAttribute, testTargetWithDefault.get())
                }
                dependencies.add(project.dependencies.project(path = ":native:kotlin-test-native-xctest"))
            })
        }
    }

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE) // it doesn't matter at all how is this analyzer named, or where it's placed
    @get:Optional
    protected val minidumpAnalyzer: ConfigurableFileCollection = objects.fileCollection().apply {
        if (HostManager.hostIsMac && !project.hasProperty("disableBreakpad")) {
            val fileCollection = project.configurations.detachedConfiguration(
                project.dependencies.project(":kotlin-native:tools:minidump-analyzer"),
            ).also {
                it.attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-executable"))
                }
            }
            from(fileCollection)
        }
    }

    override fun asArguments(): Iterable<String> {
        val customKlibs = customTestDependencies.files + xcTestConfiguration.files
        return listOfNotNull(
            "-D${KOTLIN_NATIVE_HOME.fullName}=${internalNativeHomeDir.get().absolutePath}",
            "-D${COMPILER_CLASSPATH.fullName}=${compilerClasspath.files.takeIf { it.isNotEmpty() }?.joinToString(File.pathSeparator) { it.absolutePath }}",
            "-D${COMPILER_PLUGINS.fullName}=${compilerPluginDependencies.files.joinToString(File.pathSeparator) { it.absolutePath }}".takeIf { !compilerPluginDependencies.isEmpty },
            testKind.orNull?.let { "-D${TEST_KIND.fullName}=$it" },
            "-D${TEAMCITY.fullName}=$teamcity",
            customCompilerDist.orNull?.let { "-D${CUSTOM_KOTLIN_NATIVE_HOME.fullName}=${it.asFile.absolutePath}" },
            testTarget.orNull?.let { "-D${TEST_TARGET.fullName}=$it" },
            testMode.orNull?.let { "-D${TEST_MODE.fullName}=$it" },
            compileOnly.orNull?.let { "-D${COMPILE_ONLY.fullName}=$it" },
            optimizationMode.orNull?.let { "-D${OPTIMIZATION_MODE.fullName}=$it" },
            useThreadStateChecker.orNull?.let { "-D${USE_THREAD_STATE_CHECKER.fullName}=$it" },
            gcType.orNull?.let { "-D${GC_TYPE.fullName}=$it" },
            binaryOptions.orNull?.let { "-D${BINARY_OPTIONS.fullName}=$it" },
            gcScheduler.orNull?.let { "-D${GC_SCHEDULER.fullName}=$it" },
            allocator.orNull?.let { "-D${ALLOCATOR.fullName}=$it" },
            cacheMode.orNull?.let { "-D${CACHE_MODE.fullName}=$it" },
            executionTimeout.orNull?.let { "-D${EXECUTION_TIMEOUT.fullName}=$it" },
            sanitizer.orNull?.let { "-D${SANITIZER.fullName}=$it" },
            sharedTestExecution.orNull?.let { "-D${SHARED_TEST_EXECUTION.fullName}=$it" },
            eagerGroupCreation.orNull?.let { "-D${EAGER_GROUP_CREATION.fullName}=$it" },
            xctestFramework.orNull?.let { "-D${XCTEST_FRAMEWORK.fullName}=$it" },
            "-D${CUSTOM_KLIBS.fullName}=${customKlibs.joinToString(File.pathSeparator) { it.absolutePath }}".takeIf { customKlibs.isNotEmpty() },
            if (minidumpAnalyzer.isEmpty) null else "-D${MINIDUMP_ANALYZER.fullName}=${minidumpAnalyzer.singleFile.absolutePath}",
        )
    }
}

private fun ProviderFactory.testProperty(property: TestProperty) =
    gradleProperty(property.fullName).orElse(gradleProperty(property.shortName))

/**
 * @param taskName Name of Gradle task.
 * @param tag Optional JUnit test tag. See https://junit.org/junit5/docs/current/user-guide/#writing-tests-tagging-and-filtering
 * @param requirePlatformLibs Where platform KLIBs from the Kotlin/Native distribution are required for running this test.
 * @param customCompilerDependencies The [Configuration]s that provide additional JARs to be added to the compiler's classpath.
 * @param customTestDependencies The [Configuration]s that provide KLIBs to be added to Kotlin/Native compiler dependencies list
 *   along with Kotlin/Native stdlib KLIB and Kotlin/Native platform KLIBs (the latter only if [requirePlatformLibs] is `true`).
 * @param compilerPluginDependencies The [Configuration]s that provide compiler plugins to be enabled for the Kotlin/Native compiler
 *   for the duration of test execution.
 * @param allowParallelExecution if false, force junit to execute test sequentially
 */
fun Project.nativeTest(
    taskName: String,
    tag: String?,
    requirePlatformLibs: Boolean = false,
    customCompilerDependencies: List<FileCollection> = emptyList(),
    customTestDependencies: List<FileCollection> = emptyList(),
    compilerPluginDependencies: List<FileCollection> = emptyList(),
    allowParallelExecution: Boolean = true,
    customCompilerDist: TaskProvider<Sync>? = null,
    maxMetaspaceSizeMb: Int = 512,
    defineJDKEnvVariables: List<JdkMajorVersion> = emptyList(),
    body: Test.() -> Unit = {},
) = projectTest(
    taskName,
    jUnitMode = JUnitMode.JUnit5,
    maxHeapSizeMb = 3072, // Extra heap space for Kotlin/Native compiler.
    maxMetaspaceSizeMb = maxMetaspaceSizeMb,
    defineJDKEnvVariables = defineJDKEnvVariables,
) {
    group = "verification"

    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        workingDir = rootDir

        // Use ARM64 JDK on ARM64 Mac as required by the K/N compiler.
        // See https://youtrack.jetbrains.com/issue/KTI-2421#focus=Comments-27-12231298.0-0.
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))

        // Using JDK 11 instead of JDK 8 (project default) makes some tests take 15-25% more time.
        // This seems to be caused by the fact that JDK 11 uses G1 GC by default, while JDK 8 uses Parallel GC.
        // Switch back to Parallel GC to mitigate the test execution time degradation:
        jvmArgs("-XX:+UseParallelGC")
        // Another reason for switching back to Parallel GC is CLI tests:
        // some of them validate the compiler performance report.
        // The latter contains GC statistics, and the format varies per GC.
        // So using G1 GC makes those tests fail because they expect the format of Parallel GC statistics.

        // Effectively remove the limit for the amount of stack trace elements in Throwable.
        jvmArgs("-XX:MaxJavaStackTraceDepth=1000000")

        // Double the stack size. This is needed to compile some marginal tests with extra-deep IR tree, which requires a lot of stack frames
        // for visiting it. Example: codegen/box/strings/concatDynamicWithConstants.kt
        // Such tests are successfully compiled in old test infra with the default 1 MB stack just by accident. New test infra requires ~55
        // additional stack frames more compared to the old one because of another launcher, etc. and it turns out this is not enough.
        jvmArgs("-Xss2m")

        jvmArgumentProviders.add(objects.newInstance(NativeArgsProvider::class.java, requirePlatformLibs).apply {
            this.customCompilerDependencies.from(customCompilerDependencies)
            this.compilerPluginDependencies.from(compilerPluginDependencies)
            this.customTestDependencies.from(customTestDependencies)
            if (customCompilerDist != null) {
                this.customCompilerDist.fileProvider(customCompilerDist.map { it.destinationDir })
            }
        })

        val availableCpuCores: Int = if (allowParallelExecution) Runtime.getRuntime().availableProcessors() else 1
        if (!kotlinBuildProperties.isTeamcityBuild
            && minOf(kotlinBuildProperties.junit5NumberOfThreadsForParallelExecution ?: 16, availableCpuCores) > 4
        ) {
            logger.info("$path JIT C2 compiler has been disabled")
            jvmArgs("-XX:TieredStopAtLevel=1") // Disable C2 if there are more than 4 CPUs at the host machine.
        }

        // Pass the current Gradle task name so test can use it in logging.
        environment("GRADLE_TASK_NAME", path)

        useJUnitPlatform {
            tag?.let { includeTags(it) }
        }

        if (!allowParallelExecution) {
            systemProperty("junit.jupiter.execution.parallel.enabled", "false")
        }

        doFirst {
            logger.info(
                buildString {
                    appendLine("$path parallel test execution parameters:")
                    append("  Available CPU cores = $availableCpuCores")
                    systemProperties.filterKeys { it.startsWith("junit.jupiter") }.toSortedMap().forEach { (key, value) ->
                        append("\n  $key = $value")
                    }
                }
            )
        }
    } else
        doFirst {
            throw GradleException(
                """
                    Can't run task $path. The Kotlin/Native part of the project is currently disabled.
                    Make sure that "kotlin.native.enabled" is set to "true" in local.properties file, or is passed
                    as a Gradle command-line parameter via "-Pkotlin.native.enabled=true".
                """.trimIndent()
            )
        }

    // This environment variable is here for two reasons:
    // 1. It is also used for the cinterop tool itself. So it is good to have it here as well,
    //    just to make the tests match the production as closely as possible.
    // 2. It disables a certain machinery in libclang that is known to cause troubles.
    //    (see e.g. https://youtrack.jetbrains.com/issue/KT-61299 for more details).
    //    Strictly speaking, it is not necessary since we beat them by other means,
    //    but it is still nice to have it as a failsafe.
    environment("LIBCLANG_DISABLE_CRASH_RECOVERY" to "1")

    body()
}