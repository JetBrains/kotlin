/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.klib.*
import kotlinx.validation.api.klib.TargetHierarchy
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.slf4j.LoggerFactory
import javax.inject.Inject

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
@CacheableTask
public abstract class KotlinKlibInferAbiTask : WorkerAwareTaskBase() {
    /**
     * The name of a target to infer a dump for.
     */
    @get:Input
    public abstract val target: Property<KlibTarget>

    /**
     * Newly created dumps that will be used for ABI inference.
     */
    @get:Nested
    public abstract val inputDumps: SetProperty<KlibDumpMetadata>

    /**
     * Previously generated merged ABI dump file, the golden image every dump should be verified against.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val oldMergedKlibDump: RegularFileProperty

    /**
     * A path to an inferred dump file.
     */
    @get:OutputFile
    public abstract val outputAbiFile: RegularFileProperty

    @get:Inject
    public abstract val executor: WorkerExecutor

    @TaskAction
    internal fun generate() {
        val q = executor.classLoaderIsolation {
            it.classpath.from(runtimeClasspath)
        }
        q.submit(KlibInferAbiWorker::class.java) { params ->
            params.target.set(target)
            params.inputDumps.set(inputDumps.get().map {
                KlibMetadataLocal(it.target, it.dumpFile.get().asFile)
            })
            params.oldMergedKlibDump.set(oldMergedKlibDump)
            params.outputAbiFile.set(outputAbiFile)
        }
        q.await()
    }
}

internal interface KlibInferAbiParameters : WorkParameters {
    val target: Property<KlibTarget>
    val inputDumps: SetProperty<KlibMetadataLocal>
    val oldMergedKlibDump: RegularFileProperty
    val outputAbiFile: RegularFileProperty
}

internal abstract class KlibInferAbiWorker : WorkAction<KlibInferAbiParameters> {
    private val logger = LoggerFactory.getLogger(KlibInferAbiWorker::class.java)

    @OptIn(ExperimentalBCVApi::class)
    override fun execute() {
        val availableDumps = parameters.inputDumps.get().map {
            it.target to it.dumpFile
        }.filter { it.second.exists() }.toMap()
        // Find a set of supported targets that are closer to unsupported target in the hierarchy.
        // Note that dumps are stored using configurable name, but grouped by the canonical target name.
        val matchingTargets = findMatchingTargets(availableDumps.keys, parameters.target.get())
        // Load dumps that are a good fit for inference
        val supportedTargetDumps = matchingTargets.map { target ->
            val dumpFile = availableDumps[target]!!
            KlibDump.from(dumpFile, target.configurableName).also {
                check(it.targets.single() == target)
            }
        }

        // Load an old dump, if any
        var image: KlibDump? = null
        val oldDumpFile = parameters.oldMergedKlibDump.asFile.get()
        if (oldDumpFile.exists()) {
            if (oldDumpFile.length() > 0L) {
                image = KlibDump.from(oldDumpFile)
            } else {
                logger.warn(
                    "Project's ABI file exists, but empty: $oldDumpFile. " +
                            "The file will be ignored during ABI dump inference for the unsupported target " +
                            parameters.target.get()
                )
            }
        }

        inferAbi(parameters.target.get(), supportedTargetDumps, image).saveTo(parameters.outputAbiFile.asFile.get())

        logger.warn(
            "An ABI dump for target ${parameters.target.get()} was inferred from the ABI generated for the following targets " +
                    "as the former target is not supported by the host compiler: " +
                    "[${matchingTargets.joinToString(",")}]. " +
                    "Inferred dump may not reflect an actual ABI for the target ${parameters.target.get()}. " +
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
