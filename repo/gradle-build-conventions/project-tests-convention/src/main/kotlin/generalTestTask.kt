/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.CommandLineArgumentProvider
import java.io.File
import java.nio.file.Files
import javax.inject.Inject


abstract class GeneralTestArgumentProvider @Inject constructor() : CommandLineArgumentProvider {
    @get:Inject
    protected abstract val providers: ProviderFactory

    @get:Internal
    abstract val projectName: Property<String>

    @get:Internal
    abstract val taskName: Property<String>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    val excludesFile: Provider<File> = providers.environmentVariable("TEAMCITY_PARALLEL_TESTS_ARTIFACT_PATH")
        .map { File(it) }
        .filter { it.exists() }

    @get:Internal
    val tempDir: Provider<String> =
        providers.environmentVariable("TMPDIR").orElse(providers.systemProperty("java.io.tmpdir"))

    @get:Internal
    val prefix = projectName.zip(taskName) { projectName, taskName -> "${projectName}Project_${taskName}_" }

    /**
     * Directory for unified JVM GC logs (`-Xlog:gc*`) of the forked test JVMs.
     * Intentionally not an output: the logs are diagnostics for OOM investigations and must not affect caching.
     */
    @get:Internal
    abstract val gcLogDirectory: DirectoryProperty

    /** Major version of the JDK that launches the tests; GC logging flags differ between JDK 8 and unified logging (9+). */
    @get:Internal
    abstract val javaMajorVersion: Property<Int>

    override fun asArguments(): Iterable<String?> = buildList {
        excludesFile.orNull?.let { add("-Dteamcity.build.parallelTests.excludesFile=${it.path}") }
        tempDir.orNull?.let { add("-Djava.io.tmpdir=" + Files.createTempDirectory(File(it).toPath(), prefix.get()).toString()) }
        gcLogDirectory.orNull?.let { dir ->
            // The JVM refuses to start if the log file cannot be created, so the directory has to exist up front.
            val logDir = dir.asFile.apply { mkdirs() }
            // `%p` is expanded to the PID by the JVM, so every forked test JVM writes its own file.
            // Rotation keeps the last 100 MB per JVM, which covers the run-up to an OOM.
            val logFile = logDir.resolve("gc-%p.log")
            if (javaMajorVersion.get() >= 9) {
                add("-Xlog:gc*:file=$logFile:time,uptime,level,tags:filecount=5,filesize=20m")
            } else {
                // JDK 8 has no unified logging (`-Xlog` is rejected as an unrecognized option).
                add("-Xloggc:$logFile")
                add("-XX:+PrintGCDetails")
                add("-XX:+PrintGCDateStamps")
                add("-XX:+UseGCLogFileRotation")
                add("-XX:NumberOfGCLogFiles=5")
                add("-XX:GCLogFileSize=20M")
            }
        }
    }
}

val testMaxHeapSizeTiny get() = 256.MiB
val testMaxHeapSizeSmall get() = 1.GiB
val testMaxHeapSizeMedium get() = 2.GiB
val testMaxHeapSizeLarge get() = 4.GiB
val testMaxHeapSizeHuge get() = 8.GiB

internal val testDefaultMaxHeapSize = testMaxHeapSizeMedium
internal val testDefaultMinHeapSize = 64.MiB
internal val testDefaultMaxMetaspaceSize = 512.MiB
internal val testDefaultReservedCodeCacheSize = 256.MiB
internal val testDefaultGC = GarbageCollector.G1

