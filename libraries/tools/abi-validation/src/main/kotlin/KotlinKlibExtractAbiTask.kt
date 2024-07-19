/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.klib.KlibDump
import kotlinx.validation.api.klib.KlibTarget
import kotlinx.validation.api.klib.saveTo
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject

/**
 * Extracts dump for targets supported by the host compiler from a merged API dump stored in a project.
 *
 * If some targets the dump stored in a project directory was generated for are not supported by the host compiler,
 * only supported tasks could be extracted for further validation.
 */
@CacheableTask
public abstract class KotlinKlibExtractAbiTask : WorkerAwareTaskBase() {
    /**
     * Merged KLib dump that should be filtered by this task.
     */
    @get:InputFiles // don't fail the task if file does not exist, instead print custom error message from generate()
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val inputAbiFile: RegularFileProperty

    /**
     * List of the targets that need to be filtered out from [inputAbiFile].
     */
    @get:Input
    public abstract val targetsToRemove: SetProperty<KlibTarget>

    /**
     * Refer to [KlibValidationSettings.strictValidation] for details.
     */
    @get:Input
    public val strictValidation: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    /**
     * A path to the resulting dump file.
     */
    @get:OutputFile
    public abstract val outputAbiFile: RegularFileProperty

    @get:Inject
    public abstract val executor: WorkerExecutor

    private val rootDir = project.rootDir

    @TaskAction
    internal fun generate() {
        val q = executor.classLoaderIsolation {
            it.classpath.from(runtimeClasspath)
        }
        q.submit(KlibExtractAbiWorker::class.java) { params ->
            params.inputAbiFile.set(inputAbiFile)
            params.targetsToRemove.set(targetsToRemove)
            params.strictValidation.set(strictValidation)
            params.outputAbiFile.set(outputAbiFile)
            params.rootDir.set(rootDir)
        }
        q.await()
    }
}

internal interface KlibExtractAbiParameters : WorkParameters {
    val inputAbiFile: RegularFileProperty
    val targetsToRemove: SetProperty<KlibTarget>
    val strictValidation: Property<Boolean>
    val outputAbiFile: RegularFileProperty
    val rootDir: DirectoryProperty
}

internal abstract class KlibExtractAbiWorker : WorkAction<KlibExtractAbiParameters> {
    @OptIn(ExperimentalBCVApi::class)
    override fun execute() {
        val inputFile = parameters.inputAbiFile.asFile.get()
        val rootDir = parameters.rootDir.asFile.get()
        if (!inputFile.exists()) {
            error("File with project's API declarations '${inputFile.relativeTo(rootDir)}' does not exist.\n" +
                    "Please ensure that ':apiDump' was executed in order to get API dump to compare the build against")
        }
        if (inputFile.length() == 0L) {
            Files.copy(inputFile.toPath(), parameters.outputAbiFile.asFile.get().toPath(), StandardCopyOption.REPLACE_EXISTING)
            return
        }
        val dump = KlibDump.from(inputFile)
        val unsupportedTargets = parameters.targetsToRemove.get().map(KlibTarget::targetName).toSet()
        // Filter out only unsupported files.
        // That ensures that target renaming will be caught and reported as a change.
        if (unsupportedTargets.isNotEmpty() && parameters.strictValidation.get()) {
            throw IllegalStateException(
                "Validation could not be performed as some targets (namely, ${parameters.targetsToRemove}) " +
                        "are not available and the strictValidation mode was enabled."
            )
        }
        dump.remove(unsupportedTargets.map(KlibTarget::parse))
        dump.saveTo(parameters.outputAbiFile.asFile.get())
    }
}
