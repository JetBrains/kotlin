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
import org.gradle.workers.WorkAction

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

    @TaskAction
    internal fun generate() {
        val workQueue = executor.classLoaderIsolation {
            it.classpath.from(runtimeClasspath)
        }
        workQueue.submit(KlibAbiBuildWorker::class.java) { params ->
            fillCommonParams(params)

            params.klibFile.from(klibFile)
            params.target.set(target)
            params.signatureVersion.set(signatureVersion)
            params.outputAbiFile.set(outputAbiFile)
        }
    }
}

internal interface KlibAbiBuildParameters : BuildParametersBase {
    val klibFile: ConfigurableFileCollection
    val signatureVersion: Property<KlibSignatureVersion>
    val target: Property<KlibTarget>
    val outputAbiFile: RegularFileProperty
}

internal abstract class KlibAbiBuildWorker : WorkAction<KlibAbiBuildParameters> {
    @OptIn(ExperimentalBCVApi::class)
    override fun execute() {
        val outputFile = parameters.outputAbiFile.asFile.get()
        outputFile.delete()
        outputFile.parentFile.mkdirs()

        val dump = KlibDump.fromKlib(parameters.klibFile.singleFile, parameters.target.get().configurableName,
            KlibDumpFilters {
                ignoredClasses.addAll(parameters.ignoredClasses.get())
                ignoredPackages.addAll(parameters.ignoredPackages.get())
                nonPublicMarkers.addAll(parameters.nonPublicMarkers.get())
                signatureVersion = parameters.signatureVersion.get()
            })

        dump.saveTo(outputFile)
    }
}
