/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cpp.CppHeadersSet
import org.jetbrains.kotlin.cpp.cppHeadersSet
import org.jetbrains.kotlin.gradle.plugin.konan.prepareAsOutput
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.nativeDistribution.NativeDistributionProperty
import org.jetbrains.kotlin.platformManagerProvider
import java.io.File
import javax.inject.Inject
import kotlin.collections.flatMap
import kotlin.collections.joinToString
import kotlin.collections.plus

private abstract class KonanJvmInteropAction @Inject constructor(
        private val execOperations: ExecOperations,
) : WorkAction<KonanJvmInteropAction.Parameters> {
    interface Parameters : WorkParameters {
        val interopStubGeneratorClasspath: ConfigurableFileCollection
        val interopStubGeneratorNativeLibraries: ConfigurableFileCollection
        val defFile: RegularFileProperty
        val compilerOpts: ListProperty<String>
        val outputDirectory: DirectoryProperty
        val distribution: NativeDistributionProperty
        val propertiesOverride: MapProperty<String, String>
        val platformManager: Property<PlatformManager>
    }

    override fun execute() {
        val hostPlatform = parameters.platformManager.get().hostPlatform
        val outputDirectory = parameters.outputDirectory.get()
        execOperations.javaexec {
            classpath(parameters.interopStubGeneratorClasspath)
            mainClass.set("org.jetbrains.kotlin.native.interop.gen.jvm.MainKt")
            jvmArgs("-ea")
            systemProperty("java.library.path", parameters.interopStubGeneratorNativeLibraries.files.joinToString(File.pathSeparator) { it.parentFile.absolutePath })
            systemProperty("konan.home", parameters.distribution.get().root.asFile.absolutePath)
            environment("LIBCLANG_DISABLE_CRASH_RECOVERY", "1")
            environment("PATH", (hostPlatform.clang.clangPaths + environment["PATH"]).joinToString(File.pathSeparator))
            args("-generated", outputDirectory.dir("kotlin").asFile.absolutePath)
            args("-Xtemporary-files-dir", outputDirectory.dir("c").asFile.absolutePath)
            args("-flavor", "jvm")
            args("-def", parameters.defFile.get().asFile.absolutePath)
            args("-target", hostPlatform.target.name)
            args("-Xoverride-konan-properties", parameters.propertiesOverride.get().entries.joinToString(separator = ";") { (key, value) ->
                "$key=$value"
            })
            args(parameters.compilerOpts.get().flatMap { listOf("-compiler-option", it) })
        }
    }
}

/**
 * A task to generate bindings for [defFile] using JVM-flavored cinterop.
 * The output will be placed in [outputDirectory].
 */
@CacheableTask
open class KonanJvmInteropTask @Inject constructor(
        objectFactory: ObjectFactory,
        layout: ProjectLayout,
        private val workerExecutor: WorkerExecutor,
) : DefaultTask() {
    /**
     * Classpath for the interop StubGenerator tool.
     */
    @get:Classpath
    val interopStubGeneratorClasspath: ConfigurableFileCollection = objectFactory.fileCollection()

    /**
     * Native libraries required for the interop StubGenerator tool.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY) // The bindings call native libraries by name
    val interopStubGeneratorNativeLibraries: ConfigurableFileCollection = objectFactory.fileCollection()

    /**
     * .def file for which to generate bindings.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY) // Package name is derived from .def file name
    val defFile: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Locations to search for headers.
     *
     * Will be passed to the compiler as `-I…` and will also be used to compute task dependencies: recompile if the headers change.
     */
    @get:Nested
    val headersDirs: CppHeadersSet = objectFactory.cppHeadersSet().apply {
        workingDir.set(layout.projectDirectory)
    }

    /**
     * Additional compiler options.
     */
    @get:Input
    val compilerOpts: ListProperty<String> = objectFactory.listProperty(String::class.java)

    /**
     * Output directory for bindings.
     */
    @get:OutputDirectory
    val outputDirectory: DirectoryProperty = objectFactory.directoryProperty()

    @get:Nested
    protected val platformManagerProvider = objectFactory.platformManagerProvider(project)

    @TaskAction
    fun run() {
        outputDirectory.get().asFile.prepareAsOutput()

        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(KonanJvmInteropAction::class.java) {
            this.interopStubGeneratorClasspath.from(this@KonanJvmInteropTask.interopStubGeneratorClasspath)
            this.interopStubGeneratorNativeLibraries.from(this@KonanJvmInteropTask.interopStubGeneratorNativeLibraries)
            this.defFile.set(this@KonanJvmInteropTask.defFile)
            this.compilerOpts.set(this@KonanJvmInteropTask.compilerOpts)
            this.compilerOpts.addAll(this@KonanJvmInteropTask.headersDirs.asCompilerArguments)
            this.outputDirectory.set(this@KonanJvmInteropTask.outputDirectory)
            this.distribution.set(platformManagerProvider.distribution)
            this.propertiesOverride.set(platformManagerProvider.konanPropertiesOverride)
            this.platformManager.set(platformManagerProvider.platformManager)
        }
    }
}