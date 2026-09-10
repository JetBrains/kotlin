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
 * Infers which domains require all their tests for merging to master.
 * Uses [FeatureBranchDiffBuildService] and `domains.yaml` to find domains containing changed files,
 * then adds domains that declare they must run all tests for those changes and domains requested in commit messages.
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

    private var changedDomainsValue: Set<Domain>? = null
    private var affectedDomainsValue: Set<Domain>? = null

    @get:Synchronized
    val changedDomains: Set<Domain>
        get() {
            changedDomainsValue?.let { return it }
            val root = parameters.repositoryRoot.get().toPath()
            val changes = parameters.diffService.get().diff.map { rawPath -> RepositoryPath(root, Path(rawPath)) }
            val changedDomains = inferChangedDomains(changes)
            changedDomainsValue = changedDomains
            return changedDomains
        }


    @get:Synchronized
    val affectedDomains: Set<Domain>
        get() {
            affectedDomainsValue?.let { return it }
            val commitMessages = parameters.diffService.get().messages
            val affected = inferAffectedDomains(changedDomains, commitMessages)
            affectedDomainsValue = affected
            return affected
        }

    @Synchronized
    override fun close() {
        affectedDomainsValue = null
        changedDomainsValue = null
    }
}

internal fun inferChangedDomains(changes: List<RepositoryPath>): Set<Domain> {
    return changes.flatMap { it.domains }.toSortedSet()
}

internal fun inferAffectedDomains(changedDomains: Set<Domain>, commitMessages: List<String>): Set<Domain> {
    return changedDomains.withAffectedDependencies()
        .plus(resolveAffectedDomainsFromCommitMessages(commitMessages))
        .toSortedSet()
}

/**
 * Maps a domain to the domains that must run all tests when it contains changed files.
 *
 * Only direct declarations are included; this relationship is not transitive.
 */
private val domainDependees: Map<Domain, List<Domain>> = buildMap<Domain, MutableList<Domain>> {
    allDomainInfos.forEach { domainInfo ->
        put(domainInfo.domain, mutableListOf())
    }

    allDomainInfos.forEach { domainInfo ->
        domainInfo.mustRunAllTestsOnChangesIn.forEach { dependency ->
            get(dependency.domain)?.add(domainInfo.domain)
        }
    }
}

internal fun Iterable<Domain>.withAffectedDependencies(): Set<Domain> {
    return buildSet {
        this@withAffectedDependencies.forEach { domain ->
            add(domain)
            addAll(domainDependees[domain].orEmpty())
        }
    }.toSortedSet()
}

internal fun resolveAffectedDomainsFromCommitMessages(commitMessages: List<String>): Set<Domain> {
    val commandRegex = Regex("""\^(?:test|affects):\v*(?<domains>.*)$""")
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
