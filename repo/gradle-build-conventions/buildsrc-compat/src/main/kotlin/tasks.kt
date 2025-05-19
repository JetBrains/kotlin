/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


// usages in build scripts are not tracked properly
@file:Suppress("unused")

import com.sun.management.OperatingSystemMXBean
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File
import java.lang.Character.isLowerCase
import java.lang.Character.isUpperCase
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import kotlin.collections.toTypedArray

val kotlinGradlePluginAndItsRequired = arrayOf(
    ":kotlin-assignment",
    ":compose-compiler-gradle-plugin",
    ":kotlin-allopen",
    ":kotlin-noarg",
    ":kotlin-power-assert",
    ":kotlin-sam-with-receiver",
    ":kotlin-lombok",
    ":kotlin-serialization",
    ":kotlin-parcelize-compiler",
    ":kotlin-compiler-embeddable",
    ":native:kotlin-native-utils",
    ":kotlin-util-klib",
    ":kotlin-util-io",
    ":kotlin-compiler-runner",
    ":kotlin-daemon-embeddable",
    ":kotlin-daemon-client",
    ":kotlin-gradle-plugins-bom",
    ":kotlin-gradle-plugin-api",
    ":kotlin-gradle-plugin-annotations",
    ":kotlin-gradle-plugin-idea",
    ":kotlin-gradle-plugin-idea-proto",
    ":kotlin-gradle-plugin",
    ":kotlin-gradle-plugin-model",
    ":kotlin-tooling-metadata",
    ":kotlin-tooling-core",
    ":kotlin-reflect",
    ":kotlin-test",
    ":kotlin-gradle-subplugin-example",
    ":kotlin-stdlib-common",
    ":kotlin-stdlib",
    ":kotlin-stdlib-jdk7",
    ":kotlin-stdlib-jdk8",
    ":kotlin-dom-api-compat",
    ":examples:annotation-processor-example",
    ":kotlin-assignment-compiler-plugin.embeddable",
    ":kotlin-allopen-compiler-plugin.embeddable",
    ":kotlin-noarg-compiler-plugin.embeddable",
    ":kotlin-power-assert-compiler-plugin.embeddable",
    ":kotlin-sam-with-receiver-compiler-plugin.embeddable",
    ":kotlin-lombok-compiler-plugin.embeddable",
    ":kotlinx-serialization-compiler-plugin.embeddable",
    ":kotlin-annotation-processing-embeddable",
    ":plugins:compose-compiler-plugin:compiler",
    ":kotlin-script-runtime",
    ":kotlin-scripting-common",
    ":kotlin-scripting-jvm",
    ":kotlin-scripting-compiler-embeddable",
    ":kotlin-scripting-compiler-impl-embeddable",
    ":native:kotlin-klib-commonizer-embeddable",
    ":native:kotlin-klib-commonizer-api",
    ":native:swift:swift-export-embeddable",
    ":compiler:build-tools:kotlin-build-statistics",
    ":compiler:build-tools:kotlin-build-tools-api",
    ":compiler:build-tools:kotlin-build-tools-impl",
    ":libraries:tools:gradle:fus-statistics-gradle-plugin",
    ":kotlin-util-klib-metadata",
    ":libraries:tools:abi-validation:abi-tools-api",
    ":libraries:tools:abi-validation:abi-tools",
    ":kotlin-metadata-jvm",
)

fun Task.dependsOnKotlinGradlePluginInstall() {
    kotlinGradlePluginAndItsRequired.forEach { dependency ->
        dependsOn("${dependency}:install")
    }
}

fun Task.dependsOnKotlinGradlePluginPublish() {
    kotlinGradlePluginAndItsRequired
        .filter {
            // Compose compiler plugin does not assemble with LV 1.9 and should not be a part of the dist bundle for now
            it != ":plugins:compose-compiler-plugin:compiler"
        }
        .forEach { dependency ->
            project.rootProject.tasks.findByPath("${dependency}:publish")?.let { task ->
                dependsOn(task)
            }
        }
}

// Mixing JUnit4 and Junit5 in one module proved to be problematic, consider using separate modules instead
enum class JUnitMode {
    JUnit4, JUnit5
}

abstract class MuteWithDatabaseArgumentProvider @Inject constructor(objects: ObjectFactory) : CommandLineArgumentProvider {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val mutesFile: RegularFileProperty = objects.fileProperty()

