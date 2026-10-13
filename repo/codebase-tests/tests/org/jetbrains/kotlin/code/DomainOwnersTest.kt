/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.jetbrains.kotlin.testFederation.allDomainInfos
import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Checks that the `team` declared for each domain in `repo/domains.yaml` is declared in `.space/CODEOWNERS`.
 */
class DomainOwnersTest {
    private val codeOwnersFile = File(System.getProperty("codeOwnersTest.spaceCodeOwnersFile"))
    private val codeOwnersTeams = parseCodeOwnersTeams(codeOwnersFile)

    @Test
    fun `domain teams exist in CODEOWNERS`() {
        val problems = allDomainInfos
            .filter { domain -> "\"${domain.team}\"" !in codeOwnersTeams }
            .map { domain -> "${domain.domain.name}: team '${domain.team}' is not declared in $codeOwnersFile" }

        if (problems.isNotEmpty()) {
            fail(problems.joinToString("\n", prefix = "Domain teams missing in CODEOWNERS:\n"))
        }
    }
}

private val ownerNamesRegex = "(\"[^\"]+\")|(\\S+)".toRegex()

private fun parseOwnerNames(owners: String): List<String> =
    ownerNamesRegex.findAll(owners).map { it.value }.toList()

/**
 * Teams are returned in their quoted form, as they appear in CODEOWNERS (e.g. `"Kotlin JVM"`).
 */
private fun parseCodeOwnersTeams(file: File): Set<String> {
    val teams = mutableSetOf<String>()
    file.readLines().forEach { line ->
        when {
            line.startsWith("# SPACE_OWNER: ") ->
                teams += parseOwnerNames(line.removePrefix("# SPACE_OWNER: "))
            line.startsWith("# SPACE_TO_GITHUB_OWNER: ") ->
                teams += parseOwnerNames(line.removePrefix("# SPACE_TO_GITHUB_OWNER: ")).take(1)
        }
    }
    return teams
}
