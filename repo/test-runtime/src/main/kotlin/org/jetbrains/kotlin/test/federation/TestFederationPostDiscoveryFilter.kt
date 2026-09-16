/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.junit.platform.engine.FilterResult
import org.junit.platform.engine.FilterResult.excluded
import org.junit.platform.engine.FilterResult.included
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.PostDiscoveryFilter
import kotlin.jvm.optionals.getOrNull
import kotlin.math.absoluteValue

class TestFederationPostDiscoveryFilter : PostDiscoveryFilter {
    override fun apply(descriptor: TestDescriptor): FilterResult {
        val source = descriptor.source.getOrNull() as? MethodSource
            ?: return included("Not a method-based test")
        if (testFederationMode == null) return included("$TEST_FEDERATION_MODE_KEY is not set")
        if (testFederationMode == TestFederationMode.Full) return included("'TestFederationMode.Full' is set")

        if (isAutoSmokeTest(descriptor, source)) return included("Auto smoke test selected")
        if (isMustRunAlways(descriptor)) return included("@${MustRunAlways::class.java.simpleName}")

        /* Select tests marked to run for one of the changed domains. */
        val changedDomains = testFederationChangedDomains
            ?: return excluded("Missing '${TEST_FEDERATION_CHANGED_DOMAINS_KEY}'")
        val contracts = changedDomains.filter { domain -> isContract(domain, descriptor) }
        if (contracts.isNotEmpty()) return included("Contracts: ${contracts.joinToString(", ")}")
        return excluded("Not selected automatically / Not @MustRunAlways / Not a contract test")
    }
}

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

private fun isMustRunAlways(descriptor: TestDescriptor): Boolean =
    descriptor.tags.any { it.name == "smoke" }

private fun isContract(domain: Domain, descriptor: TestDescriptor) =
    descriptor.tags.any { it.name == "contract:${domain.name}" }
