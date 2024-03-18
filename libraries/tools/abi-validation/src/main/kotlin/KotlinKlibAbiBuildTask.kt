/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.klib.KLibDumpFilters
import kotlinx.validation.api.klib.KlibDump
import kotlinx.validation.api.klib.KlibSignatureVersion
import kotlinx.validation.api.klib.saveTo
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.Serializable

internal class SerializableSignatureVersion(val version: Int) : Serializable {
    constructor(version: KlibSignatureVersion) : this(version.version)

    fun toKlibSignatureVersion(): KlibSignatureVersion = KlibSignatureVersion(version)
}

/**
 * Generates a text file with a KLib ABI dump for a single klib.
 */
internal abstract class KotlinKlibAbiBuildTask : BuildTaskBase() {

    /**
     * Path to a klib to dump.
     */
    @InputFiles
    lateinit var klibFile: FileCollection

    /**
     * Bind this task with a klib compilation.
     */
    @InputFiles
    lateinit var compilationDependencies: FileCollection

    /**
     * Refer to [KlibValidationSettings.signatureVersion] for details.
     */
    @Optional
    @get:Input
    var signatureVersion: SerializableSignatureVersion = SerializableSignatureVersion(KlibSignatureVersion.LATEST)

    /**
     * Name of a target [klibFile] was compiled for.
     */
    @Input
    lateinit var target: String

    @OptIn(ExperimentalBCVApi::class)
    @TaskAction
    internal fun generate() {
        outputApiFile.delete()
        outputApiFile.parentFile.mkdirs()

        val dump = KlibDump.fromKlib(klibFile.singleFile, target, KLibDumpFilters {
            ignoredClasses.addAll(this@KotlinKlibAbiBuildTask.ignoredClasses)
            ignoredPackages.addAll(this@KotlinKlibAbiBuildTask.ignoredPackages)
            nonPublicMarkers.addAll(this@KotlinKlibAbiBuildTask.nonPublicMarkers)
            signatureVersion = this@KotlinKlibAbiBuildTask.signatureVersion.toKlibSignatureVersion()
        })

        dump.saveTo(outputApiFile)
    }
}
