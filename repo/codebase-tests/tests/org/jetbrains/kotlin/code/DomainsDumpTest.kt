/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.jetbrains.kotlin.repoTestFixtures.isTestFederationIgnored
import org.jetbrains.kotlin.test.isTeamCityBuild
import org.jetbrains.kotlin.testFederation.allDomainInfos
import org.jetbrains.kotlin.testFederation.isTestFederationIgnored
import org.jetbrains.kotlin.tooling.core.withClosure
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import java.io.File
import java.nio.file.FileSystems
import kotlin.io.path.Path
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

}

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
