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
    fun commonizedWithAbsentModules() = doTestSuccessfulCommonization(
        mapOf(
            "target1" to listOf("foo", "bar"),
            "target2" to listOf("foo", "qix")
        )
    )

    companion object {
        private fun Map<String, List<String>>.toCommonizationParameters() = Parameters().also {
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
            assertEquals(Result.NothingToCommonize, result)
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
                expectedAbsentModuleNames = emptySet(),
                actualModuleResults = result.modulesByTargets.getValue(result.sharedTarget)
            )

            result.leafTargets.forEach { target ->
                val allModuleNames = originalModules.getValue(target.name).toSet()
                val expectedAbsentModuleNames = allModuleNames - expectedCommonModuleNames

                assertModulesMatch(
                    expectedCommonizedModuleNames = expectedCommonModuleNames,
                    expectedAbsentModuleNames = expectedAbsentModuleNames,
                    actualModuleResults = result.modulesByTargets.getValue(target)
                )
            }
        }

        private fun assertModulesMatch(
            expectedCommonizedModuleNames: Set<String>,
            expectedAbsentModuleNames: Set<String>,
            actualModuleResults: Collection<ModuleResult>
        ) {
            assertEquals(expectedCommonizedModuleNames.size + expectedAbsentModuleNames.size, actualModuleResults.size)

            val actualCommonizedModuleNames = mutableSetOf<String>()
            val actualAbsentModuleNames = mutableSetOf<String>()

            actualModuleResults.forEach { moduleResult ->
                when (moduleResult) {
                    is ModuleResult.Commonized -> {
                        actualCommonizedModuleNames += moduleResult.module.name.asString().removeSurrounding("<", ">")
                    }
                    is ModuleResult.Absent -> {
                        actualAbsentModuleNames += moduleResult.originalLocation.name
                    }
                }
            }

            assertEquals(expectedCommonizedModuleNames, actualCommonizedModuleNames)
            assertEquals(expectedAbsentModuleNames, actualAbsentModuleNames)
        }
    }
}
