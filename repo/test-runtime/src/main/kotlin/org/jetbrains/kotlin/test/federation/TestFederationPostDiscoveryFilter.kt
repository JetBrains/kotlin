/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.jetbrains.kotlin.testFederation.TestSubset.*
import org.junit.platform.engine.FilterResult
import org.junit.platform.engine.FilterResult.excluded
import org.junit.platform.engine.FilterResult.included
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestTag
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.PostDiscoveryFilter
import kotlin.jvm.optionals.getOrNull
import kotlin.math.absoluteValue

internal class TestFederationPostDiscoveryFilter : PostDiscoveryFilter {
    override fun apply(descriptor: TestDescriptor): FilterResult {
        val requestedSubsets = testFederationSubsets
        val source = descriptor.source.getOrNull() as? MethodSource ?: return included("Not a method-based test")
        val test = FederatedTest(descriptor, source)

        checkSubsetDeclarations(test)

        if (AllTests in requestedSubsets) {
            return included("Selected by AllTests")
        }
        if (test.isSmokeTest) {
            when {
                SmokeTests in requestedSubsets -> return included("Selected by SmokeTests")
                !test.isContractTest -> return excluded("Not selected smoke test")
            }
        }
        if (test.isContractTest) {
            val matchingContracts = test.contracts.intersect(requestedSubsets)
            return when {
                matchingContracts.isNotEmpty() -> included("Selected by " + matchingContracts.joinToString(", "))
                else -> excluded("Not selected contract test")
            }
        }
        return excluded("Not selected plain test")
    }

    private class FederatedTest(
        val descriptor: TestDescriptor,
        val source: MethodSource,
    ) {
        val isSmokeTest: Boolean by lazy { smokeTag in descriptor.tags || isAutoSmokeTest() }

        val isContractTest: Boolean by lazy { contracts.isNotEmpty() }

        val contracts: Set<TestSubset> by lazy {
            descriptor.tags
                .map(TestTag::getName)
                .mapNotNull(::contractSubsetFromTag)
                .filter { it !in selfContracts }
                .toSet()
        }

        /**
         * Selects an approximate percentage of tests using a hash of each test's identity.
         * The same identity gives the same selection for a given percentage.
         * Test templates and factories are selected as a whole, before their children are registered.
         */
        private fun isAutoSmokeTest(): Boolean {
            if (autoSmokeTestPercentage <= 0) return false
            if (autoSmokeTestPercentage >= 100) return true
            var hashCode = source.className.hashCode()
            hashCode = hashCode * 31 + source.methodName.hashCode()
            hashCode = hashCode * 31 + descriptor.uniqueId.toString().hashCode()
            return (hashCode % 100).absoluteValue < autoSmokeTestPercentage
        }
    }

    private fun checkSubsetDeclarations(test: FederatedTest) {
        val subsetsContainingThisTest = buildSet {
            if (test.isSmokeTest) add(SmokeTests)
            if (test.isContractTest) addAll(test.contracts)
        }
        val subsetsDeclaredInGradle = testFederationDeclaredSubsets.orEmpty()

        val undeclaredSubsets = subsetsContainingThisTest - subsetsDeclaredInGradle

        check(undeclaredSubsets.isEmpty()) {
            buildString {
                appendLine(
                    """
                    |Undeclared test subsets for task '$testFederationTaskPath'
                    ${undeclaredSubsets.joinToString("\n") { "|- $it" }}
                    |
                    |DETAILS
                    |-------
                    |The test '${test.source.className}.${test.source.methodName}'
                    |belongs to $subsetsContainingThisTest
                    |but the test task declares only $subsetsDeclaredInGradle
                    |
                    |HOW TO FIX
                    |----------
                    |testFederation {
                    ${undeclaredSubsets.joinToString("\n") { "|    " + it.gradleDeclaration() }}
                    |}
                    """.trimMargin()
                )
            }
        }
    }
}

private fun TestSubset.gradleDeclaration(): String =
    when (this) {
        SmokeTests -> "smokeTests()"
        else -> {
            val domain = testFederationDomainsByContractSubset.getValue(this)
            "contractTestsFor(${domain.name})"
        }
    }

private val smokeTag = TestTag.create("smoke")
private val selfContracts = testFederationDomains.map(::contractTestsSubsetOf).toSet()
private val testFederationDomainsByContractSubset = Domain.entries.associateBy(::contractTestsSubsetOf)
