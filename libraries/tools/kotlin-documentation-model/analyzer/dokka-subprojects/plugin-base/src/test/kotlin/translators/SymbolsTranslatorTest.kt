/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package translators

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import utils.OnlySymbols
import kotlin.test.Test
import kotlin.test.assertEquals

class SymbolsTranslatorTest : BaseAbstractTest() {
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    @OnlySymbols
    fun `should warn about unresolved symbol`() {
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |fun f(a: UnresolvedSymbols)
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { m ->
                val warn = logger.warnMessages.first()
                val path = m.sourceSets.first().sourceRoots.first().absolutePath
                    .replace("\\","/") // for Win

                assertEquals(
                    "`UnresolvedSymbols` is unresolved in file:///PATH/Test.kt:1:7",
                    warn.replace(path, "PATH")
                )
                assertEquals(
                    1,
                    logger.debugMessages.size
                )
            }
        }
    }

    @Test
    @OnlySymbols
    fun `should warn about unresolved annotation`() {
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |
            |fun f(a: @Unresolved(1) Int) = 0
            |
            |@Deprecated(asda)
            |fun f(a: Int) = 0
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { m ->
                val warns = logger.warnMessages
                val path = m.sourceSets.first().sourceRoots.first().absolutePath
                    .replace("\\","/") // for Win

                assertEquals(
                    "Unknown annotation `@Unresolved(1)` in file:///PATH/Test.kt:1:10",
                    warns[0].replace(path, "PATH")
                )
                assertEquals(
                    "Unsupported annotation value `@Deprecated(asda)` in file:///PATH/Test.kt:3:1",
                    warns[1].replace(path, "PATH")
                )
                assertEquals(
                    1,
                    logger.debugMessages.size
                )
            }
        }
    }
}
