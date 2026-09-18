/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CodeRuleRepositoryTests {
    private fun projectOf(vararg files: Pair<String, String>): Project {
        val pathToText = files.associate { [path, text] -> ProjectFilePath(path) to text }
        return projectOf(pathToText)
    }

    private fun projectOf(files: Map<ProjectFilePath, String>): Project = object : Project {
        override suspend fun readLines(path: ProjectFilePath): List<String>? =
            files[path]?.lines()

        override suspend fun fileExists(path: ProjectFilePath): Boolean = path in files
    }

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
            patterns = listOf("*"),
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
            patterns = listOf("*"),
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
                    
                    Applies to: `${todoRule.patterns.patterns.single()}`
                    
                    ${todoRule.text}
                """.trimIndent()
            )

            put(
                irNodeRule.source,
                """
                    |# ${irNodeRule.name}
                    |
                    |Applies to: `${irNodeRule.patterns.patterns.single()}`
                    |
                    |${irNodeRule.text}
                    |
                    |# ${irTestRule.name}
                    |
                    |Applies to:`${irTestRule.patterns.patterns.single()}`
                    |
                    |
                    |${irTestRule.text}
                """.trimMargin()
            )

            put(
                nativeSpecificRule.source,
                """
                    # ${nativeSpecificRule.name}
                    
                    Applies to: `${nativeSpecificRule.patterns.patterns.single()}`
                    
                    ${nativeSpecificRule.text}
                """.trimIndent()
            )

            put(
                kotlinNativeRule.source,
                """
                    @../native/code-rules.md
                    
                    @/compiler/ir/code-rules.md
                    
                    
                    # ${kotlinNativeRule.name}
                    Applies to:
                    ```
                    ${kotlinNativeRule.patterns.patterns.first()}
                    ${kotlinNativeRule.patterns.patterns.last()}
                    ```
                    
                    ${kotlinNativeRule.text}
                """.trimIndent()
            )

            put(
                ProjectFilePath("js/code-rules.md"),
                "@../compiler/ir/code-rules.md"
            )
        }

        val ruleRepo = CodeRuleRepository(projectOf(files))

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

    private suspend fun CodeRuleRepository.getRuleNames(path: String): Set<String> =
        getRules(ProjectFilePath(path)).mapTo(mutableSetOf()) { it.name }

    @Test
    fun `include doesn't include files in enclosing directories`() = runBlocking {
        val ruleRepo = CodeRuleRepository(
            projectOf(
                "lib/code-rules.md" to """
                    # Lib rule
                    
                    Applies to: `*`
                """.trimIndent(),
                "lib/sub/code-rules.md" to """
                    # Lib sub rule
                    
                    Applies to: `*`
                """.trimIndent(),
                "lib/shared.md" to """
                    # Lib shared rule
                    
                    Applies to: `*`
                """.trimIndent(),
                "lib/sub/shared.md" to """
                    # Lib sub shared rule
                    
                    Applies to: `*`
                """.trimIndent(),
                "app/code-rules.md" to """
                    @/lib/sub/code-rules.md
                    @/lib/sub/shared.md
                """.trimIndent(),
            )
        )

        assertEquals(setOf("Lib rule", "Lib sub rule"), ruleRepo.getRuleNames("lib/sub/foo.kt"))
        assertEquals(setOf("Lib sub rule", "Lib sub shared rule"), ruleRepo.getRuleNames("app/foo.kt"))
    }

    @Test
    fun `patterns are relative to the rule file directory`() = runBlocking {
        val ruleRepo = CodeRuleRepository(
            projectOf(
                "code-rules.md" to """
                    # Root rule

                    Applies to: `/app/src`
                """.trimIndent(),
                "app/code-rules.md" to """
                    # App rule

                    Applies to: `src/main`
                """.trimIndent(),
            )
        )

        assertEquals(setOf("Root rule", "App rule"), ruleRepo.getRuleNames("app/src/main/foo.kt"))
        assertEquals(setOf("Root rule"), ruleRepo.getRuleNames("app/src/foo.kt"))
        assertEquals(emptySet(), ruleRepo.getRuleNames("app/sub/app/src/main/foo.kt"))
    }

    @Test
    fun `included rule patterns are relative to the including file directory`() = runBlocking {
        val ruleRepo = CodeRuleRepository(
            projectOf(
                "lib/code-rules.md" to """
                    # Lib rule

                    Applies to: `src`
                """.trimIndent(),
                "app/code-rules.md" to "@/lib/code-rules.md",
                "src/app/code-rules.md" to "@/lib/code-rules.md",
            )
        )

        assertEquals(setOf("Lib rule"), ruleRepo.getRuleNames("lib/src/foo.kt"))
        assertEquals(emptySet(), ruleRepo.getRuleNames("lib/test/foo.kt"))

        assertEquals(setOf("Lib rule"), ruleRepo.getRuleNames("app/src/foo.kt"))
        assertEquals(emptySet(), ruleRepo.getRuleNames("app/test/foo.kt"))

        // Enclosing directories of the including file don't matter:
        assertEquals(emptySet(), ruleRepo.getRuleNames("src/app/foo.kt"))
        assertEquals(setOf("Lib rule"), ruleRepo.getRuleNames("src/app/src/foo.kt"))
    }

    @Test
    fun `included rule with anchored pattern is rejected`() = runBlocking {
        val ruleRepo = CodeRuleRepository(
            projectOf(
                "lib/code-rules.md" to """
                    # Lib rule

                    Applies to:
                    ```
                    *.kt
                    src/main
                    ```
                """.trimIndent(),
                "app/code-rules.md" to "@/lib/code-rules.md",
            )
        )

        assertEquals(setOf("Lib rule"), ruleRepo.getRuleNames("lib/src/main/foo.kt"))

        val exception = assertFailsWith<IllegalStateException> { ruleRepo.getRuleNames("app/src/main/foo.kt") }
        assertEquals(
            """
                Rule "Lib rule" in lib/code-rules.md applies to files in app/
                because app/code-rules.md includes it (directly or transitively).
                Rules included from another directory can have only unanchored patterns:
                without `/` except a trailing one, or starting with `**/`. But the rule has:
                src/main
            """.trimIndent(),
            exception.message
        )
    }

    @Test
    fun `rule included from the same directory can have anchored patterns`() = runBlocking {
        val ruleRepo = CodeRuleRepository(
            projectOf(
                "lib/shared.md" to """
                    # Shared rule

                    Applies to: `src/main`
                """.trimIndent(),
                "lib/code-rules.md" to "@shared.md",
                "app/code-rules.md" to "@/lib/code-rules.md",
            )
        )

        assertEquals(setOf("Shared rule"), ruleRepo.getRuleNames("lib/src/main/foo.kt"))
        assertEquals(emptySet(), ruleRepo.getRuleNames("lib/sub/src/main/foo.kt"))

        // But not if included transitively from another directory:
        val exception = assertFailsWith<IllegalStateException> { ruleRepo.getRuleNames("app/src/main/foo.kt") }
        assertContains(
            exception.message.orEmpty(),
            """Rule "Shared rule" in lib/shared.md applies to files in app/"""
        )
    }
}
