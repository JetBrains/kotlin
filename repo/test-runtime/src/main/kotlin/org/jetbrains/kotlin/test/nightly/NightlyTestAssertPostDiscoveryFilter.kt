/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.nightly

import org.jetbrains.kotlin.testFederation.NightlyTest
import org.jetbrains.kotlin.testFederation.nightlyTagKey
import org.junit.platform.engine.FilterResult
import org.junit.platform.engine.FilterResult.included
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestTag
import org.junit.platform.launcher.LauncherDiscoveryListener
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.PostDiscoveryFilter
import java.util.function.UnaryOperator

private val nightlyTag = TestTag.create(nightlyTagKey)

private val isTeamCityBuild: Boolean =
    System.getProperty("kotlin.build.teamcity")?.toBoolean() ?: (System.getenv("TEAMCITY_VERSION") != null)

class InsufficientNightlyFilteringException(message: String) : IllegalStateException(message)

/**
 * Asserts that the tests tagged as [NightlyTest] are only discovered by the test runs which explicitly include the `nightly` tag,
 * so that they are executed by the dedicated nightly test tasks only, and not by general ones. This is needed to avoid an
 * accidental test duplicate execution on CI (if the tag is not excluded, it will appear in normal test execution).
 */
internal class NightlyTestAssertPostDiscoveryFilter(
    private val isCheckEnabled: Boolean = isTeamCityBuild,
) : PostDiscoveryFilter {
    override fun apply(descriptor: TestDescriptor): FilterResult {
        if (!isCheckEnabled) {
            return included("The check is disabled")
        }
        if (nightlyTag !in descriptor.tags) {
            return included("Not a nightly test")
        }

        val isTestMethod = descriptor.type == TestDescriptor.Type.TEST || descriptor.mayRegisterTests()
        if (!isTestMethod) {
            // It's more important to check tests than containers, skip the check for them
            return included("Containers are checked by their tests")
        }

        val request = NightlyTestDiscoveryListener.currentRequest
            ?: error(
                """
                Cannot check the test "${descriptor.uniqueId}": the discovery request is unknown. 
                Maybe "${NightlyTestDiscoveryListener::class.java.name}" is not registered
                """.trimIndent()
            )

        val requestFilters = request.postDiscoveryFilters
        if (!requestFilters.includes(descriptor)) {
            return included("Not selected by the discovery request")
        }

        if (requestFilters.includes(TestDescriptorWithTags(descriptor, descriptor.tags - nightlyTag))) {
            throw InsufficientNightlyFilteringException(
                """
                The nightly test "${descriptor.uniqueId}" execution without explicit include "$nightlyTagKey" tag.
                * Create a dedicated nightly task which excludes non-nightly tags: `useJUnitPlatform { excludeTags("!$nightlyTagKey") }`
                * Exclude the `nightly` tag from other test tasks: `useJUnitPlatform { excludeTags("$nightlyTagKey") }`
                The nightly task should be added as dependency to `testLifecycleTask`.
                """.trimIndent()
            )
        }

        return included("Explicitly selected by the \"$nightlyTagKey\" tag")
    }
}

/**
 * Provides the current discovery request to the [NightlyTestAssertPostDiscoveryFilter].
 * The launcher applies the post-discovery filters in the thread of the discovery, between the notifications of this listener.
 * Discoveries may be nested (e.g., by the JUnit Platform Suite Engine), so the requests are stacked.
 *
 * Need this get a launcher request and test tag filters.
 */
internal class NightlyTestDiscoveryListener : LauncherDiscoveryListener {
    override fun launcherDiscoveryStarted(request: LauncherDiscoveryRequest) {
        discoveryRequests.get().addLast(request)
    }

    override fun launcherDiscoveryFinished(request: LauncherDiscoveryRequest) {
        val requests = discoveryRequests.get()
        requests.removeLastOrNull()
        if (requests.isEmpty()) discoveryRequests.remove()
    }

    companion object {
        private val discoveryRequests: ThreadLocal<ArrayDeque<LauncherDiscoveryRequest>> = ThreadLocal.withInitial { ArrayDeque() }

        val currentRequest: LauncherDiscoveryRequest?
            get() = discoveryRequests.get().lastOrNull()
    }
}

/** Whether all [PostDiscoveryFilter]s include the [descriptor], as the launcher removes the tests excluded by any of them */
private fun List<PostDiscoveryFilter>.includes(descriptor: TestDescriptor): Boolean =
    all { filter -> filter.apply(descriptor).included() }

private class TestDescriptorWithTags(
    private val delegate: TestDescriptor,
    private val overriddenTags: Set<TestTag>,
) : TestDescriptor by delegate {
    override fun getTags(): Set<TestTag> = overriddenTags

    /* Java default methods are not delegated automatically, while the delegate may override them (e.g., the legacy reporting name) */
    override fun getLegacyReportingName(): String = delegate.legacyReportingName
    override fun getAncestors(): Set<TestDescriptor> = delegate.ancestors
    override fun getDescendants(): Set<TestDescriptor> = delegate.descendants
    override fun orderChildren(orderer: UnaryOperator<List<TestDescriptor>>) = delegate.orderChildren(orderer)
    override fun isRoot(): Boolean = delegate.isRoot
    override fun isContainer(): Boolean = delegate.isContainer
    override fun isTest(): Boolean = delegate.isTest
    override fun mayRegisterTests(): Boolean = delegate.mayRegisterTests()
    override fun prune() = delegate.prune()
    override fun accept(visitor: TestDescriptor.Visitor) = delegate.accept(visitor)
}