internal fun Project.createGeneralTestTask(
    taskName: String = "test",
    javaLauncher: JdkMajorVersion = DEFAULT_JAVA_LAUNCHER_FOR_TESTS,
    maxHeapSize: Size = testDefaultMaxHeapSize,
    minHeapSize: Size = testDefaultMinHeapSize,
    maxMetaspaceSize: Size = testDefaultMaxMetaspaceSize,
    reservedCodeCacheSize: Size = testDefaultReservedCodeCacheSize,
    garbageCollector: GarbageCollector? = testDefaultGC,
    defineJDKEnvVariables: List<JdkMajorVersion> = emptyList(),
    body: Test.() -> Unit = {},
): TaskProvider<Test> {

    val properties = kotlinBuildProperties
    val effectiveXmx = properties.testXmx.orElse(maxHeapSize)
    val effectiveXms = properties.testXms.orElse(minHeapSize)
    val effectiveGC = properties.testGarbageCollector.orElse(provider { garbageCollector })

    val shouldInstrument = project.providers.gradleProperty("kotlin.test.instrumentation.disable")
        .orNull?.toBoolean() != true

    val testTaskInitializer = fun Test.() {
        this.javaLauncher.set(getToolchainLauncherFor(javaLauncher))

        // Only `test` gets its classpath from the java plugin; other tasks, including `testDataManagerWarmup`
        // (configured with `taskName == "test"`), need this fallback. Checking the name first also avoids
        // resolving the classpath of `test` during configuration.
        if (name != "test" && classpath.isEmpty) {
            classpath = sourceSets.getByName("test").runtimeClasspath
            testClassesDirs = sourceSets.getByName("test").output.classesDirs
        }
        val ideaHomeForTests =
            this.project.configurations.detachedConfiguration(this.project.dependencies.project(":", configuration = "ideaHomeForTests"))
        jvmArgumentProviders.add(this.project.objects.newInstance(SystemPropertyClasspathDirectoryProvider::class.java).apply {
            property.set("idea.home.path")
            classpath.from(ideaHomeForTests)
            directory.value(ideaHomePathForTests())
        })

        if (shouldInstrument) {
            val agentJar = configurations.detachedConfiguration(dependencies.project(":test-instrumenter")).apply { isTransitive = false }
            val bootClasspathJar = configurations.detachedConfiguration(dependencies.project(":test-instrumenter", "bootClasspath"))
            val debugProperty = kotlinBuildProperties.booleanProperty("test.instrumenter.debug")

            systemProperty("test.instrumenter.debug", debugProperty.get())

            val testInstrumentationProvider = objects.newInstance<TestInstrumentationArgumentProvider>().apply {
                this.agentJar.from(agentJar)
                this.bootClasspathJar.from(bootClasspathJar)
                this.debug.set(debugProperty)
            }
            jvmArgumentProviders.add(testInstrumentationProvider)
        }

        // The glibc default number of memory pools on 64bit systems is 8 times the number of CPU cores
        // Choosing a value MALLOC_ARENA_MAX is generally a tradeoff between performance and memory consumption.
        // Not setting MALLOC_ARENA_MAX gives the best performance, but may mean higher memory use.
        // Setting MALLOC_ARENA_MAX to “2” or “1” makes glibc use fewer memory pools and potentially less memory,
        // but this may reduce performance.
        environment("MALLOC_ARENA_MAX", "2")

        jvmArgs(
            "-ea",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-XX:+UseCodeCacheFlushing",
            "-XX:ReservedCodeCacheSize=${reservedCodeCacheSize.toJvmArg()}",
            "-XX:MaxMetaspaceSize=${maxMetaspaceSize.toJvmArg()}",
            "-Djna.nosys=true",
        )

        when (effectiveGC.orNull) {
            GarbageCollector.G1 -> jvmArgs("-XX:+UseG1GC")
            GarbageCollector.Parallel -> jvmArgs("-XX:+UseParallelGC")
            null -> Unit
        }

        val nativeMemoryTracking = project.providers.gradleProperty("kotlin.build.test.process.NativeMemoryTracking")
        if (nativeMemoryTracking.isPresent) {
            jvmArgs("-XX:NativeMemoryTracking=${nativeMemoryTracking.get()}")
        }

        this.maxHeapSize = effectiveXmx.get().toJvmArg()
        this.minHeapSize = effectiveXms.get().toJvmArg()

        systemProperty("idea.is.unit.test", "true")
        systemProperty("idea.use.native.fs.for.win", false)
        systemProperty("java.awt.headless", "true")
        environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
        environment("PROJECT_BUILD_DIR", project.layout.buildDirectory.get().asFile)
        systemProperty(
            "kotlin.test.update.test.data",
            project.kotlinBuildProperties.booleanProperty("kotlin.test.update.test.data", false).get()
        )
        systemProperty("cacheRedirectorEnabled", project.kotlinBuildProperties.isCacheRedirectorEnabled.get())
        project.kotlinBuildProperties.junit5NumberOfThreadsForParallelExecution?.let { n ->
            systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
            systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", n)
        }

        val testArgumentProvider = objects.newInstance<GeneralTestArgumentProvider>().also {
            it.projectName.set(project.name)
            it.taskName.set(name)
            it.gcLogDirectory.set(project.layout.buildDirectory.dir("test-gc-logs/$name"))
            // Read from the task's final launcher, not the `javaLauncher` parameter:
            // some modules reassign `Test.javaLauncher` in their own configuration (e.g. fir2ir to JDK 8).
            it.javaMajorVersion.set(this.javaLauncher.map { launcher -> launcher.metadata.languageVersion.asInt() })
        }
        jvmArgumentProviders.add(testArgumentProvider)

        systemProperty("idea.ignore.disabled.plugins", "true")

        doFirst {
            // workaround for a Gradle bug: https://github.com/gradle/gradle/issues/37539
            // the tests won't be skipped by Gradle but will be disabled by TCParallelTestsExecutionCondition
            // this can be removed after Gradle updated to a version with the fix (likely 9.6.0)
            val excludesFile = testArgumentProvider.excludesFile
            if (excludesFile.isPresent) {
                logger.warn("Removing excludes set by TeamCity")
                val parallelTestsExcludes = File(excludesFile.get().path).readLines().filter { !it.startsWith("#") }.toSet()
                filter.excludePatterns.removeAll(parallelTestsExcludes)
            }
        }

        val fs = project.serviceOf<FileSystemOperations>()
        doLast {
            File(testArgumentProvider.tempDir.get(), testArgumentProvider.prefix.get()).let {
                try {
                    fs.delete {
                        delete(it)
                    }
                } catch (e: Exception) {
                    logger.warn("Can't delete test temp root folder $it", e.printStackTrace())
                }
            }
        }

        if (!kotlinBuildProperties.isTeamcityBuild.get()) {
            defineJDKEnvVariables.forEach { version ->
                val jdkHome = project.getToolchainJdkHomeFor(version).orNull ?: error("Can't find toolchain for $version")
                environment(version.envName, jdkHome)
            }
        }
        body()
    }

    // A special mock test task is required for the test data manager
    // to be able to fully reuse its configuration without forcing real tests execution.
    // Mirrors only `test`: other general test tasks (e.g., `codebaseTest`) must not add their configuration to it.
    // `getOrCreateTask` like for `test` itself, so a repeated `testTask { ... }` configures both the same way.
    if (taskName == "test") {
        project.pluginManager.withPlugin("test-data-manager") {
            getOrCreateTask<Test>("testDataManagerWarmup") {
                description = "Carries Test configuration for test-data-manager"

                testTaskInitializer()
                onlyIf("configuration carrier; tests must not be executed") {
                    false
                }
            }
        }
    }

    return getOrCreateTask<Test>(taskName, testTaskInitializer)
}

private val Test.commandLineIncludePatterns: Set<String>
    get() = (filter as? DefaultTestFilter)?.commandLineIncludePatterns.orEmpty()

private inline fun String.isFirstChar(f: (Char) -> Boolean) = isNotEmpty() && f(first())
