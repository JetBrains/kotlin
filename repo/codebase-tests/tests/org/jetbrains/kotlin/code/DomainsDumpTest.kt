/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import kotlinx.coroutines.*
import org.jetbrains.kotlin.code.DomainsDumpTest.Companion.sanitize
import org.jetbrains.kotlin.repoTestFixtures.isGitIgnored
import org.jetbrains.kotlin.repoTestFixtures.isTestFederationIgnored
import org.jetbrains.kotlin.repoTestFixtures.toRepositoryPath
import org.jetbrains.kotlin.test.isTeamCityBuild
import org.jetbrains.kotlin.testFederation.Domain
import org.jetbrains.kotlin.testFederation.RepositoryPath
import org.jetbrains.kotlin.testFederation.allDomainInfos
import org.jetbrains.kotlin.testFederation.domains
import org.jetbrains.kotlin.testFederation.isTestFederationIgnored
import org.jetbrains.kotlin.tooling.core.withClosure
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.test.Test

class DomainsDumpTest {

    @Test
    fun `test - domains dump is up to date`() {
        val tree = generateDomainsTree()

        domainsDumpFile.checkDump(tree.generateDomainsDump()) { expectText, actualText ->
            if (expectText != actualText) {
                throw AssertionFailedError(
                    "${domainsDumpFile.name} mismatch: ${domainsDumpFile.name}",
                    FileInfo(domainsDumpFile.absolutePath, expectText.encodeToByteArray()),
                    actualText,
                )
            }
        }

        domainsIgnoreDumpFile.checkDump(tree.generateDomainsIgnoreDump()) { expectedText, actualText ->
            assertIgnoredPathsDumpContains(expectedText, actualText)
        }

        /* Check if any 'include' or 'exclude' rules are orphan (not matching anything) */

        fun globMatchesAnyNode(glob: String): Boolean {
            val matcher = FileSystems.getDefault().getPathMatcher("glob:$glob")
            return tree.withClosure { it.children }.any { node ->
                matcher.matches(node.path.value)
            }
        }

        class UnmatchedRule(val domain: org.jetbrains.kotlin.testFederation.DomainInfo, val rule: String, val isInclude: Boolean)

        val unmatchedRules = mutableListOf<UnmatchedRule>()

        allDomainInfos.forEach { domain ->
            domain.include.forEach forEachRule@{ include ->
                if (!globMatchesAnyNode(include)) {
                    unmatchedRules.add(UnmatchedRule(domain, include, isInclude = true))
                }
            }

            domain.exclude.forEach forEachRule@{ exclude ->
                if (!globMatchesAnyNode(exclude)) {
                    unmatchedRules.add(UnmatchedRule(domain, exclude, isInclude = false))
                }
            }
        }

        if (unmatchedRules.isNotEmpty()) error(buildString {
            appendLine("Unmatched includes/excludes found")
            unmatchedRules.groupBy { it.domain }.forEach { [domain, nodes] ->
                appendLine("${domain.domain.name}:")
                val includes = nodes.filter { it.isInclude }
                if (includes.isNotEmpty()) {
                    appendLine("  include:")
                    includes.forEach { include ->
                        appendLine("    - \"${include.rule}\"")
                    }
                }
                val excludes = nodes.filterNot { it.isInclude }
                if (excludes.isNotEmpty()) {
                    appendLine("  exclude:")
                    excludes.forEach { exclude ->
                        appendLine("    - \"${exclude.rule}\"")
                    }
                }
            }
        })
    }

    companion object {
        fun String.sanitize() = this.replace(Regex("\\R"), "\n").trim()
    }

    object Update {
        @JvmStatic
        fun main(args: Array<String>) {
            val tree = generateDomainsTree()
            mapOf(
                domainsDumpFile to tree.generateDomainsDump(),
                domainsIgnoreDumpFile to tree.generateDomainsIgnoreDump(),
            ).forEach { [file, actualText] ->
                val expectText = if (file.isFile) file.readText().sanitize() else null
                if (actualText != expectText) {
                    file.writeText(actualText)
                    println("Updated: ${file.toURI().toURL()}")
                } else {
                    println("Up to date: ${file.toURI().toURL()}")
                }
            }
        }
    }
}

private val domainsDumpFile = File("repo/domains.dump.txt")

private val domainsIgnoreDumpFile = File("repo/domains.ignore.dump.txt")

internal fun File.checkDump(actualText: String, isTeamCity: Boolean = isTeamCityBuild, check: (String, String) -> Unit) {
    try {
        check(readText().sanitize(), actualText)
    } catch (failure: AssertionFailedError) {
        if (isTeamCity) {
            resolveSibling("$nameWithoutExtension-actual.txt").writeText(actualText)
        }
        throw failure
    }
}

/**
 * We do not assert that the dump matches fully.
 * Removing an ignored filed is still allowed to be 'fast tracked on CI', but could render the dump 'stale' which could block merges.
 * Therefore, it is acceptable to have entries in the ignored dump which would be ignored, but  no longer exist in the repostiory.
 * Regular updates to the dumps will remove such entries
 */
