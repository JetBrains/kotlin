/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.commonizer.utils.MockModulesProvider
import org.jetbrains.kotlin.descriptors.commonizer.utils.assertCommonizationPerformed
import org.junit.Test
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertEquals

@ExperimentalContracts
class CommonizerFacadeTest {

    @Test
    fun nothingToCommonize0() = doTestNothingToCommonize(
        emptyMap()
    )

    @Test
    fun nothingToCommonize1() = doTestNothingToCommonize(
        mapOf(
            "target1" to listOf("foo")
        )
    )

    @Test
    fun commonized1() = doTestSuccessfulCommonization(
        mapOf(
            "target1" to listOf("foo"),
            "target2" to listOf("foo")
        )
    )

    @Test
    fun commonized2() = doTestSuccessfulCommonization(
        mapOf(
            "target1" to listOf("foo", "bar"),
            "target2" to listOf("bar", "foo")
        )
    )

    @Test
    fun commonizedWithDifferentModules() = doTestSuccessfulCommonization(
        mapOf(
            "target1" to listOf("foo"),
            "target2" to listOf("bar")
        )
    )

    @Test
    fun commonizedWithMissingModules() = doTestSuccessfulCommonization(
        mapOf(
            "target1" to listOf("foo", "bar"),
            "target2" to listOf("foo", "qix")
        )
    )

    companion object {
        private fun Map<String, List<String>>.toCommonizationParameters() = CommonizerParameters().also {
            forEach { (targetName, moduleNames) ->
                it.addTarget(
                    TargetProvider(
                        target = LeafTarget(targetName),
                        builtInsClass = DefaultBuiltIns::class.java,
                        builtInsProvider = BuiltInsProvider.defaultBuiltInsProvider,
                        modulesProvider = MockModulesProvider.create(moduleNames),
                        dependeeModulesProvider = null
                    )
                )
            }
        }

        private fun doTestNothingToCommonize(originalModules: Map<String, List<String>>) {
            val result = runCommonization(originalModules.toCommonizationParameters())
            assertEquals(CommonizerResult.NothingToDo, result)
        }

        private fun doTestSuccessfulCommonization(originalModules: Map<String, List<String>>) {
            val result = runCommonization(originalModules.toCommonizationParameters())
            assertCommonizationPerformed(result)

            val expectedCommonModuleNames = mutableSetOf<String>()
            originalModules.values.forEachIndexed { index, moduleNames ->
                if (index == 0)
                    expectedCommonModuleNames.addAll(moduleNames)
                else
                    expectedCommonModuleNames.retainAll(moduleNames)
            }
            assertModulesMatch(
                expectedCommonizedModuleNames = expectedCommonModuleNames,
                expectedMissingModuleNames = emptySet(),
                actualModuleResults = result.modulesByTargets.getValue(result.sharedTarget)
            )

            result.leafTargets.forEach { target ->
                val allModuleNames = originalModules.getValue(target.name).toSet()
                val expectedMissingModuleNames = allModuleNames - expectedCommonModuleNames

                assertModulesMatch(
                    expectedCommonizedModuleNames = expectedCommonModuleNames,
                    expectedMissingModuleNames = expectedMissingModuleNames,
                    actualModuleResults = result.modulesByTargets.getValue(target)
                )
            }
        }

        private fun assertModulesMatch(
            expectedCommonizedModuleNames: Set<String>,
            expectedMissingModuleNames: Set<String>,
            actualModuleResults: Collection<ModuleResult>
        ) {
            assertEquals(expectedCommonizedModuleNames.size + expectedMissingModuleNames.size, actualModuleResults.size)

            val actualCommonizedModuleNames = mutableSetOf<String>()
            val actualMissingModuleNames = mutableSetOf<String>()

            actualModuleResults.forEach { moduleResult ->
                when (moduleResult) {
                    is ModuleResult.Commonized -> actualCommonizedModuleNames += moduleResult.libraryName
                    is ModuleResult.Missing -> actualMissingModuleNames += moduleResult.libraryName
                }
            }

            assertEquals(expectedCommonizedModuleNames, actualCommonizedModuleNames)
            assertEquals(expectedMissingModuleNames, actualMissingModuleNames)
        }
    }
}
