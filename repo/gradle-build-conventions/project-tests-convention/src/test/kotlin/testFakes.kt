/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestFailure
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.api.tasks.testing.source.ClassSource
import org.gradle.api.tasks.testing.source.ClasspathResourceSource
import org.gradle.api.tasks.testing.source.DirectorySource
import org.gradle.api.tasks.testing.source.FilePosition
import org.gradle.api.tasks.testing.source.FileSource
import org.gradle.api.tasks.testing.source.MethodSource
import org.gradle.api.tasks.testing.source.OtherSource
import org.gradle.api.tasks.testing.source.TestSource
import java.io.File

internal fun descriptor(
    name: String,
    className: String? = null,
    displayName: String = name,
    parent: TestDescriptor? = null,
    composite: Boolean = false,
    source: TestSource? = null,
): TestDescriptor = FakeTestDescriptor(name, className, displayName, parent, composite, source)

internal fun result(status: ResultType, startTime: Long, endTime: Long): TestResult =
    FakeTestResult(status, startTime, endTime)

internal fun classSource(className: String): TestSource = ClassSource { className }

internal fun methodSource(className: String, methodName: String): TestSource = object : MethodSource {
    override fun getClassName(): String = className
    override fun getMethodName(): String = methodName
}

internal fun fileSource(file: File, position: FilePosition?): TestSource = object : FileSource {
    override fun getFile(): File = file
    override fun getPosition(): FilePosition? = position
}

internal fun directorySource(directory: File): TestSource = DirectorySource { directory }

internal fun classpathResourceSource(name: String, position: FilePosition?): TestSource = object : ClasspathResourceSource {
    override fun getClasspathResourceName(): String = name
    override fun getPosition(): FilePosition? = position
}

internal fun otherSource(): TestSource = object : OtherSource {}

internal fun position(line: Int, column: Int? = null): FilePosition = object : FilePosition {
    override fun getLine(): Int = line
    override fun getColumn(): Int? = column
}

private class FakeTestDescriptor(
    private val name: String,
    private val className: String?,
    private val displayName: String,
    private val parent: TestDescriptor?,
    private val composite: Boolean,
    private val source: TestSource?,
) : TestDescriptor {
    override fun getName(): String = name
    override fun getClassName(): String? = className
    override fun getDisplayName(): String = displayName
    override fun getParent(): TestDescriptor? = parent
    override fun isComposite(): Boolean = composite
    override fun getSource(): TestSource = source ?: super.getSource()
}

private class FakeTestResult(
    private val status: ResultType,
    private val startTime: Long,
    private val endTime: Long,
) : TestResult {
    override fun getResultType(): ResultType = status
    override fun getStartTime(): Long = startTime
    override fun getEndTime(): Long = endTime
    override fun getTestCount(): Long = 1
    override fun getSuccessfulTestCount(): Long = if (status == ResultType.SUCCESS) 1 else 0
    override fun getFailedTestCount(): Long = if (status == ResultType.FAILURE) 1 else 0
    override fun getSkippedTestCount(): Long = if (status == ResultType.SKIPPED) 1 else 0
    override fun getException(): Throwable? = null
    override fun getExceptions(): List<Throwable> = emptyList()
    override fun getFailures(): List<TestFailure> = emptyList()
    override fun getAssumptionFailure(): TestFailure? = null
}
