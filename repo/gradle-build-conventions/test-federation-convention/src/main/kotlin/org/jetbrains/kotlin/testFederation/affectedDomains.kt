/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.tooling.core.withClosure
import kotlin.io.path.Path


/**
 * Infer which [Domain]s are affected by the current set of changes.
 * This service uses the underlying [FeatureBranchDiffBuildService] to determine the 'diff' of the current branch
 * Based upon the ProjectDomain declaration (in ProjectDomains.yaml) and the current diff, a set of affected ProjectDomains can be inferred.
 */
internal val Project.affectedDomainsService: Provider<AffectedDomainsBuildService>
    get() = gradle.sharedServices.registerIfAbsent("affectedDomainsBuildService", AffectedDomainsBuildService::class.java) {
        parameters.diffService.set(featureBranchDiffService)
    }

internal abstract class AffectedDomainsBuildService : BuildService<AffectedDomainsBuildService.Params>, AutoCloseable {
    interface Params : BuildServiceParameters {
        val diffService: Property<FeatureBranchDiffBuildService>
    }

    private var cachedValue: Set<Domain>? = null

    @get:Synchronized
    val affectedDomains: Set<Domain>
        get() {
            cachedValue?.let { return it }
            val changes = parameters.diffService.get().diff.map { rawPath -> RepositoryPath(Path(rawPath)) }
            val affected = inferAffectedDomains(changes)
            cachedValue = affected
            return affected
        }

    @Synchronized
    override fun close() {
        cachedValue = null
    }
}

private fun inferAffectedDomains(changes: List<RepositoryPath>): Set<Domain> {
    return changes.map { it.domain }.withAffectedDependencies()
}

internal fun inferAffectedDomains(argumentString: String): Set<Domain>? {
    return Domain.fromArgumentString(argumentString)?.withAffectedDependencies()
}

internal fun Iterable<Domain>.withAffectedDependencies(): Set<Domain> {
    val dependees = buildMap {
        allDomainInfos.forEach { domainInfo ->
            put(domainInfo.domain, mutableListOf())
        }

        allDomainInfos.forEach { domainInfo ->
            domainInfo.fullyAffectedBy.forEach { dependency ->
                get(dependency.domain)?.add(domainInfo.domain)
            }
        }
    }

    return withClosure<Domain> { system -> dependees[system].orEmpty() }
}

