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
import org.jetbrains.kotlin.tooling.core.withLinearClosure
import java.io.File
import kotlin.io.path.Path


/**
 * Infer which [Domain]s are affected by the current set of changes.
 * This service uses the underlying [FeatureBranchDiffBuildService] to determine the 'diff' of the current branch
 * Based upon the ProjectDomain declaration (in ProjectDomains.yaml) and the current diff, a set of affected ProjectDomains can be inferred.
 */
internal val Project.affectedDomainsService: Provider<AffectedDomainsBuildService>
    get() = gradle.sharedServices.registerIfAbsent("affectedDomainsBuildService", AffectedDomainsBuildService::class.java) {
        parameters.repositoryRoot.set(gradle.withLinearClosure { it.parent }.last().rootProject.projectDir)
        parameters.diffService.set(featureBranchDiffService)
    }

internal abstract class AffectedDomainsBuildService : BuildService<AffectedDomainsBuildService.Params>, AutoCloseable {
    interface Params : BuildServiceParameters {
        val repositoryRoot: Property<File>
        val diffService: Property<FeatureBranchDiffBuildService>
    }

    private var affectedDomainsValue: Set<Domain>? = null

    @get:Synchronized
    val affectedDomains: Set<Domain>
        get() {
            affectedDomainsValue?.let { return it }
            val root = parameters.repositoryRoot.get().toPath()
            val changes = parameters.diffService.get().diff.map { rawPath -> RepositoryPath(root, Path(rawPath)) }
            val commitMessages = parameters.diffService.get().messages
            val affectedDomains = inferDirectlyAffectedDomains(changes, commitMessages)
            affectedDomainsValue = affectedDomains
            return affectedDomains
        }

    @Synchronized
    override fun close() {
        affectedDomainsValue = null
    }
}

private fun inferDirectlyAffectedDomains(changes: List<RepositoryPath>, commitMessages: List<String>): Set<Domain> {
    return changes.flatMap { it.domains }
        .plus(resolveAffectedDomainsFromCommitMessages(commitMessages))
        .toSortedSet()
}

/**
 * `key`: A domain whose changes trigger contracts.
 * `value`: Domains whose full-domain contracts are triggered by the key.
 */
internal val contractedDomainsByTrigger: Map<Domain, List<Domain>> = buildMap<Domain, MutableList<Domain>> {
    allDomainInfos.forEach { domainInfo ->
        put(domainInfo.domain, mutableListOf())
    }

    allDomainInfos.forEach { domainInfo ->
        domainInfo.contract.forEach { trigger ->
            get(trigger.domain)?.add(domainInfo.domain)
        }
    }
}

internal fun Iterable<Domain>.withContractedDomains(): Set<Domain> {
    return buildSet {
        this@withContractedDomains.forEach { domain ->
            add(domain)
            addAll(contractedDomainsByTrigger[domain].orEmpty())
        }
    }.toSortedSet()
}

internal fun resolveAffectedDomainsFromCommitMessages(commitMessages: List<String>): Set<Domain> {
    val commandRegex = Regex("""\^affects:\v*(?<domains>.*)$""")
    val splitRegex = Regex("""([\h,;])""")

    return buildSet {
        commitMessages.forEach { message ->
            message.lines().forEach { line ->
                val match = commandRegex.matchEntire(line) ?: return@forEach
                val domains = match.groups["domains"]?.value.orEmpty()
                domains.split(splitRegex).map { it.trim() }.forEach { domainString ->
                    runCatching { Domain.fromArgumentString(domainString) }
                        .onSuccess { domains -> addAll(domains.orEmpty()) }
                        .onFailure {
                            throw IllegalArgumentException("Command '$line' contains domain '$domainString', which is not a valid domain")
                        }
                }
            }
        }
    }
}
