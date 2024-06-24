/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.klib.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Generates a text file with a KLib ABI dump for a single klib.
 */
@CacheableTask
public abstract class KotlinKlibAbiBuildTask : BuildTaskBase() {

    /**
     * Collection consisting of a single path to a compiled klib (either file, or directory).
     *
     * By the end of the compilation process, there might be no klib file emitted,
     * for example, when there are no sources in a project in general,
     * or for a target in particular.
     * The lack of a compiled klib file is not considered as an error, and
     * instead causes the ask being skipped.
     */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val klibFile: ConfigurableFileCollection

    /**
     * Refer to [KlibValidationSettings.signatureVersion] for details.
     */
    @get:Input
    public val signatureVersion: Property<KlibSignatureVersion> =
        project.objects.property(KlibSignatureVersion::class.java)
            .convention(KlibSignatureVersion.LATEST)

    /**
     * A target [klibFile] was compiled for.
     */
    @get:Input
    public abstract val target: Property<KlibTarget>

    /**
     * A path to the resulting dump file.
     */
    @get:OutputFile
    public abstract val outputAbiFile: RegularFileProperty

    @OptIn(ExperimentalBCVApi::class)
    @TaskAction
    internal fun generate() {
        val outputFile = outputAbiFile.asFile.get()
        outputFile.delete()
        outputFile.parentFile.mkdirs()

        val dump = KlibDump.fromKlib(klibFile.singleFile, target.get().configurableName, KLibDumpFilters {
            ignoredClasses.addAll(this@KotlinKlibAbiBuildTask.ignoredClasses.get())
            ignoredPackages.addAll(this@KotlinKlibAbiBuildTask.ignoredPackages.get())
            nonPublicMarkers.addAll(this@KotlinKlibAbiBuildTask.nonPublicMarkers.get())
            signatureVersion = this@KotlinKlibAbiBuildTask.signatureVersion.get()
        })

        dump.saveTo(outputFile)
    }
}
