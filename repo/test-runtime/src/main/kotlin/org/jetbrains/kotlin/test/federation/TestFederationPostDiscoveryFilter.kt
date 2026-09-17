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

        if (AllTests in subsets) {
            return included("Selected by AllTests")
        }
        if (isSmokeTest(descriptor, source)) {
            return if (SmokeTests in subsets) included("Selected by SmokeTests")
            else excluded("Not selected smoke test")
        }
        if (isContractTest(descriptor)) {
            return findMatchingContracts(subsets, descriptor)
                .takeIf { it.isNotEmpty() }
                ?.let { matchingContracts -> included("Selected by " + matchingContracts.joinToString(", ")) }
                ?: excluded("Not selected contract test")
        }
        return excluded("Not selected plain test")
    }
}

private fun isSmokeTest(descriptor: TestDescriptor, source: MethodSource): Boolean =
    descriptor.tags.any { it.name == "smoke" } || isAutoSmokeTest(descriptor, source)

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

private fun isContractTest(descriptor: TestDescriptor): Boolean =
    descriptor.declaredContracts.isNotEmpty()

private fun findMatchingContracts(subsets: Set<TestSubset>, descriptor: TestDescriptor): Set<TestSubset> {
    val requestedContracts = subsets.filter(TestSubset::isContract).toSet()
    return descriptor.declaredContracts.intersect(requestedContracts)
}

private val TestDescriptor.declaredContracts: Set<TestSubset>
    get() = tags.map(TestTag::getName)
        .mapNotNull(::contractSubsetFromTag)
        .filterNot(TestSubset::isSelfDeclaredContract)
        .toSet()

private fun TestSubset.isSelfDeclaredContract(): Boolean {
    val selfContracts = testFederationDomains.map(::contractTestsSubsetOf).toSet()
    return this in selfContracts
}
