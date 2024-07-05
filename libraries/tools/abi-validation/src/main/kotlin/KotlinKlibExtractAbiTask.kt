/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.klib.KlibDump
import kotlinx.validation.api.klib.KlibTarget
import kotlinx.validation.api.klib.saveTo
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Extracts dump for targets supported by the host compiler from a merged API dump stored in a project.
 *
 * If some targets the dump stored in a project directory was generated for are not supported by the host compiler,
 * only supported tasks could be extracted for further validation.
 */
@CacheableTask
public abstract class KotlinKlibExtractAbiTask : DefaultTask() {
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

    private val rootDir = project.rootDir

    @OptIn(ExperimentalBCVApi::class)
    @TaskAction
    internal fun generate() {
        val inputFile = inputAbiFile.asFile.get()
        if (!inputFile.exists()) {
            error("File with project's API declarations '${inputFile.relativeTo(rootDir)}' does not exist.\n" +
                    "Please ensure that ':apiDump' was executed in order to get API dump to compare the build against")
        }
        if (inputFile.length() == 0L) {
            Files.copy(inputFile.toPath(), outputAbiFile.asFile.get().toPath(), StandardCopyOption.REPLACE_EXISTING)
            return
        }
        val dump = KlibDump.from(inputFile)
        val unsupportedTargets = targetsToRemove.get().map(KlibTarget::targetName).toSet()
        // Filter out only unsupported files.
        // That ensures that target renaming will be caught and reported as a change.
        if (unsupportedTargets.isNotEmpty() && strictValidation.get()) {
            throw IllegalStateException(
                "Validation could not be performed as some targets (namely, $targetsToRemove) are not available " +
                        "and the strictValidation mode was enabled."
            )
        }
        dump.remove(unsupportedTargets.map(KlibTarget::parse))
        dump.saveTo(outputAbiFile.asFile.get())
    }
}
