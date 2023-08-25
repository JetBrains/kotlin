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
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import java.io.File
import java.lang.Character.isLowerCase
import java.lang.Character.isUpperCase
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path

val kotlinGradlePluginAndItsRequired = arrayOf(
    ":kotlin-assignment",
    ":kotlin-allopen",
    ":kotlin-noarg",
    ":kotlin-sam-with-receiver",
    ":kotlin-lombok",
    ":kotlin-serialization",
    ":kotlin-android-extensions",
    ":kotlin-android-extensions-runtime",
    ":kotlin-parcelize-compiler",
    ":kotlin-build-common",
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
    ":kotlin-sam-with-receiver-compiler-plugin.embeddable",
    ":kotlin-lombok-compiler-plugin.embeddable",
    ":kotlinx-serialization-compiler-plugin.embeddable",
    ":kotlin-annotation-processing-embeddable",
    ":kotlin-script-runtime",
    ":kotlin-scripting-common",
    ":kotlin-scripting-jvm",
    ":kotlin-scripting-compiler-embeddable",
    ":kotlin-scripting-compiler-impl-embeddable",
    ":kotlin-test-js-runner",
    ":native:kotlin-klib-commonizer-embeddable",
    ":native:kotlin-klib-commonizer-api",
    ":prepare:kotlin-gradle-plugin-compiler-dependencies",
    ":compiler:build-tools:kotlin-build-statistics",
    ":compiler:build-tools:kotlin-build-tools-api",
    ":compiler:build-tools:kotlin-build-tools-impl",
)

fun Task.dependsOnKotlinGradlePluginInstall() {
    kotlinGradlePluginAndItsRequired.forEach {
        dependsOn("${it}:install")
    }
}

fun Task.dependsOnKotlinGradlePluginPublish() {
    kotlinGradlePluginAndItsRequired.forEach {
        project.rootProject.tasks.findByPath("${it}:publish")?.let { task ->
            dependsOn(task)
        }
    }
}

// Mixing JUnit4 and Junit5 in one module proved to be problematic, consider using separate modules instead
enum class JUnitMode {
    JUnit4, JUnit5
}

/**
 * @param parallel is redundant if @param jUnit5Enabled is true, because
 *   JUnit5 supports parallel test execution by itself, without gradle help
 */