internal fun assertIgnoredPathsDumpContains(expectedText: String, actualText: String) {
    fun String.entries(): Set<String> = lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("- ") }
        .map { it.removePrefix("- ") }
        .toSet()

    val expectedPaths = expectedText.entries()
    val actualPaths = actualText.entries()
    val missingPaths = actualPaths - expectedPaths

    val noLongerIgnoredPaths = (expectedPaths - actualPaths)
        .filterNot { path -> Path(path).isTestFederationIgnored() }

    if (missingPaths.isNotEmpty() || noLongerIgnoredPaths.isNotEmpty()) {
        throw AssertionFailedError(
            buildString {
                appendLine("${domainsIgnoreDumpFile.name} mismatch:")
                if (missingPaths.isNotEmpty()) {
                    appendLine("Missing ignored paths:")
                    missingPaths.forEach { appendLine(" - $it") }
                }
                if (noLongerIgnoredPaths.isNotEmpty()) {
                    appendLine("Extra paths no longer covered by ignore rules:")
                    noLongerIgnoredPaths.forEach { appendLine(" - $it") }
                }
            },
            FileInfo(domainsIgnoreDumpFile.absolutePath, expectedText.encodeToByteArray()),
            actualText,
        )
    }
}

private fun generateDomainsTree(): Node {
    return runBlocking(Dispatchers.Default) {
        val tree = absoluteRepositoryRoot.toNode() ?: error("Missing root tree")
        if (tree !is Node) error("Expected a tree of nodes. Found: $tree")
        tree.conflate()
    }
}

private fun Node.generateDomainsDump(): String {
    return buildString {
        appendDumpHeader()

        fun <T> sorting() = compareBy<Map.Entry<Collection<Domain>, T>> { [domains, _] -> domains.size }
            .thenComparing { [domains, _] -> domains.sumOf { it.ordinal } }

        val allNodes = this@generateDomainsDump.withClosure { it.children }

        allNodes.filter { it.children.isEmpty() }
            .groupBy { it.domains }.entries.sortedWith(sorting()).forEach { [domains, nodes] ->
                appendLine("${domains.joinToString(", ") { it.name }}:")
                nodes.toList().sortedBy { it.path.value.invariantSeparatorsPathString }.forEach { node ->
                    appendLine(" - ${node.path.value.invariantSeparatorsPathString}")
                }
                appendLine()
            }

    }.sanitize()
}

private fun Node.generateDomainsIgnoreDump(): String {
    return buildString {
        appendDumpHeader()
        appendLine("ignored:")
        this@generateDomainsIgnoreDump.withClosure { it.children }
            .flatMap { it.ignored }.toSet().sortedBy { it.value }.forEach { ignored ->
                appendLine(" - ${ignored.value.invariantSeparatorsPathString}")
            }
    }.sanitize()
}

private fun StringBuilder.appendDumpHeader() {
    appendLine("####################################################")
    appendLine("This dump is generated by ${DomainsDumpTest::class.simpleName}.kt")
    appendLine("####################################################")
    appendLine()
}

sealed class AbstractNode

private data class Node(
    val path: RepositoryPath,
    val domains: List<Domain>,
    val children: List<Node>,
    val ignored: Set<RepositoryPath>,
) : AbstractNode()

private data class Ignored(val path: RepositoryPath) : AbstractNode()

private suspend fun Path.toNode(): AbstractNode? {
    return coroutineScope {
        val repositoryPath = toRepositoryPath()
        val domains = repositoryPath.domains
        if (domains.isEmpty()) return@coroutineScope Ignored(repositoryPath)
        val isDirectory = isDirectory()

        val abstractNodes = if (isDirectory) {
            listDirectoryEntries()
                .parallelMap { child ->
                    if (child.isGitIgnored()) return@parallelMap null
                    child.toNode()
                }
                .filterNotNull()
        } else emptyList()

        if (isDirectory && abstractNodes.isEmpty()) {
            return@coroutineScope null
        }

        val children = mutableListOf<Node>()
        val ignored = hashSetOf<RepositoryPath>()

        abstractNodes.forEach { abstractNode ->
            when (abstractNode) {
                is Ignored -> ignored.add(abstractNode.path)
                is Node -> {
                    // Propagate ignores upwards in the tree
                    children.add(abstractNode.copy(ignored = emptySet()))
                    ignored.addAll(abstractNode.ignored)
                }
            }
        }

        Node(repositoryPath, domains, children.sortedBy { it.path.value }, ignored)
    }
}

private suspend fun Node.conflate(): Node {
    val conflatedChildren = children.parallelMap { it.conflate() }

    /* If a directory only contains directories, then it can be conflated */
    if (children.all { it.children.isEmpty() } && children.all { it.path.resolve().isDirectory() }) {
        return copy(children = emptyList())
    }

    val newChildren = if (conflatedChildren.any { child -> child.domains != domains || child.children.isNotEmpty() }) conflatedChildren
    else emptyList()

    return copy(children = newChildren)
}

private suspend fun <T, R> Iterable<T>.parallelMap(transform: suspend (T) -> R): List<R> {
    return coroutineScope {
        map { value ->
            async { transform(value) }
        }.awaitAll()
    }
}
