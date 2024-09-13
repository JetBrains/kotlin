/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.PlatformManagerProvider
import org.jetbrains.kotlin.gradle.plugin.konan.prepareAsOutputDirectory
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject

@CacheableTask
open class KonanJvmInteropTask @Inject constructor(
        objectFactory: ObjectFactory,
        layout: ProjectLayout,
        private val execOperations: ExecOperations
) : DefaultTask() {
    /**
     * Classpath with Interop Stub Generator CLI tool.
     */
    @get:Classpath
    val interopStubGeneratorClasspath: ConfigurableFileCollection = objectFactory.fileCollection()

    /**
     * Native libraries required for Interop Stub Generator CLI tool.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    val interopStubGeneratorNativeLibs: ConfigurableFileCollection = objectFactory.fileCollection()

    /**
     * `.def` file for which to generate bridges.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY) // The name is used for generated package name
    val defFile: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Locations to search for headers.
     *
     * Will be passed to the compiler as `-I…` and will also be used to compute task dependencies: recompile if the headers change.
     */
    // Marked as input via [headers].
    @get:Internal
    val headersDirs: ConfigurableFileCollection = objectFactory.fileCollection()

    /**
     * Computed header files used for task dependencies tracking.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE) // manually computed: [headersPathsRelativeToWorkingDir]
    protected val headers: FileCollection = layout.files(headersDirs.files).asFileTree.matching {
        include("**/*.h")
    }

    @get:Input
    @Suppress("unused")
    protected val headersPathsRelativeToWorkingDir: Provider<List<String>> = headers.elements.map { elements ->
        val base = layout.projectDirectory
        elements.map {
            it.asFile.toRelativeString(base.asFile)
        }
    }

    /**
     * Compiler options for `clang`.
     */
    @get:Input
    val compilerOptions: ListProperty<String> = objectFactory.listProperty()

    /**
     * Generated Kotlin bridges.
     */
    @get:OutputDirectory
    val kotlinBridges: DirectoryProperty = objectFactory.directoryProperty()

    /**
     * Generated C bridge.
     */
    @get:OutputFile
    val cBridge: RegularFileProperty = objectFactory.fileProperty()

    @get:LocalState
    val temporaryFilesDir: DirectoryProperty = objectFactory.directoryProperty()

    @get:Nested
    val platformManagerProvider: Property<PlatformManagerProvider> = objectFactory.property()

    @TaskAction
    fun run() {
        kotlinBridges.get().prepareAsOutputDirectory()

        val compilerArgs = buildList {
            headersDirs.mapTo(this) { "-I${it.absolutePath}" }
            addAll(platformManagerProvider.get().platformManager.hostPlatform.clangForJni.hostCompilerArgsForJni)
            addAll(compilerOptions.get())
        }

        val nativeLibrariesPaths = interopStubGeneratorNativeLibs.files.joinToString(separator = File.pathSeparator) {
            it.parentFile.absolutePath
        }

        execOperations.javaexec {
            classpath(interopStubGeneratorClasspath)
            mainClass.assign("org.jetbrains.kotlin.native.interop.gen.jvm.MainKt")
            jvmArgs("-ea")
            systemProperties(mapOf(
                    "java.library.path" to nativeLibrariesPaths,
                    // Set the konan.home property because we run the cinterop tool not from a distribution jar
                    // so it will not be able to determine this path by itself.
                    "konan.home" to platformManagerProvider.get().nativeProtoDistribution.root.asFile.absolutePath,
            ))
            environment(mapOf("LIBCLANG_DISABLE_CRASH_RECOVERY" to "1"))
            environment["PATH"] = buildList {
                addAll(platformManagerProvider.get().platformManager.hostPlatform.clang.clangPaths)
                add(environment["PATH"])
            }.joinToString(separator = File.pathSeparator)

            args("-generated", kotlinBridges.get().asFile.absolutePath)
            args("-Xtemporary-files-dir", temporaryFilesDir.get().asFile.absolutePath)
            args("-flavor", "jvm")
            args("-def", defFile.get().asFile.absolutePath)
            args("-target", HostManager.Companion.host)
            args(compilerArgs.flatMap { listOf("-compiler-option", it) })
        }.assertNormalExitValue()

        val generatedName = defFile.get().asFile.nameWithoutExtension.split(".").reversed().joinToString(separator = "")
        val originalStubs = temporaryFilesDir.file("${generatedName}stubs.c").get().asFile
        require(originalStubs.exists())

        // interop tool uses precompiled headers, generated .c file does not have required includes. Add them manually.

        val processedHeaders = buildList {
            defFile.get().asFile.useLines {
                val lines = it.dropWhile { !it.startsWith("headers") }.iterator()
                require(lines.hasNext()) { "${defFile.get().asFile} does not have `headers` field" }
                val spacesRegex = "\\s+".toRegex()
                var line = lines.next().replace("^headers\\s*=\\s*".toRegex(), "")
                while (true) {
                    val elements = line.split(spacesRegex).filterNot { it.isEmpty() }
                    if (elements.lastOrNull() != "\\") {
                        addAll(elements)
                        break // if the line did not end on \, stop parsing.
                    }
                    addAll(elements.dropLast(1))
                    if (!lines.hasNext()) {
                        break
                    }
                    line = lines.next()
                }
            }
        }

        cBridge.get().asFile.printWriter().use { writer ->
            (listOf("stdint.h", "string.h", "jni.h") + processedHeaders).forEach {
                writer.appendLine("#include <$it>")
            }
            originalStubs.useLines { lines ->
                lines.forEach {
                    writer.appendLine(it)
                }
            }
        }
    }
}