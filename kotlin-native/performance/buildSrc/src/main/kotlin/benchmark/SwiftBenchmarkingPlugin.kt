package org.jetbrains.kotlin.benchmark

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTask
import javax.inject.Inject

private const val NATIVE_FRAMEWORK_NAME = "benchmark"
private const val EXTENSION_NAME = "swiftBenchmark"

open class SwiftBenchmarkExtension @Inject constructor(project: Project) : BenchmarkExtension(project) {
    /**
     * The minimum Swift compiler requirement.
     *
     * See [the official documentation](https://docs.swift.org/swiftpm/documentation/packagemanagerdocs/settingswifttoolsversion)
     * for details.
     */
    val swiftToolsVersion: Property<String> = project.objects.property(String::class.java)

    /**
     * Directory where to place `Package.swift` for the Kotlin-generated XCFramework
     */
    val packageDirectory: DirectoryProperty = project.objects.directoryProperty()

    val generateSwiftPackage by project.tasks.registering(GenerateSwiftPackageTask::class)
    val buildSwift by project.tasks.registering(SwiftBuildTask::class)
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class SwiftBenchmarkingPlugin : BenchmarkingPlugin() {
    override val Project.benchmark: SwiftBenchmarkExtension
        get() = extensions.getByName(EXTENSION_NAME) as SwiftBenchmarkExtension

    override fun Project.createExtension() = extensions.create<SwiftBenchmarkExtension>(EXTENSION_NAME, this)

    override fun Project.configureTasks() {
        kotlin.apply {
            targets.withType(KotlinNativeTarget::class).configureEach {
                val xcf = XCFramework(NATIVE_FRAMEWORK_NAME)
                binaries.framework(NATIVE_FRAMEWORK_NAME, listOf(project.buildType)) {
                    isStatic = true
                    export(dependencies.project(":benchmarksLauncher"))
                    xcf.add(this)
                }
            }
        }
        benchmark.generateSwiftPackage.configure {
            swiftToolsVersion.set(benchmark.swiftToolsVersion)
            packageDirectory.set(benchmark.packageDirectory)
            val xcFrameworkTask = tasks.named("assemble${NATIVE_FRAMEWORK_NAME.capitalized}${project.buildType.getName().capitalized}XCFramework", XCFrameworkTask::class)
            xcFramework.fileProvider(xcFrameworkTask.map { it.outputs.files.singleFile })
        }
        benchmark.buildSwift.configure {
            inputs.files(benchmark.generateSwiftPackage.map { it.outputs.files }) // Package.swift depends on Kotlin framework
            product.set(benchmark.applicationName)
            outputDirectory.set(layout.buildDirectory)
            scratchPath.set(layout.buildDirectory.dir("swiftbuild"))
            options.addAll("-c", "release") // We are only interested in the optimized Swift.
        }
        benchmark.konanRun.configure {
            executable.set(project.benchmark.buildSwift.map { it.outputFile.get() })
        }
        val linkTaskProvider = kotlin.hostTarget().binaries.getFramework(NATIVE_FRAMEWORK_NAME, project.buildType).linkTaskProvider
        benchmark.getCodeSize.configure {
            codeSizeBinary.fileProvider(linkTaskProvider.map { it.outputFile.get().resolve(NATIVE_FRAMEWORK_NAME) })
        }
        benchmark.konanJsonReport.configure {
            compilerFlags.addAll(linkTaskProvider.map { it.toolOptions.freeCompilerArgs.get() })
        }
    }
}