    override fun asArguments(): Iterable<String> =
        listOf("-Dorg.jetbrains.kotlin.test.mutes.file=${mutesFile.get().asFile.canonicalPath}")
}

fun Test.muteWithDatabase() {
    jvmArgumentProviders.add(
        project.objects.newInstance<MuteWithDatabaseArgumentProvider>().apply {
            mutesFile.fileValue(File(project.rootDir, "tests/mute-common.csv"))
        })
    systemProperty("org.jetbrains.kotlin.skip.muted.tests", if (project.rootProject.hasProperty("skipMutedTests")) "true" else "false")
    // This system property is only useful for JUnit Platform, but it does no harm on JUnit4
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}

/*
 * Workaround for TW-92736
 * TC parallelTests.excludesFile may contain invalid entries leading to skipping large groups of tests.
 */
fun Test.cleanupInvalidExcludePatternsForTCParallelTests(excludesFilePath: String) {
    val candidateTestClassNames = mutableSetOf<String>()
    candidateClassFiles.visit {
        if (!isDirectory && name.endsWith(".class")) {
            candidateTestClassNames.add(path.substringBefore(".class").replace('/', '.'))
        }
    }

    val parallelTestsExcludes = File(excludesFilePath).readLines().filter { !it.startsWith("#") }.toSet()
    val excludePatterns = filter.excludePatterns

    parallelTestsExcludes.forEach {
        if (!candidateTestClassNames.contains(it)) {
            logger.warn("WARNING: parallelTests excludesFile contains class name missing in test classes: $it")
            logger.warn("Removing '$it.*' from `excludePatterns`")
            excludePatterns.remove("$it.*")
        }
    }

    filter.setExcludePatterns(*excludePatterns.toTypedArray())
}

/**
 * @param parallel is redundant if @param jUnit5Enabled is true, because
 *   JUnit5 supports parallel test execution by itself, without gradle help
 */
