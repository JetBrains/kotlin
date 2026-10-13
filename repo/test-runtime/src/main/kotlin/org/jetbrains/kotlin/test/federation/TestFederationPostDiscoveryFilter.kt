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
        val source = descriptor.source.getOrNull() as? MethodSource ?: return included("Not a method-based test")
        val subsets = testFederationSubsets
        val declaredContracts by lazy { findDeclaredContracts(descriptor) }
        fun isContractTest() = declaredContracts.isNotEmpty()

        if (AllTests in subsets) {
            return included("Selected by AllTests")
        }
        if (isSmokeTest(descriptor, source)) {
            when {
                SmokeTests in subsets -> return included("Selected by SmokeTests")
                !isContractTest() -> return excluded("Not selected smoke test")
            }
        }
        if (isContractTest()) {
            val matchingContracts = declaredContracts.intersect(subsets)
            return when {
                matchingContracts.isNotEmpty() -> included("Selected by " + matchingContracts.joinToString(", "))
                else -> excluded("Not selected contract test")
            }
        }
        return if (PlainTests in subsets) included("Selected by PlainTests")
        else excluded("Not selected plain test")
    }

    private fun isSmokeTest(descriptor: TestDescriptor, source: MethodSource): Boolean =
        smokeTag in descriptor.tags || isAutoSmokeTest(descriptor, source)

    /**
     * Selects an approximate percentage of tests using a hash of each test's identity.
     * The same identity gives the same selection for a given percentage.
     * Test templates and factories are selected as a whole, before their children are registered.
     */
    private fun isAutoSmokeTest(descriptor: TestDescriptor, source: MethodSource): Boolean {
        if (autoSmokeTestPercentage <= 0) return false
        if (autoSmokeTestPercentage >= 100) return true
        var hashCode = source.className.hashCode()
        hashCode = hashCode * 31 + source.methodName.hashCode()
        hashCode = hashCode * 31 + descriptor.uniqueId.toString().hashCode()
        return (hashCode % 100).absoluteValue < autoSmokeTestPercentage
    }

    private fun findDeclaredContracts(descriptor: TestDescriptor): Set<TestSubset> =
        descriptor.tags.map(TestTag::getName)
            .mapNotNull(::contractSubsetFromTag)
            .filter { it !in selfContracts }
            .toSet()

    companion object {
        private val smokeTag = TestTag.create("smoke")
        private val selfContracts = testFederationDomains.map(::contractTestsSubsetOf).toSet()
    }
}
