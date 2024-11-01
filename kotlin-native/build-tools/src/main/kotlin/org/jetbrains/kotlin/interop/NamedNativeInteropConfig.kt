/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.interop

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.*
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.dependencies.NativeDependenciesExtension
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.Platform
import org.jetbrains.kotlin.nativeDistribution.nativeProtoDistribution
import org.jetbrains.kotlin.utils.capitalized
import java.io.File
import javax.inject.Inject

private open class StubGeneratorArgumentProvider @Inject constructor(
        objectFactory: ObjectFactory,
) : CommandLineArgumentProvider {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    val nativeLibraries: ConfigurableFileCollection = objectFactory.fileCollection()

    override fun asArguments(): Iterable<String> = listOf(
            "-Djava.library.path=${nativeLibraries.files.joinToString(File.pathSeparator) { it.parentFile.absolutePath }}"
    )
}

class NamedNativeInteropConfig(
        private val project: Project,
        private val _name: String,
) : Named {
    override fun getName(): String = _name

    val genTask = project.tasks.register<JavaExec>("gen${_name.capitalized}InteropStubs")

    private var defFile: String? = null

    fun defFile(value: String) {
        defFile = value
        genTask.configure {
            inputs.file(project.file(value))
        }
    }

    private val compilerOpts = mutableListOf<String>()

    fun compilerOpts(values: List<String>) {
        compilerOpts.addAll(values)
    }

    init {
        genTask.configure {
            notCompatibleWithConfigurationCache("This task uses Task.project at execution time")
            dependsOn(project.extensions.getByType<NativeDependenciesExtension>().hostPlatformDependency)
            dependsOn(project.extensions.getByType<NativeDependenciesExtension>().llvmDependency)
            classpath = project.configurations.getByName(NativeInteropPlugin.INTEROP_STUB_GENERATOR_CONFIGURATION)
            mainClass = "org.jetbrains.kotlin.native.interop.gen.jvm.MainKt"
            jvmArgs("-ea")
            jvmArgumentProviders.add(project.objects.newInstance<StubGeneratorArgumentProvider>().apply {
                nativeLibraries.from(project.configurations.getByName(NativeInteropPlugin.INTEROP_STUB_GENERATOR_CPP_RUNTIME_CONFIGURATION))
            })
            systemProperties(mapOf(
                    // Set the konan.home property because we run the cinterop tool not from a distribution jar
                    // so it will not be able to determine this path by itself.
                    "konan.home" to project.nativeProtoDistribution.root.asFile.absolutePath,
            ))
            environment(mapOf("LIBCLANG_DISABLE_CRASH_RECOVERY" to "1"))

            outputs.dir(generatedSrcDir)
            outputs.dir(temporaryFilesDir)

            // defer as much as possible
            doFirst {
                args("-generated", generatedSrcDir)
                args("-Xtemporary-files-dir", temporaryFilesDir)
                args("-flavor", "jvm")

                if (defFile != null) {
                    args("-def", project.file(defFile!!))
                }

                args("-target", HostManager.host)

                // TODO: the interop plugin should probably be reworked to execute clang from build scripts directly
                environment["PATH"] = project.files(hostPlatform.clang.clangPaths).asPath + File.pathSeparator + environment["PATH"]

                args(compilerOpts.flatMap { listOf("-compiler-option", it) })
            }
        }
    }

    private val generatedSrcDir: File
        get() = project.layout.buildDirectory.dir("nativeInteropStubs/$name/kotlin").get().asFile

    private val temporaryFilesDir: File
        get() = project.layout.buildDirectory.dir("nativeInteropStubs/$name/c").get().asFile

    val hostPlatform: Platform
        get() = project.extensions.getByType<NativeDependenciesExtension>().hostPlatform
}