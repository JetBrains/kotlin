/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetType
import org.jetbrains.kotlin.buildtools.api.abi.dumpJvmAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.dumpKlibAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpJvmAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpKlibAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.filters
import org.jetbrains.kotlin.gradle.plugin.abi.internal.AbiValidationPaths.LEGACY_JVM_DUMP_EXTENSION
import org.jetbrains.kotlin.gradle.plugin.abi.internal.AbiValidationPaths.LEGACY_KLIB_DUMP_EXTENSION
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import java.nio.file.Path

@CacheableTask
internal abstract class KotlinAbiDumpTaskImpl : AbiToolsTask(), UsesKotlinToolingDiagnostics {
    @get:OutputDirectory
    abstract val dumpDir: DirectoryProperty

    @get:InputFiles // don't fail the task if the file does not exist https://github.com/gradle/gradle/issues/2016
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val referenceKlibDump: RegularFileProperty

    @get:Input
    abstract val unsupportedTargets: SetProperty<KlibTargetId>

    @get:Input
    abstract val keepLocallyUnsupportedTargets: Property<Boolean>

    @get:Nested
    abstract val jvm: ListProperty<JvmTargetInfo>

    /**
     * A property that contains actual klib targets.
     */
    @get:Nested
    abstract val klib: ListProperty<KlibTargetInfo>

    @get:Input
    @get:Optional
    abstract val includedClasses: SetProperty<String>

    @get:Input
    @get:Optional
    abstract val excludedClasses: SetProperty<String>

    @get:Input
    @get:Optional
    abstract val includedAnnotatedWith: SetProperty<String>

    @get:Input
    @get:Optional
    abstract val excludedAnnotatedWith: SetProperty<String>

    @get:Input
    val projectName: String = project.name


    override fun runTools(abiValidationToolchain: AbiValidationToolchain, buildSession: KotlinToolchains.BuildSession) {
        val abiDir = dumpDir.get().asFile

        val jvmTargets = jvm.get()
        val klibTargets = klib.get()
        val unsupported = unsupportedTargets.get()
        val keepUnsupported = keepLocallyUnsupportedTargets.get()

        val jvmDumpName = projectName + LEGACY_JVM_DUMP_EXTENSION
        val klibDumpName = projectName + LEGACY_KLIB_DUMP_EXTENSION

        if (!keepUnsupported && unsupported.isNotEmpty()) {
            throw IllegalStateException(
                "Validation could not be performed as targets $unsupportedTargets " +
                        "are not supported by host compiler and the 'keepLocallyUnsupportedTargets' mode was disabled."
            )
        }

        abiDir.mkdirs()
        abiDir.deleteDirectoryContents()

        jvmTargets.forEach { jvmTarget ->
            val classfiles = jvmTarget.classfilesDirs.asFileTree
                .asSequence()
                .filter {
                    !it.isDirectory && it.name.endsWith(".class") && !it.name.startsWith("META-INF/")
                }.map { it.toPath() }.asIterable()


            val dirForDump = if (jvmTarget.subdirectoryName == "") {
                abiDir
            } else {
                abiDir.resolve(jvmTarget.subdirectoryName).also { it.mkdirs() }
            }

            val dumpFile = dirForDump.resolve(jvmDumpName)

            dumpFile.bufferedWriter().use { writer ->
                val operation = abiValidationToolchain.dumpJvmAbiToStringOperation(writer, classfiles) {
                    this[DumpJvmAbiToStringOperation.PATTERN_FILTERS] = filters {
                        this[AbiFilters.INCLUDE_NAMED] = includedClasses.getOrElse(emptySet())
                        this[AbiFilters.EXCLUDE_NAMED] = excludedClasses.getOrElse(emptySet())
                        this[AbiFilters.INCLUDE_ANNOTATED_WITH] = includedAnnotatedWith.getOrElse(emptySet())
                        this[AbiFilters.EXCLUDE_ANNOTATED_WITH] = excludedAnnotatedWith.getOrElse(emptySet())
                    }
                }
                buildSession.executeOperation(operation)
            }
        }

        if (klibTargets.isNotEmpty() || unsupported.isNotEmpty()) {
            val klibs: MutableMap<KlibTargetId, Path> = mutableMapOf()
            klibTargets.forEach { suite ->
                val klibDir = suite.klibFiles.files.first()
                if (klibDir.exists()) {
                    klibs[KlibTargetId(KlibTargetType.fromCanonicalName(suite.canonicalName), suite.targetName)] = klibDir.toPath()
                }
            }
            if (unsupported.isNotEmpty()) {
                unsupported.forEach { unsupportedTarget ->
                    reportDiagnostic(
                        KotlinToolingDiagnostics.AbiValidationUnsupportedTarget.invoke(unsupportedTarget.targetType)
                    )
                }
            }
            val referenceFile = referenceKlibDump.get().asFile

            abiDir.resolve(klibDumpName).bufferedWriter().use { writer ->
                val operation = abiValidationToolchain.dumpKlibAbiToStringOperation(writer, klibs) {
                    this[DumpKlibAbiToStringOperation.PATTERN_FILTERS] = filters {
                        this[AbiFilters.INCLUDE_NAMED] = includedClasses.getOrElse(emptySet())
                        this[AbiFilters.EXCLUDE_NAMED] = excludedClasses.getOrElse(emptySet())
                        this[AbiFilters.INCLUDE_ANNOTATED_WITH] = includedAnnotatedWith.getOrElse(emptySet())
                        this[AbiFilters.EXCLUDE_ANNOTATED_WITH] = excludedAnnotatedWith.getOrElse(emptySet())
                    }

                    val targetsToInfer = unsupported.toSet()
                    if (targetsToInfer.isNotEmpty()) {
                        this[DumpKlibAbiToStringOperation.TARGETS_TO_INFER] = targetsToInfer
                        this[DumpKlibAbiToStringOperation.REFERENCE_DUMP_FILE] = referenceFile.toPath()
                    }
                }
                buildSession.executeOperation(operation)
            }
        }
    }

    internal class JvmTargetInfo(
        @get:Input
        val subdirectoryName: String,

        @get:Optional
        @get:Classpath
        val classfilesDirs: FileCollection,
    )

    internal class KlibTargetInfo(
        @get:Input
        val targetName: String,

        @get:Input
        val canonicalName: String,

        @get:InputFiles
        @get:Optional
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val klibFiles: FileCollection,
    )

    companion object {
        const val NAME = "internalDumpKotlinAbi"
    }
}
