/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.interop

import bootstrapKotlinVersion
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.dependencies.NativeDependenciesExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.nativeDistribution.nativeProtoDistribution
import org.jetbrains.kotlin.utils.capitalized
import java.io.File

class NamedNativeInteropConfig(
        private val project: Project,
        private val _name: String,
) : Named {
    override fun getName(): String = _name

    init {
        require(project.plugins.hasPlugin("kotlin"))
    }

    private val interopStubsName = name + "InteropStubs"
    private val interopStubs: SourceSet = project.extensions.getByType<JavaPluginExtension>().sourceSets.create(interopStubsName)

    val genTask = project.tasks.register<JavaExec>("gen" + interopStubsName.capitalized)

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

    private var headers = emptyList<String>()

    fun headers(files: List<String>) {
        headers = headers + files
    }

    val configuration: Configuration = project.configurations.create(interopStubs.name)

    init {
        project.tasks.getByName<KotlinCompile>(interopStubs.getTaskName("compile", "Kotlin")) {
            compilerOptions.freeCompilerArgs.add("-Xskip-prerelease-check")
        }

        (interopStubs.extensions.getByName("kotlin") as SourceDirectorySet).srcDir(project.tasks.named(genTask.name).map { generatedSrcDir })

        project.dependencies {
            (interopStubs.apiConfigurationName)(project(":kotlin-native:Interop:Runtime"))
            (interopStubs.apiConfigurationName)("org.jetbrains.kotlin:kotlin-stdlib:${project.bootstrapKotlinVersion}")
        }

        configuration.extendsFrom(project.configurations.getByName(interopStubs.runtimeClasspathConfigurationName))
        project.dependencies.add(this.configuration.name, interopStubs.output)

        genTask.configure {
            notCompatibleWithConfigurationCache("This task uses Task.project at execution time")
            dependsOn(project.extensions.getByType<NativeDependenciesExtension>().hostPlatformDependency)
            dependsOn(project.extensions.getByType<NativeDependenciesExtension>().llvmDependency)
            dependsOn(":kotlin-native:libclangInterop:nativelibs")
            dependsOn(":kotlin-native:Interop:Runtime:nativelibs")
            classpath = project.configurations.getByName(NativeInteropPlugin.INTEROP_STUB_GENERATOR_CONFIGURATION)
            mainClass = "org.jetbrains.kotlin.native.interop.gen.jvm.MainKt"
            jvmArgs("-ea")
            systemProperties(mapOf(
                    "java.library.path" to project.files(
                            project.project(":kotlin-native:libclangInterop").layout.buildDirectory.dir("nativelibs"),
                            project.project(":kotlin-native:Interop:Runtime").layout.buildDirectory.dir("nativelibs"),
                    ).asPath,
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
                requireNotNull(defFile)
                args("-def", project.file(defFile!!))
                args("-target", HostManager.host)

                // TODO: the interop plugin should probably be reworked to execute clang from build scripts directly
                environment["PATH"] = project.files(project.extensions.getByType<NativeDependenciesExtension>().hostPlatform.clang.clangPaths).asPath + File.pathSeparator + environment["PATH"]

                args(compilerOpts.flatMap { listOf("-compiler-option", it) })

                headers.forEach {
                    args("-header", it)
                }
            }

            doLast {
                // interop tool uses precompiled headers, generated .c file does not have required includes. Add them manually.
                val generatedName = defFile!!.split(".").reversed().drop(1).joinToString(separator = "")
                val originalStubs = temporaryFilesDir.resolve("${generatedName}stubs_original.c")
                val modifiedStubs = temporaryFilesDir.resolve("${generatedName}stubs.c")
                modifiedStubs.copyTo(originalStubs, overwrite = true)
                modifiedStubs.printWriter().use { writer ->
                    (listOf("stdint.h", "string.h", "jni.h") + headers).forEach {
                        writer.appendLine("#include <$it>")
                    }
                    originalStubs.useLines { lines ->
                        lines.forEach {
                            writer.appendLine(it)
                        }
                    }
                }
                originalStubs.delete()
            }
        }
    }

    fun dependsOn(vararg deps: Any) {
        // TODO: add all files to inputs
        genTask.configure {
            dependsOn(deps)
        }
    }

    private val generatedSrcDir: File
        get() = project.layout.buildDirectory.dir("nativeInteropStubs/$name/kotlin").get().asFile

    private val temporaryFilesDir: File
        get() = project.layout.buildDirectory.dir("interopTemp").get().asFile
}