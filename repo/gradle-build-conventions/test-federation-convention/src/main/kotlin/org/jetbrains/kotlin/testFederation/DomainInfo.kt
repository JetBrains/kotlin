/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Project
import org.jetbrains.kotlin.tooling.core.withLinearClosure
import java.nio.file.FileSystem
import java.nio.file.Path
import kotlin.io.path.isDirectory

internal sealed interface DomainInfo {
    /**
     * @see DeclaredDomain.home
     */
    val home: String

    /**
     * The corresponsing [Domain] this info belongs to
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
     * @see DeclaredDomain.fullyAffectedBy
     */
    val fullyAffectedBy: List<DomainInfo>

    companion object
}


data class RepositoryPath(private val root: Path, val value: Path) {
    val fileSystem: FileSystem get() = root.fileSystem

    /**
     * Resolves the absolute path relative to the repository root
     */
    fun resolve(): Path = root.resolve(value)

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
 * Resolves the domain by the 'most specific' rule.
 */
internal fun DomainInfo.Companion.resolveDomainInfoOf(path: RepositoryPath): DomainInfo {
    val fileSystem = path.fileSystem
    val value = if (path.resolve().isDirectory()) path.value.resolve(".") else path.value

    var domain: DomainInfo = UnknownDomainInfo
    var matchingInclude = ""

    allDomainInfos.forEach { currentDomain ->
        var isInclude = false
        var matchingRule = ""

        currentDomain.exclude.forEach { exclude ->
            if (fileSystem.getPathMatcher("glob:$exclude").matches(value)) {
                if (exclude.length > matchingRule.length) {
                    matchingRule = exclude
                    isInclude = false
                }
            }
        }

        currentDomain.include.forEach { include ->
            if (fileSystem.getPathMatcher("glob:$include").matches(value)) {
                if (include.length > matchingRule.length) {
                    matchingRule = include
                    isInclude = true
                }
            }
        }

        if (isInclude && matchingRule.length > matchingInclude.length) {
            domain = currentDomain
            matchingInclude = matchingRule
        }
    }

    return domain
}


val RepositoryPath.domain: Domain
    get() = DomainInfo.resolveDomainInfoOf(this).domain
