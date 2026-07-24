/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeRuleRepositoryTests {

    @Test
    fun `smoke test`() = runBlocking {
        fun rule(
            name: String,
            text: String,
            patterns: List<String>,
            source: String,
        ): CodeRule = CodeRule(
            name = name,
            text = text,
            patterns = CodeRulePatterns(patterns),
            source = ProjectFilePath(source)
        )

        val todoRule = rule(
            name = "TODOs",
            text = "If a TODO is added to the code, it should be accompanied by a YouTrack issue number.",
            patterns = listOf(),
            source = "code-rules.md"
        )

        val irNodeRule = rule(
            name = "Don't lose IR nodes",
            text = """
                When transforming Kotlin backend IR, make sure to preserve all expression nodes,
                unless it is known that the node can't have side effects.
            """.trimIndent(),
            patterns = listOf("src"),
            source = "compiler/ir/code-rules.md"
        )

        val irTestRule = rule(
            name = "Use existing test infra",
            text = "Use infrastructure from compiler/tests-common-new when adding new test classes",
            patterns = listOf("test"),
            source = "compiler/ir/code-rules.md"
        )

        val nativeSpecificRule = rule(
            name = "Native-specific code location",
            text = "Use native/ and kotlin-native/ only for Kotlin/Native-specific files.",
            patterns = listOf(),
            source = "native/code-rules.md"
        )

        val kotlinNativeRule = rule(
            name = "No new files in kotlin-native/",
            text = "Don't create new files in kotlin-native/. Use native/ instead.",
            patterns = listOf("src", "test"),
            source = "kotlin-native/code-rules.md"
        )

        val files = buildMap {
            put(
                todoRule.source,
                """
                    # ${todoRule.name}
                    
                    ${todoRule.text}
                """.trimIndent()
            )

            put(
                irNodeRule.source,
                """
                    |# ${irNodeRule.name}
                    |
                    |Pattern: ${irNodeRule.patterns.patterns.single()}
                    |
                    |${irNodeRule.text}
                    |
                    |# ${irTestRule.name}
                    |
                    |Pattern:${irTestRule.patterns.patterns.single()}
                    |
                    |
                    |${irTestRule.text}
                """.trimMargin()
            )

            put(
                nativeSpecificRule.source,
                """
                    # ${nativeSpecificRule.name}
                    
                    ${nativeSpecificRule.text}
                """.trimIndent()
            )

            put(
                kotlinNativeRule.source,
                """
                    @../native/code-rules.md
                    
                    @/compiler/ir/code-rules.md
                    
                    
                    # ${kotlinNativeRule.name}
                    Pattern: ${kotlinNativeRule.patterns.patterns.first()}
                    
                    Pattern: ${kotlinNativeRule.patterns.patterns.last()}
                    
                    ${kotlinNativeRule.text}
                """.trimIndent()
            )

            put(
                ProjectFilePath("js/code-rules.md"),
                "@../compiler/ir/code-rules.md"
            )
        }

        val project = object : Project {
            override suspend fun readLines(path: ProjectFilePath): List<String>? =
                files[path]?.lines()

            override suspend fun fileExists(path: ProjectFilePath): Boolean = path in files
        }

        val ruleRepo = CodeRuleRepository(project)

        assertEquals(
            setOf(irNodeRule, todoRule),
            ruleRepo.getRules(ProjectFilePath("compiler/ir/ir.tree/src/org/jetbrains/foo.kt"))
        )

        assertEquals(
            setOf(irTestRule, todoRule),
            ruleRepo.getRules(ProjectFilePath("compiler/ir/ir.tree/test/org/jetbrains/foo.kt"))
        )

        assertEquals(
            setOf(nativeSpecificRule, todoRule),
            ruleRepo.getRules(ProjectFilePath("native/foo.kt"))
        )

        assertEquals(
            setOf(irNodeRule, nativeSpecificRule, kotlinNativeRule, todoRule),
            ruleRepo.getRules(ProjectFilePath("kotlin-native/backend.native/src/bar.kt"))
        )

        assertEquals(
            setOf(irTestRule, todoRule),
            ruleRepo.getRules(ProjectFilePath("js/js.ir/test/baz.kt"))
        )
    }
}
