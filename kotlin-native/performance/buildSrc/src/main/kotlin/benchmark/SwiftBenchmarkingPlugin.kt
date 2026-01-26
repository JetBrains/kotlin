package org.jetbrains.kotlin.benchmark

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.withType
import org.gradle.language.swift.tasks.SwiftCompile
import org.gradle.nativeplatform.tasks.LinkExecutable
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

private const val NATIVE_FRAMEWORK_NAME = "benchmark"

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class SwiftBenchmarkingPlugin : BenchmarkingPlugin() {
    private val Project.swiftLinkTaskProvider: TaskProvider<LinkExecutable>
        get() = project.tasks.named("linkRelease", LinkExecutable::class) // Only interested in the optimized Swift

    private val Project.kotlinLinkTaskProvider: TaskProvider<out KotlinNativeLink>
        get() = hostKotlinNativeTarget.binaries.getFramework(NATIVE_FRAMEWORK_NAME, project.buildType).linkTaskProvider

    override fun Project.createNativeBinary(target: KotlinNativeTarget) {
        target.binaries.framework(NATIVE_FRAMEWORK_NAME, listOf(project.buildType)) {
            export(dependencies.project(":benchmarksLauncher"))
        }
    }

    override fun Project.configureExtraTasks() {
        val framework = kotlinLinkTaskProvider.map { it.outputFile.get() }
        tasks.withType(SwiftCompile::class).configureEach {
            compilerArgs.add("-F")
            compilerArgs.add(framework.map { it.parentFile.absolutePath })
        }
        val debugLinkerFailure by tasks.registering {
            val file = projectDir.resolve("build/tmp/linkRelease/output.txt")
            doFirst {
                if (file.exists()) {
                    logger.error("LINKER FAILURE DETAILS:\n${file.readText()}")
                }
            }
        }
        tasks.withType(LinkExecutable::class).configureEach {
            linkerArgs.add("-Xlinker")
            linkerArgs.add("-rpath")
            linkerArgs.add("-Xlinker")
            linkerArgs.add(framework.map { it.parentFile.absolutePath })
            linkerArgs.add("-F")
            linkerArgs.add(framework.map { it.parentFile.absolutePath })
            linkerArgs.add("-v")
            finalizedBy(debugLinkerFailure)
        }
    }

    override fun RunKotlinNativeTask.configureKonanRunTask() {
        executable.set(project.swiftLinkTaskProvider.map { it.linkedFile.get() })
    }

    override fun JsonReportTask.configureKonanJsonReportTask() {
        codeSizeBinary.fileProvider(project.kotlinLinkTaskProvider.map { it.outputFile.get().resolve(NATIVE_FRAMEWORK_NAME) })
        compilerFlags.addAll(project.kotlinLinkTaskProvider.map { it.toolOptions.freeCompilerArgs.get() })
    }

    override fun apply(target: Project) {
        target.pluginManager.apply("swift-application")
        super.apply(target)
    }
}
