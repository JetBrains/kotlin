/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.klib.*
import kotlinx.validation.api.klib.TargetHierarchy
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File

/**
 * Task infers a possible KLib ABI dump for an unsupported target.
 * To infer a dump, tasks walk up the default targets hierarchy tree starting from the unsupported
 * target until it finds a node corresponding to a group of targets having at least one supported target.
 * After that, dumps generated for such supported targets are merged and declarations that are common to all
 * of them are considered as a common ABI that most likely will be shared by the unsupported target.
 * At the next step, if a project contains an old dump, declarations specific to the unsupported target are copied
 * from it and merged into the common ABI extracted previously.
 * The resulting dump is then used as an inferred dump for the unsupported target.
 */
internal abstract class KotlinKlibInferAbiForUnsupportedTargetTask : DefaultTask() {
    @get:Internal
    internal val projectName = project.name

    /**
     * The name of a target to infer a dump for.
     */
    @Input
    lateinit var unsupportedTargetName: String

    /**
     * The name of a target to infer a dump for.
     */
    @Input
    lateinit var unsupportedTargetCanonicalName: String

    /**
     * A root directory containing dumps successfully generated for each supported target.
     * It is assumed that this directory contains subdirectories named after targets.
     */
    @InputFiles
    lateinit var outputApiDir: String

    /**
     * Set of all supported targets.
     */
    @Input
    lateinit var supportedTargets: Provider<Set<String>>

    /**
     * Previously generated merged ABI dump file, the golden image every dump should be verified against.
     */
    @InputFiles
    lateinit var inputImageFile: File

    /**
     * The name of a dump file.
     */
    @Input
    lateinit var dumpFileName: String

    /**
     * A path to an inferred dump file.
     */
    @OutputFile
    lateinit var outputFile: File

    @OptIn(ExperimentalBCVApi::class)
    @TaskAction
    internal fun generate() {
        val unsupportedTarget = KlibTarget(unsupportedTargetCanonicalName, unsupportedTargetName)
        val supportedTargetNames = supportedTargets.get().map { KlibTarget.parse(it) }.toSet()
        // Find a set of supported targets that are closer to unsupported target in the hierarchy.
        // Note that dumps are stored using configurable name, but grouped by the canonical target name.
        val matchingTargets = findMatchingTargets(supportedTargetNames, unsupportedTarget)
        // Load dumps that are a good fit for inference
        val supportedTargetDumps = matchingTargets.map { target ->
            val dumpFile = File(outputApiDir).parentFile.resolve(target.configurableName).resolve(dumpFileName)
            KlibDump.from(dumpFile, target.configurableName).also {
                check(it.targets.single() == target)
            }
        }

        // Load an old dump, if any
        var image: KlibDump? = null
        if (inputImageFile.exists()) {
            if (inputImageFile.length() > 0L) {
                image = KlibDump.from(inputImageFile)
            } else {
                logger.warn(
                    "Project's ABI file exists, but empty: $inputImageFile. " +
                            "The file will be ignored during ABI dump inference for the unsupported target " +
                            unsupportedTarget
                )
            }
        }

        inferAbi(unsupportedTarget, supportedTargetDumps, image).saveTo(outputFile)

        logger.warn(
            "An ABI dump for target $unsupportedTarget was inferred from the ABI generated for the following targets " +
                    "as the former target is not supported by the host compiler: " +
                    "[${matchingTargets.joinToString(",")}]. " +
                    "Inferred dump may not reflect an actual ABI for the target $unsupportedTarget. " +
                    "It is recommended to regenerate the dump on the host supporting all required compilation target."
        )
    }

    private fun findMatchingTargets(
        supportedTargets: Set<KlibTarget>,
        unsupportedTarget: KlibTarget
    ): Collection<KlibTarget> {
        var currentGroup: String? = unsupportedTarget.targetName
        while (currentGroup != null) {
            // If a current group has some supported targets, use them.
            val groupTargets = TargetHierarchy.targets(currentGroup)
            val matchingTargets = supportedTargets.filter { groupTargets.contains(it.targetName) }
            if (matchingTargets.isNotEmpty()) {
                return matchingTargets
            }
            // Otherwise, walk up the target hierarchy.
            currentGroup = TargetHierarchy.parent(currentGroup)
        }
        throw IllegalStateException(
            "The target $unsupportedTarget is not supported by the host compiler " +
                    "and there are no targets similar to $unsupportedTarget to infer a dump from it."
        )
    }
}
