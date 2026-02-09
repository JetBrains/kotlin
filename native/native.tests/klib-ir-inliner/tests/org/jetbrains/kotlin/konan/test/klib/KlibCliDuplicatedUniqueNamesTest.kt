/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.test.utils.patchManifestAsMap
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.collections.set

@Tag("klib")
class KlibCliDuplicatedUniqueNamesTest : AbstractNativeSimpleTest() {
    @Test
    fun `Metadata compilation - DENY strategy`() = runTest(
        strategy = DuplicatedUniqueNameStrategy.DENY,
        expectedDuplicatedNameMessagePrefix = "error",
        freeCompilerArgs = listOf("-Xmetadata-klib")
    )

    @Test
    fun `Metadata compilation - ALLOW_FIRST_WITH_WARNING strategy`() = runTest(
        strategy = DuplicatedUniqueNameStrategy.ALLOW_FIRST_WITH_WARNING,
        expectedDuplicatedNameMessagePrefix = "warning",
        isUnresolvedReferenceErrorExpected = true,
        freeCompilerArgs = listOf("-Xmetadata-klib")
    )

    @Test
    fun `Metadata compilation - ALLOW_ALL_WITH_WARNING strategy`() = runTest(
        strategy = DuplicatedUniqueNameStrategy.ALLOW_ALL_WITH_WARNING,
        expectedDuplicatedNameMessagePrefix = null,
        freeCompilerArgs = listOf("-Xmetadata-klib")
    )

    @Test
    fun `Metadata compilation - default strategy`() = runTest(
        strategy = null,
        expectedDuplicatedNameMessagePrefix = null,
        freeCompilerArgs = listOf("-Xmetadata-klib")
    )

    @Test
    fun `Regular compilation - DENY strategy`() = runTest(
        strategy = DuplicatedUniqueNameStrategy.DENY,
        expectedDuplicatedNameMessagePrefix = "error",
    )

    @Test
    fun `Regular compilation - ALLOW_FIRST_WITH_WARNING strategy`() = runTest(
        strategy = DuplicatedUniqueNameStrategy.ALLOW_FIRST_WITH_WARNING,
        isUnresolvedReferenceErrorExpected = true,
        expectedDuplicatedNameMessagePrefix = "warning",
    )

    @Test
    fun `Regular compilation - ALLOW_ALL_WITH_WARNING strategy`() = runTest(
        strategy = DuplicatedUniqueNameStrategy.ALLOW_ALL_WITH_WARNING,
        expectedDuplicatedNameMessagePrefix = null,
    )

    @Test
    fun `Regular compilation - default strategy`() = runTest(
        strategy = null,
        expectedDuplicatedNameMessagePrefix = "error",
    )

    private fun runTest(
        strategy: DuplicatedUniqueNameStrategy?,
        expectedDuplicatedNameMessagePrefix: String?,
        isUnresolvedReferenceErrorExpected: Boolean = false,
        freeCompilerArgs: List<String>? = null,
    ) {
        val extraCliArgs = buildList {
            freeCompilerArgs?.let(::addAll)
            strategy?.asCliArgument()?.let(::add)
        }

        val modules = newSourceModules {
            addRegularModule("a")
            addRegularModule("b")
            addRegularModule("c") { dependsOn("a", "b") }
        }

        try {
            modules.compileToKlibsViaCli(extraCliArgs = extraCliArgs) { module, successKlib ->
                when (module.name) {
                    "a", "b" -> {
                        // set the same `unique_name`
                        patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                            properties[KLIB_PROPERTY_UNIQUE_NAME] = DUPLICATED_UNIQUE_NAME
                        }
                    }

                    "c" -> if (expectedDuplicatedNameMessagePrefix != null) fail { "Normally unreachable code" }

                    else -> fail { "Unexpected module: ${module.name}" }
                }
            }
        } catch (cte: CompilationToolException) {
            val compilerOutputFromModuleC: List<String> = cte.reason.lines()

            val duplicatedNameMessagePresent = compilerOutputFromModuleC.any {
                it.startsWith("$expectedDuplicatedNameMessagePrefix: KLIB resolver: The same 'unique_name=$DUPLICATED_UNIQUE_NAME' found in more than one library")
            }
            assertTrue(duplicatedNameMessagePresent)

            val unresolvedReferenceErrorPresent = compilerOutputFromModuleC.any {
                it.contains("error: unresolved reference")
            }
            assertTrue(unresolvedReferenceErrorPresent == isUnresolvedReferenceErrorExpected)
        }
    }

    companion object {
        private const val DUPLICATED_UNIQUE_NAME = "DUPLICATED_UNIQUE_NAME"

        private fun DuplicatedUniqueNameStrategy.asCliArgument(): String {
            return CommonKlibBasedCompilerArguments::duplicatedUniqueNameStrategy.cliArgument(alias)
        }
    }
}
