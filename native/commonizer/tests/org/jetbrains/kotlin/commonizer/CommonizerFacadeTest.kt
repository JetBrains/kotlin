/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.ResultsConsumer.ModuleResult
import org.jetbrains.kotlin.commonizer.ResultsConsumer.Status
import org.jetbrains.kotlin.commonizer.konan.NativeManifestDataProvider
import org.jetbrains.kotlin.commonizer.utils.MockModulesProvider
import org.jetbrains.kotlin.commonizer.utils.MockNativeManifestDataProvider
import org.jetbrains.kotlin.commonizer.utils.MockResultsConsumer
import org.junit.Test
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalContracts
class CommonizerFacadeTest {

    @Test
    fun nothingToCommonize0() = doTestNothingToCommonize(
        emptyMap()
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
    fun commonized3() = doTestSuccessfulCommonization(
        mapOf(
            "target1" to listOf("foo")
        )
    )

    @Test
    fun commonizedWithDifferentModules() = doTestNothingToCommonize(
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

        private fun Map<String, List<String>>.toCommonizerParameters(
            resultsConsumer: ResultsConsumer,
            manifestDataProvider: (CommonizerTarget) -> NativeManifestDataProvider = { MockNativeManifestDataProvider(it) },
            commonizerSettings: CommonizerSettings = DefaultCommonizerSettings,
        ): CommonizerParameters {
            val targetDependentModuleNames = mapKeys { (targetName, _) -> LeafCommonizerTarget(targetName) }.toTargetDependent()
            val sharedTarget = SharedCommonizerTarget(targetDependentModuleNames.targets.allLeaves())

            return CommonizerParameters(
                outputTargets = setOf(sharedTarget),
                dependenciesProvider = TargetDependent(sharedTarget.withAllLeaves()) { null },
                manifestProvider = TargetDependent(sharedTarget.withAllLeaves(), manifestDataProvider),
                targetProviders = targetDependentModuleNames.map { target, moduleNames ->
                    TargetProvider(
                        target = target,
                        modulesProvider = MockModulesProvider.create(moduleNames)
                    )
                },
                resultsConsumer = resultsConsumer,
                settings = commonizerSettings,
            )
        }

        private fun doTestNothingToCommonize(originalModules: Map<String, List<String>>) {
            val results = MockResultsConsumer()
            runCommonization(originalModules.toCommonizerParameters(results))
            assertEquals(Status.NOTHING_TO_DO, results.status)
            assertTrue(results.modulesByTargets.isEmpty())
        }

        private fun doTestSuccessfulCommonization(originalModules: Map<String, List<String>>) {
            val results = MockResultsConsumer()
            runCommonization(originalModules.toCommonizerParameters(results))
            assertEquals(Status.DONE, results.status)

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
                actualModuleResults = results.modulesByTargets.getValue(results.sharedTarget)
            )

            results.leafTargets.forEach { target ->
                val allModuleNames = originalModules.getValue(target.name).toSet()
                val expectedMissingModuleNames = allModuleNames - expectedCommonModuleNames

                assertModulesMatch(
                    expectedCommonizedModuleNames = expectedCommonModuleNames,
                    expectedMissingModuleNames = expectedMissingModuleNames,
                    actualModuleResults = results.modulesByTargets.getValue(target)
                )
            }
        }

        private fun assertModulesMatch(
            expectedCommonizedModuleNames: Set<String>,
            expectedMissingModuleNames: Set<String>,
            actualModuleResults: Collection<ModuleResult>
        ) {

            val actualCommonizedModuleNames = mutableSetOf<String>()
            val actualMissingModuleNames = mutableSetOf<String>()

            actualModuleResults.forEach { moduleResult ->
                actualCommonizedModuleNames += moduleResult.libraryName
            }

            assertEquals(expectedCommonizedModuleNames.size + expectedMissingModuleNames.size, actualModuleResults.size)
            assertEquals(expectedCommonizedModuleNames, actualCommonizedModuleNames)
            assertEquals(expectedMissingModuleNames, actualMissingModuleNames)
        }
    }
}
