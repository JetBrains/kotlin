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
import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.api.AbiToolsInterface
import org.jetbrains.kotlin.abi.tools.api.v2.KlibTarget
import org.jetbrains.kotlin.gradle.plugin.abi.AbiValidationPaths.LEGACY_JVM_DUMP_EXTENSION
import org.jetbrains.kotlin.gradle.plugin.abi.AbiValidationPaths.LEGACY_KLIB_DUMP_EXTENSION
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.incremental.deleteDirectoryContents

@CacheableTask
internal abstract class KotlinLegacyAbiDumpTaskImpl : AbiToolsTask(), KotlinLegacyAbiDumpTask, UsesKotlinToolingDiagnostics {
    @get:OutputDirectory
    abstract override val dumpDir: DirectoryProperty

    @get:InputFiles // don't fail the task if file does not exist https://github.com/gradle/gradle/issues/2016
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val referenceKlibDump: RegularFileProperty

    @get:Input
    abstract val unsupportedTargets: SetProperty<KlibTarget>

    @get:Input
    abstract val klibIsEnabled: Property<Boolean>

    @get:Input
    abstract val keepUnsupportedTargets: Property<Boolean>

    @get:Nested
    abstract val jvm: ListProperty<JvmTargetInfo>

    /**
     * An internal property to disable adding a dependency on klib build tasks if [klibIsEnabled] = false.
     */
    @get:Internal
    abstract val klibInput: ListProperty<KlibTargetInfo>

    /**
     * A property that contains actual klib targets filtered by [klibIsEnabled].
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
    abstract val variantName: Property<String>

    @get:Input
    val projectName: String = project.name


    override fun runTools(tools: AbiToolsInterface) {
        val abiDir = dumpDir.get().asFile

        val jvmTargets = jvm.get()
        val klibTargets = klib.get()
        val unsupported = unsupportedTargets.get()
        val keepUnsupported = keepUnsupportedTargets.get()

        val jvmDumpName = projectName + LEGACY_JVM_DUMP_EXTENSION
        val klibDumpName = projectName + LEGACY_KLIB_DUMP_EXTENSION

        if (!keepUnsupported && unsupported.isNotEmpty()) {
            throw IllegalStateException(
                "Validation could not be performed as targets $unsupportedTargets " +
                        "are not supported by host compiler and the 'keepUnsupported' mode was disabled."
            )
        }

        val filters = AbiFilters(
            includedClasses.getOrElse(emptySet()),
            excludedClasses.getOrElse(emptySet()),
            includedAnnotatedWith.getOrElse(emptySet()),
            excludedAnnotatedWith.getOrElse(emptySet())
        )

        abiDir.mkdirs()
        abiDir.deleteDirectoryContents()

        jvmTargets.forEach { jvmTarget ->
            val classfiles = jvmTarget.classfilesDirs.asFileTree
                .asSequence()
                .filter {
                    !it.isDirectory && it.name.endsWith(".class") && !it.name.startsWith("META-INF/")
                }.asIterable()


            val dirForDump = if (jvmTarget.subdirectoryName == "") {
                abiDir
            } else {
                abiDir.resolve(jvmTarget.subdirectoryName).also { it.mkdirs() }
            }

            val dumpFile = dirForDump.resolve(jvmDumpName)

            dumpFile.bufferedWriter().use { writer ->
                tools.v2.printJvmDump(writer, classfiles, filters)
            }
        }

        if (klibIsEnabled.get() && (klibTargets.isNotEmpty() || unsupported.isNotEmpty())) {
            val mergedDump = tools.v2.createKlibDump()
            klibTargets.forEach { suite ->
                val klibDir = suite.klibFiles.files.first()
                if (klibDir.exists()) {
                    val dump = tools.v2.extractKlibAbi(klibDir, KlibTarget(suite.canonicalTargetName, suite.targetName), filters)
                    mergedDump.merge(dump)
                }
            }

            val referenceFile = referenceKlibDump.get().asFile
            if (unsupported.isNotEmpty()) {
                val referenceDump = if (referenceFile.exists() && referenceFile.isFile) {
                    tools.v2.loadKlibDump(referenceFile)
                } else {
                    tools.v2.createKlibDump()
                }

                unsupported.map { unsupportedTarget ->
                    reportDiagnostic(
                        KotlinToolingDiagnostics.AbiValidationUnsupportedTarget.invoke(unsupportedTarget.targetName)
                    )
                    mergedDump.inferAbiForUnsupportedTarget(referenceDump, unsupportedTarget)
                }.forEach { inferredDump ->
                    mergedDump.merge(inferredDump)
                }
            }

            mergedDump.print(abiDir.resolve(klibDumpName))
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
        val canonicalTargetName: String,

        @get:InputFiles
        @get:Optional
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val klibFiles: FileCollection,
    )

    companion object {
        fun nameForVariant(variantName: String): String {
            return composeTaskName("dumpLegacyAbi", variantName)
        }
    }
}
