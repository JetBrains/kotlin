/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.tooling.core.withClosure
import kotlin.io.path.Path

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Infer which [Subsystem]s are affected by the current set of changes.
 * This service uses the underlying [FeatureBranchDiffService] to determine the 'diff' of the current branch
 * Based upon the subsystem declaration (in subsystems.yaml) and the current diff, a set of affected subsystems can be inferred.
 */
internal val Project.affectedSubsystemsService: Provider<AffectedSubsystemsService>
    get() = gradle.sharedServices.registerIfAbsent("affectedSystemBuildService", AffectedSubsystemsService::class.java) {
        parameters.diffService.set(featureBranchDiffService)
    }

internal abstract class AffectedSubsystemsService : BuildService<AffectedSubsystemsService.Params>, AutoCloseable {
    interface Params : BuildServiceParameters {
        val diffService: Property<FeatureBranchDiffService>
    }

    private var cachedValue: Set<Subsystem>? = null

    @get:Synchronized
    val affectedSubsystems: Set<Subsystem>
        get() {
            cachedValue?.let { return it }
            val changes = parameters.diffService.get().diff().map { rawPath -> RepositoryPath(Path(rawPath)) }
            val affected = inferAffectedSubsystems(changes)
            cachedValue = affected
            return affected
        }

    @Synchronized
    override fun close() {
        cachedValue = null
    }
}

private fun inferAffectedSubsystems(changes: List<RepositoryPath>): Set<Subsystem> {
    val affected = mutableSetOf<Subsystem>()

    changes.forEach { changed ->
        /* Find the 'last' subsystem which contains the change */
        val subsystem = SubsystemInfo.all.lastOrNull { subsystem -> changed in subsystem }

        /*
         From this subsystem, all children will be marked as affected
         e.g. if a change is located in 'Compiler' (not within any of its children), then all subsystems of the compiler will
         be marked as affected. If the change, however, would be in 'Wasm', then only wasm will be affected not the parent 'Compiler' system.
        */
        affected.addAll(subsystem?.withClosure { it.subsystems }?.map { it.system }.orEmpty())

        if (subsystem == null) {
            affected.add(Subsystem.Unknown)
        }
    }

    /*
    Since not all subsystems migrated properly into the test federatin, uknown systems will have a implicit dependency to the compiler
     */
    if (Subsystem.Compiler in affected) {
        affected.add(Subsystem.Unknown)
    }

    return affected
}