fun Project.projectTest(
    taskName: String = "test",
    parallel: Boolean = false,
    jUnitMode: JUnitMode = JUnitMode.JUnit4,
    maxHeapSizeMb: Int? = null,
    minHeapSizeMb: Int? = null,
    maxMetaspaceSizeMb: Int = 512,
    reservedCodeCacheSizeMb: Int = 256,
    defineJDKEnvVariables: List<JdkMajorVersion> = emptyList(),
    body: Test.() -> Unit = {},
): TaskProvider<Test> {
    if (jUnitMode == JUnitMode.JUnit5) {
        project.dependencies {
            "testImplementation"(project(":compiler:tests-mutes:mutes-junit5"))
        }
    } else {
        project.dependencies {
            "testImplementation"(project(":compiler:tests-mutes:mutes-junit4"))
        }
    }
    val shouldInstrument = project.providers.gradleProperty("kotlin.test.instrumentation.disable")
        .orNull?.toBoolean() != true
    if (shouldInstrument) {
        evaluationDependsOn(":test-instrumenter")
    }
    return getOrCreateTask<Test>(taskName) {
        inputs.files(rootProject.tasks.named("createIdeaHomeForTests").map { it.outputs.files }).withPathSensitivity(PathSensitivity.RELATIVE)

        muteWithDatabase()
        if (jUnitMode == JUnitMode.JUnit4) {
            jvmArgumentProviders.add {
                listOf(
                    "-javaagent:${classpath.find { it.name.contains("junit-foundation") }?.absolutePath ?:
                    error("junit-foundation not found in ${classpath.joinToString("\n")}")}"
                )
            }
        }

        doFirst {
            if (jUnitMode == JUnitMode.JUnit5) return@doFirst

            val commandLineIncludePatterns = commandLineIncludePatterns.toMutableSet()
            val patterns = filter.includePatterns + commandLineIncludePatterns
            if (patterns.isEmpty() || patterns.any { '*' in it }) return@doFirst
            patterns.forEach { pattern ->
                var isClassPattern = false
                val maybeMethodName = pattern.substringAfterLast('.')
                val maybeClassFqName = if (maybeMethodName.isFirstChar(::isLowerCase)) {
                    pattern.substringBeforeLast('.')
                } else {
                    isClassPattern = true
                    pattern
                }

                if (!maybeClassFqName.substringAfterLast('.').isFirstChar(::isUpperCase)) {
                    return@forEach
                }

                val classFileNameWithoutExtension = maybeClassFqName.replace('.', '/')
                val classFileName = "$classFileNameWithoutExtension.class"

                if (isClassPattern) {
                    val innerClassPattern = "$pattern$*"
                    if (pattern in commandLineIncludePatterns) {
                        commandLineIncludePatterns.add(innerClassPattern)
                        (filter as? DefaultTestFilter)?.setCommandLineIncludePatterns(commandLineIncludePatterns)
                    } else {
                        filter.includePatterns.add(innerClassPattern)
                    }
                }

                include { treeElement ->
                    val path = treeElement.path
                    if (treeElement.isDirectory) {
                        classFileNameWithoutExtension.startsWith(path)
                    } else {
                        if (path == classFileName) return@include true
                        if (!path.endsWith(".class")) return@include false
                        path.startsWith("$classFileNameWithoutExtension$")
                    }
                }
            }
        }

        if (shouldInstrument) {
            val instrumentationArgsProperty = project.providers.gradleProperty("kotlin.test.instrumentation.args")
            dependsOn(":test-instrumenter:jar")
            val testInstrumenterOutput = project.rootProject.subprojects.single { it.path == ":test-instrumenter" }
                .tasks.named("jar")
                .map { it.outputs.files.singleFile }
            doFirst {
                val agent = testInstrumenterOutput.get()
                val args = instrumentationArgsProperty.orNull?.let { "=$it" }.orEmpty()
                jvmArgs("-javaagent:$agent$args")
            }
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
            "-XX:ReservedCodeCacheSize=${reservedCodeCacheSizeMb}m",
            "-XX:MaxMetaspaceSize=${maxMetaspaceSizeMb}m",
            "-XX:CICompilerCount=2",
            "-Djna.nosys=true"
        )

        val nativeMemoryTracking = project.providers.gradleProperty("kotlin.build.test.process.NativeMemoryTracking")
        if (nativeMemoryTracking.isPresent) {
            jvmArgs("-XX:NativeMemoryTracking=${nativeMemoryTracking.get()}")
        }

        val junit5ParallelTestWorkers =
            project.kotlinBuildProperties.junit5NumberOfThreadsForParallelExecution ?: Runtime.getRuntime().availableProcessors()

        val memoryPerTestProcessMb = if (jUnitMode == JUnitMode.JUnit5)
            totalMaxMemoryForTestsMb.coerceIn(defaultMaxMemoryPerTestWorkerMb, defaultMaxMemoryPerTestWorkerMb * junit5ParallelTestWorkers)
        else
            defaultMaxMemoryPerTestWorkerMb

        maxHeapSize = "${maxHeapSizeMb ?: (memoryPerTestProcessMb - maxMetaspaceSizeMb - reservedCodeCacheSizeMb)}m"

        if (minHeapSizeMb != null) {
            minHeapSize = "${minHeapSizeMb}m"
        }

        systemProperty("idea.is.unit.test", "true")
        systemProperty("idea.home.path", project.ideaHomePathForTests().get().asFile.canonicalPath)
        systemProperty("idea.use.native.fs.for.win", false)
        systemProperty("java.awt.headless", "true")
        environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
        environment("PROJECT_CLASSES_DIRS", project.testSourceSet.output.classesDirs.asPath)
        environment("PROJECT_BUILD_DIR", project.layout.buildDirectory.get().asFile)
        systemProperty("kotlin.test.update.test.data", if (project.rootProject.hasProperty("kotlin.test.update.test.data")) "true" else "false")
        systemProperty("cacheRedirectorEnabled", project.rootProject.findProperty("cacheRedirectorEnabled")?.toString() ?: "false")
        project.kotlinBuildProperties.junit5NumberOfThreadsForParallelExecution?.let { n ->
            systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
            systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", n)
        }
        val excludesFile = project.providers.gradleProperty("teamcity.build.parallelTests.excludesFile").orNull
        if (excludesFile != null && File(excludesFile).exists()) {
            systemProperty("teamcity.build.parallelTests.excludesFile", excludesFile)
        }

        systemProperty("idea.ignore.disabled.plugins", "true")

        var subProjectTempRoot: Path? = null
        val projectName = project.name
        val teamcity = project.rootProject.findProperty("teamcity") as? Map<*, *>
        doFirst {
            if (excludesFile != null && File(excludesFile).exists()) {
                cleanupInvalidExcludePatternsForTCParallelTests(excludesFile) // Workaround for TW-92736
            }

            val systemTempRoot =
            // TC by default doesn't switch `teamcity.build.tempDir` to 'java.io.tmpdir' so it could cause to wasted disk space
                // Should be fixed soon on Teamcity side
                (teamcity?.get("teamcity.build.tempDir") as? String)
                    ?: System.getProperty("java.io.tmpdir")
            systemTempRoot.let {
                val prefix = "${projectName}Project_${taskName}_"
                subProjectTempRoot = Files.createTempDirectory(File(systemTempRoot).toPath(), prefix)
                systemProperty("java.io.tmpdir", subProjectTempRoot.toString())
            }
        }

        val fs = project.serviceOf<FileSystemOperations>()
        doLast {
            subProjectTempRoot?.let {
                try {
                    fs.delete {
                        delete(it)
                    }
                } catch (e: Exception) {
                    logger.warn("Can't delete test temp root folder $it", e.printStackTrace())
                }
            }
        }

        if (parallel && jUnitMode != JUnitMode.JUnit5) {
            val forks = (totalMaxMemoryForTestsMb / memoryPerTestProcessMb).coerceAtMost(16)
            maxParallelForks =
                project.providers.gradleProperty("kotlin.test.maxParallelForks").orNull?.toInt()
                    ?: forks.coerceIn(1, Runtime.getRuntime().availableProcessors())
        }

        if (!kotlinBuildProperties.isTeamcityBuild) {
            defineJDKEnvVariables.forEach { version ->
                val jdkHome = project.getToolchainJdkHomeFor(version).orNull ?: error("Can't find toolchain for $version")
                environment(version.envName, jdkHome)
            }
        }
    }.apply { configure(body) }
}