fun Project.projectTest(
    taskName: String = "test",
    parallel: Boolean = false,
    shortenTempRootName: Boolean = false,
    jUnitMode: JUnitMode = JUnitMode.JUnit4,
    maxHeapSizeMb: Int? = null,
    minHeapSizeMb: Int? = null,
    reservedCodeCacheSizeMb: Int = 256,
    defineJDKEnvVariables: List<JdkMajorVersion> = emptyList(),
    body: Test.() -> Unit = {}
): TaskProvider<Test> {
    val shouldInstrument = project.providers.gradleProperty("kotlin.test.instrumentation.disable")
        .orNull?.toBoolean() != true
    if (shouldInstrument) {
        evaluationDependsOn(":test-instrumenter")
    }
    return getOrCreateTask<Test>(taskName) {
        dependsOn(":createIdeaHomeForTests")

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
            val testInstrumenterOutputs = project.tasks.findByPath(":test-instrumenter:jar")!!.outputs.files
            doFirst {
                val agent = testInstrumenterOutputs.singleFile
                val args = instrumentationArgsProperty.orNull?.let { "=$it" }.orEmpty()
                jvmArgs("-javaagent:$agent$args")
            }
            dependsOn(":test-instrumenter:jar")
        }

        jvmArgs(
            "-ea",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-XX:+UseCodeCacheFlushing",
            "-XX:ReservedCodeCacheSize=${reservedCodeCacheSizeMb}m",
            "-Djna.nosys=true"
        )

        val junit5ParallelTestWorkers =
            project.kotlinBuildProperties.junit5NumberOfThreadsForParallelExecution ?: Runtime.getRuntime().availableProcessors()

        val memoryPerTestProcessMb = maxHeapSizeMb ?: if (jUnitMode == JUnitMode.JUnit5)
            totalMaxMemoryForTestsMb.coerceIn(defaultMaxMemoryPerTestWorkerMb, defaultMaxMemoryPerTestWorkerMb * junit5ParallelTestWorkers)
        else
            defaultMaxMemoryPerTestWorkerMb

        maxHeapSize = "${memoryPerTestProcessMb}m"

        if (minHeapSizeMb != null) {
            minHeapSize = "${minHeapSizeMb}m"
        }

        systemProperty("idea.is.unit.test", "true")
        systemProperty("idea.home.path", project.ideaHomePathForTests().canonicalPath)
        systemProperty("idea.use.native.fs.for.win", false)
        systemProperty("java.awt.headless", "true")
        environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
        environment("PROJECT_CLASSES_DIRS", project.testSourceSet.output.classesDirs.asPath)
        environment("PROJECT_BUILD_DIR", project.buildDir)
        systemProperty("jps.kotlin.home", project.rootProject.extra["distKotlinHomeDir"]!!)
        systemProperty("org.jetbrains.kotlin.skip.muted.tests", if (project.rootProject.hasProperty("skipMutedTests")) "true" else "false")
        systemProperty("cacheRedirectorEnabled", project.rootProject.findProperty("cacheRedirectorEnabled")?.toString() ?: "false")
        project.kotlinBuildProperties.junit5NumberOfThreadsForParallelExecution?.let { n ->
            systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
            systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", n)
        }

        systemProperty("idea.ignore.disabled.plugins", "true")

        var subProjectTempRoot: Path? = null
        val projectName = project.name
        val teamcity = project.rootProject.findProperty("teamcity") as? Map<*, *>
        doFirst {
            val systemTempRoot =
                // TC by default doesn't switch `teamcity.build.tempDir` to 'java.io.tmpdir' so it could cause to wasted disk space
                // Should be fixed soon on Teamcity side
                (teamcity?.get("teamcity.build.tempDir") as? String)
                    ?: System.getProperty("java.io.tmpdir")
            systemTempRoot.let {
                val prefix = (projectName + "Project_" + taskName + "_").takeUnless { shortenTempRootName }
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

        defineJDKEnvVariables.forEach { version ->
            val jdkHome = project.getToolchainJdkHomeFor(version).orNull ?: error("Can't find toolchain for $version")
            environment(version.envName, jdkHome)
        }
    }.apply { configure(body) }
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

object TaskUtils {
    fun useAndroidSdk(task: Task) {
        task.useAndroidConfiguration(systemPropertyName = "android.sdk", configName = "androidSdk")
    }

    fun useAndroidJar(task: Task) {
        task.useAndroidConfiguration(systemPropertyName = "android.jar", configName = "androidJar")
    }

    fun useAndroidEmulator(task: Task) {
        task.useAndroidConfiguration(systemPropertyName = "android.sdk", configName = "androidEmulator")
    }
}

private fun Task.useAndroidConfiguration(systemPropertyName: String, configName: String) {
    val configuration = with(project) {
        configurations.getOrCreate(configName)
            .also {
                if (it.allDependencies.matching { dep ->
                        dep is ProjectDependency &&
                                dep.targetConfiguration == configName &&
                                dep.dependencyProject.path == ":dependencies:android-sdk"
                    }.count() == 0) {
                    dependencies.add(
                        configName,
                        dependencies.project(":dependencies:android-sdk", configuration = configName)
                    )
                }
            }
    }

    dependsOn(configuration)

    if (this is Test) {
        val androidFilePath = configuration.singleFile.canonicalPath
        doFirst {
            systemProperty(systemPropertyName, androidFilePath)
        }
    }
}

fun Task.useAndroidSdk() {
    TaskUtils.useAndroidSdk(this)
}

fun Task.useAndroidJar() {
    TaskUtils.useAndroidJar(this)
}

fun Task.acceptAndroidSdkLicenses() {
    val separator = System.lineSeparator()
    with(project) {
        val androidSdk = configurations["androidSdk"].singleFile
        val sdkLicensesDir = androidSdk.resolve("licenses").also {
            if (!it.exists()) it.mkdirs()
        }

        val sdkLicenses = listOf(
            "8933bad161af4178b1185d1a37fbf41ea5269c55",
            "d56f5187479451eabf01fb78af6dfcb131a6481e",
            "24333f8a63b6825ea9c5514f83c2829b004d1fee",
        )
        val sdkPreviewLicense = "84831b9409646a918e30573bab4c9c91346d8abd"

        val sdkLicenseFile = sdkLicensesDir.resolve("android-sdk-license")
        if (!sdkLicenseFile.exists()) {
            sdkLicenseFile.createNewFile()
            sdkLicenseFile.writeText(
                sdkLicenses.joinToString(separator = separator)
            )
        } else {
            sdkLicenses
                .subtract(
                    sdkLicenseFile.readText().lines()
                )
                .forEach {
                    sdkLicenseFile.appendText("$it$separator")
                }
        }

        val sdkPreviewLicenseFile = sdkLicensesDir.resolve("android-sdk-preview-license")
        if (!sdkPreviewLicenseFile.exists()) {
            sdkPreviewLicenseFile.writeText(sdkPreviewLicense)
        } else {
            if (sdkPreviewLicense != sdkPreviewLicenseFile.readText().trim()) {
                sdkPreviewLicenseFile.writeText(sdkPreviewLicense)
            }
        }
    }
}

fun Project.confugureFirPluginAnnotationsDependency(testTask: TaskProvider<Test>) {
    val firPluginJvmAnnotations: Configuration by configurations.creating
    val firPluginJsAnnotations: Configuration by configurations.creating {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_RUNTIME))
            attribute(org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.attribute, org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.js)
        }
    }

    dependencies {
        firPluginJvmAnnotations(project(":plugins:fir-plugin-prototype:plugin-annotations")) { isTransitive = false }
        firPluginJsAnnotations(project(":plugins:fir-plugin-prototype:plugin-annotations")) { isTransitive = false }
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

private fun Project.optInTo(annotationFqName: String) {
    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=$annotationFqName"
        }
    }
}

fun Project.optInToExperimentalCompilerApi() {
    optInTo("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
}

fun Project.optInToIrSymbolInternals() {
    optInTo("org.jetbrains.kotlin.ir.symbols.IrSymbolInternals")
}

fun Project.optInToObsoleteDescriptorBasedAPI() {
    optInTo("org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI")
}
