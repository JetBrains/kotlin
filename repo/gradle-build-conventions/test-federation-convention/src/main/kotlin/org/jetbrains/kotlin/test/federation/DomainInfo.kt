/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Project
import org.jetbrains.kotlin.tooling.core.withLinearClosure
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import kotlin.collections.plus

sealed interface DomainInfo {

    /**
     * The corresponding [Domain] this info belongs to
     */
    val domain: Domain

    /**
     * @see DeclaredDomain.includes
     */
    val include: List<String>

    /**
     * @see DeclaredDomain.excludes
     */
    val exclude: List<String>

    /**
     * @see DeclaredDomain.mustRunAllTestsOnChangesIn
     */
    val mustRunAllTestsOnChangesIn: List<DomainInfo>

    companion object
}


/** A [value] relative to the repository [root]. */
data class RepositoryPath(val root: Path, val value: Path) {
    /**
     * Resolves the absolute path relative to the repository root
     */
    fun resolve(): Path = root.resolve(value)

    val parentOrNull: RepositoryPath?
        get() = value.parent?.let { parent -> RepositoryPath(root, parent) }

    override fun toString(): String {
        return value.toString()
    }

    init {
        require(!value.isAbsolute) { "Path must not be absolute" }
    }
}

internal fun Project.repositoryPath(path: Path): RepositoryPath {
    val root = gradle.withLinearClosure { it.parent }.last().rootProject.projectDir.toPath()
    if (!path.isAbsolute) return RepositoryPath(root, path)
    return RepositoryPath(root, root.relativize(path))
}

/**
 * Resolves the domains of the given [path] by walking up from the [path] to the repository root:
 * Domain rules matching a path take precedence over domain rules matching its parent directories.
 * A path inside a globally ignored directory is always ignored.
 */
fun DomainInfo.Companion.resolveDomainInfosOf(path: RepositoryPath): List<DomainInfo> {
    val domains = mutableSetOf<DomainInfo>()
    val excludes = mutableSetOf<DomainInfo>()

    var currentPath: RepositoryPath? = path
    while (currentPath != null) {

        /* The path is globally ignored, it does not belong to any domain */
        if (ignoreRules.includes(currentPath) == true) {
            return emptyList()
        }

        allDomainInfos.minus(excludes).forEach { currentDomain ->
            val includes = domainRules.getValue(currentDomain).includes(currentPath)

            if (includes == true) {
                domains += currentDomain
            }

            if (includes == false) {
                excludes += currentDomain
            }
        }

        currentPath = currentPath.parentOrNull
    }

    if (domains.isEmpty()) {
        domains.add(UnknownDomainInfo)
    }

    return domains.sortedBy { it.domain.name }
}


val RepositoryPath.domains: List<Domain>
    get() = DomainInfo.resolveDomainInfosOf(this).map { it.domain }

val RepositoryPath.isTestFederationIgnored: Boolean
    get() = withLinearClosure { it.parentOrNull }.any { ignoreRules.includes(it) == true }

/**
 * Global 'ignore' rules are evaluated in declaration order: the last matching rule wins.
 * Rules starting with '!' re-include paths matched by a previous rule.
 */
private val ignoreRules: PathInclusionRule by lazy {
    compilePathInclusionRules(ignores)
}

/**
 * Domain 'include' and 'exclude' rules are each evaluated in declaration order: the last matching rule wins.
 * 'exclude' rules can only narrow the set of included paths: they can never include a path (see [withExcluded]).
 * Rules starting with '!' negate the rule (e.g. '!foo' in 'exclude' revokes the exclusion of 'foo' by a previous 'exclude' rule).
 */
private val domainRules: Map<DomainInfo, PathInclusionRule> by lazy {
    allDomainInfos.associateWith { domain ->
        compilePathInclusionRules(domain.include) withExcluded compilePathInclusionRules(domain.exclude)
    }
}

internal fun interface PathInclusionRule {
    /**
     * @return `true` if the [path] is included, `false` if it is excluded, `null` if this rule does not match the [path]
     */
    fun includes(path: RepositoryPath): Boolean?
}

/**
 * Creates a rule from the given glob pattern: Matching paths are included.
 * Patterns starting with '!' are negated: Matching paths are excluded.
 */
internal fun PathInclusionRule(rule: String): PathInclusionRule {
    return if (rule.startsWith("!")) !PathMatcherInclusionRule(rule.removePrefix("!"))
    else PathMatcherInclusionRule(rule)
}

private fun PathInclusionRule(rules: List<PathInclusionRule>): PathInclusionRule {
    return CompositePathInclusionRule(rules)
}

internal fun compilePathInclusionRules(rules: List<String>): PathInclusionRule {
    return PathInclusionRule(rules.map(::PathInclusionRule))
}

/**
 * Combines two rules: the [other] rule wins if it matches the path; otherwise, this rule is used.
 */
internal operator fun PathInclusionRule.plus(other: PathInclusionRule): PathInclusionRule {
    if (this is CompositePathInclusionRule && other is CompositePathInclusionRule) {
        return CompositePathInclusionRule(this.rules + other.rules)
    }

    if (this is CompositePathInclusionRule) {
        return CompositePathInclusionRule(this.rules + other)
    }

    if (other is CompositePathInclusionRule) {
        return CompositePathInclusionRule(listOf(this) + other.rules)
    }

    return CompositePathInclusionRule(listOf(this, other))
}

/**
 * Narrows this rule by the [excluded] rule: paths included by the [excluded] rule are excluded.
 * Otherwise (the [excluded] rule does not match, or matches with a negated rule), this rule is used.
 * The [excluded] rule can therefore never include a path.
 *
 * Note: The [excluded] rule is evaluated even if this rule does not match the path:
 * An exclusion of a directory must win over an inclusion of its parent directories.
 */
internal infix fun PathInclusionRule.withExcluded(excluded: PathInclusionRule): PathInclusionRule = PathInclusionRule { path ->
    if (excluded.includes(path) == true) false
    else this@withExcluded.includes(path)
}

/**
 * Inverts the rule: included paths become excluded and vice versa.
 */
internal operator fun PathInclusionRule.not(): PathInclusionRule {
    return PathInclusionRule { path -> this.includes(path)?.not() }
}

private class PathMatcherInclusionRule(glob: String) : PathInclusionRule {
    private val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:$glob")

    override fun includes(path: RepositoryPath): Boolean? {
        return if (matcher.matches(path.value)) true else null
    }
}

private class CompositePathInclusionRule(val rules: List<PathInclusionRule>) : PathInclusionRule {
    override fun includes(path: RepositoryPath): Boolean? {
        return rules.asReversed().firstNotNullOfOrNull { rule -> rule.includes(path) }
    }
}