fun Test.enableJunit5ExtensionsAutodetection() {
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}

val defaultMaxMemoryPerTestWorkerMb = 1600
val reservedMemoryMb = 9000 // system processes, gradle daemon, kotlin daemon, etc ...

val totalMaxMemoryForTestsMb: Int
    get() {
        val mxbean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        return (mxbean.totalPhysicalMemorySize / 1048576 - reservedMemoryMb).toInt()
    }

val Test.commandLineIncludePatterns: Set<String>
    get() = (filter as? DefaultTestFilter)?.commandLineIncludePatterns.orEmpty()

private inline fun String.isFirstChar(f: (Char) -> Boolean) = isNotEmpty() && f(first())

inline fun <reified T : Task> Project.getOrCreateTask(taskName: String, noinline body: T.() -> Unit): TaskProvider<T> =
    if (tasks.names.contains(taskName)) tasks.named(taskName, T::class.java).apply { configure(body) }
    else tasks.register(taskName, T::class.java, body)

fun Project.confugureFirPluginAnnotationsDependency(testTask: TaskProvider<Test>) {
    val firPluginJvmAnnotations: Configuration by configurations.creating
    val firPluginJsAnnotations: Configuration by configurations.creating {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_RUNTIME))
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        }
    }

    dependencies {
        firPluginJvmAnnotations(project(":plugins:plugin-sandbox:plugin-annotations")) { isTransitive = false }
        firPluginJsAnnotations(project(":plugins:plugin-sandbox:plugin-annotations")) { isTransitive = false }
    }

    testTask.configure {
        dependsOn(firPluginJvmAnnotations, firPluginJsAnnotations)
        val localFirPluginJvmAnnotations: FileCollection = firPluginJvmAnnotations
        val localFirPluginJsAnnotations: FileCollection = firPluginJsAnnotations
        doFirst {
            systemProperty("firPluginAnnotations.jvm.path", localFirPluginJvmAnnotations.singleFile.canonicalPath)
            systemProperty("firPluginAnnotations.js.path", localFirPluginJsAnnotations.singleFile.canonicalPath)
        }
    }
}

fun Project.optInTo(annotationFqName: String) {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.optIn.add(annotationFqName)
    }
}

fun Project.optInToExperimentalCompilerApi() {
    optInTo("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
}

fun Project.optInToUnsafeDuringIrConstructionAPI() {
    optInTo("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
}

fun Project.optInToObsoleteDescriptorBasedAPI() {
    optInTo("org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI")
}
