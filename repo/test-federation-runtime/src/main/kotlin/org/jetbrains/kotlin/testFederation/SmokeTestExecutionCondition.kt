/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionConfigurationException
import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.jvm.optionals.getOrNull
import kotlin.math.absoluteValue

class SmokeTestExecutionCondition : ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        if (!context.testMethod.isPresent) return enabled("Test Class is always enabled")
        val allContracts = contracts(context)
        if (!testFederationAllowAffectedBy.containsAll(allContracts)) {
            throw ExtensionConfigurationException(
                """
                This test has contract for domains that are not declared at the Gradle level:
                ${(allContracts - testFederationAllowAffectedBy).joinToString("\n") { "- $it" }}
                
                HOW TO FIX:
                testTask {
                    testFederationAllowAffectedBy = setOf(${(allContracts - testFederationCurrentDomains).sorted().joinToString(", ") { "Domain.$it" }})
                }
                """.trimIndent()
            )
        }
        if (testFederationMode == null) return enabled("$TEST_FEDERATION_MODE_KEY is not set")
        if (testFederationMode == TestFederationMode.Full) return enabled("'TestFederationMode.Full' is set")

        if (isAutoSmokeTest(context)) return enabled("Auto smoke test selected")
        if (isSmokeTest(context)) return enabled("@${SmokeTest::class.java.simpleName}")
        val affected = testFederationAffectedDomains ?: return disabled("Missing '$TEST_FEDERATION_AFFECTED_DOMAINS_KEY'")
        val contracts = affected.intersect(allContracts)
        if (contracts.isNotEmpty()) return enabled("Contracts: ${contracts.joinToString(", ")}")
        return disabled("Not a smoke test / Not a contract test")
    }
}

/**
 * Tests tasks can be configured so that a given percentage of tests are automatically selected as smoke tests.
 */
private fun isAutoSmokeTest(context: ExtensionContext): Boolean {
    if (autoSmokeTestPercentage <= 0) return false
    if(autoSmokeTestPercentage >= 100) return true
    var hashCode = context.testClass.getOrNull()?.name.hashCode()
    hashCode = hashCode * 31 + context.testMethod.getOrNull()?.name.hashCode()
    hashCode = hashCode * 31 + context.uniqueId.hashCode()
    return (hashCode % 100).absoluteValue < autoSmokeTestPercentage
}

private fun isSmokeTest(context: ExtensionContext): Boolean =
    "smoke" in context.tags

private fun contracts(context: ExtensionContext) =
    context.tags.filter { it.startsWith("affectedBy:") }.map { Domain.valueOf(it.substringAfter("affectedBy:")) }.toSet()
