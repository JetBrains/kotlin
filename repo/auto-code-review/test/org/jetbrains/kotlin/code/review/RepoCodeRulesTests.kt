/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import kotlin.test.Test

class RepoCodeRulesTests {
    @Test
    fun `each rule applies to at least one file`() = runBlocking {
        val repoRoot = File(System.getProperty("kotlin.repo.auto-code-review.rootDir")!!)
        val gitTree = GitWorkingTree(repoRoot, GitCLI)
        val ruleRepo = CodeRuleRepository(gitTree.project)

        val allFiles = gitTree.lsFiles()
        val allRules = allFiles.filter { it.fileName == CODE_RULES_MD }
            .flatMap { ruleRepo.getRulesFromRulesFile(it) }
            .toSet()

        assertTrue(allRules.isNotEmpty(), "No rules found")

        val ruleToFiles = allFiles.flatMap { file ->
            ruleRepo.getRules(file).map { rule -> rule to file }
        }.groupBy({ it.first }, { it.second })

        allRules.forEach { rule ->
            assertEquals(true, ruleToFiles[rule]?.isNotEmpty()) {
                """No files match rule "${rule.name}" defined in ${rule.source}"""
            }
        }
    }
}
