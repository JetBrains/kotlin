/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestFailure
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.TestResult.ResultType

/** What Gradle hands a [org.gradle.api.tasks.testing.TestListener], with nothing behind it. */
internal fun descriptor(
    name: String,
    className: String? = null,
    displayName: String = name,
    parent: TestDescriptor? = null,
    composite: Boolean = false,
): TestDescriptor = FakeTestDescriptor(name, className, displayName, parent, composite)

/** The outcome Gradle reports with it; only what a listener reads is answered. */
internal fun result(status: ResultType, durationMillis: Long): TestResult =
    FakeTestResult(status, durationMillis)

private class FakeTestDescriptor(
    private val name: String,
    private val className: String?,
    private val displayName: String,
    private val parent: TestDescriptor?,
    private val composite: Boolean,
) : TestDescriptor {
    override fun getName(): String = name
    override fun getClassName(): String? = className
    override fun getDisplayName(): String = displayName
    override fun getParent(): TestDescriptor? = parent
    override fun isComposite(): Boolean = composite
}

// 'TestFailure' is incubating, and an implementation of 'TestResult' still has to answer for it.
@Suppress("UnstableApiUsage")
private class FakeTestResult(
    private val status: ResultType,
    private val durationMillis: Long,
) : TestResult {
    override fun getResultType(): ResultType = status
    override fun getStartTime(): Long = 0
    override fun getEndTime(): Long = durationMillis
    override fun getTestCount(): Long = 1
    override fun getSuccessfulTestCount(): Long = if (status == ResultType.SUCCESS) 1 else 0
    override fun getFailedTestCount(): Long = if (status == ResultType.FAILURE) 1 else 0
    override fun getSkippedTestCount(): Long = if (status == ResultType.SKIPPED) 1 else 0
    override fun getException(): Throwable? = null
    override fun getExceptions(): List<Throwable> = emptyList()
    override fun getFailures(): List<TestFailure> = emptyList()
    override fun getAssumptionFailure(): TestFailure? = null
}
