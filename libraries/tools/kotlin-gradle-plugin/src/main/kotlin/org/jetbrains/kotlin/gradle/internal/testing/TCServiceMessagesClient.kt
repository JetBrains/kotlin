/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.testing

import jetbrains.buildServer.messages.serviceMessages.*
import org.gradle.api.internal.tasks.testing.*
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestOutputEvent.Destination.StdErr
import org.gradle.api.tasks.testing.TestOutputEvent.Destination.StdOut
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.TestResult.ResultType.*
import org.gradle.internal.operations.OperationIdentifier
import org.gradle.process.internal.ExecHandle
import org.jetbrains.kotlin.gradle.internal.LogType
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.testing.KotlinTestFailure
import org.slf4j.Logger
import java.text.ParseException

data class TCServiceMessagesClientSettings(
    val rootNodeName: String,
    val testNameSuffix: String? = null,
    val prependSuiteName: Boolean = false,
    val treatFailedTestOutputAsStacktrace: Boolean = false,
    val stackTraceParser: (String) -> ParsedStackTrace? = { null },
    val ignoreOutOfRootNodes: Boolean = false,
    val ignoreLineEndingAfterMessage: Boolean = true,
    val escapeTCMessagesInLog: Boolean = false
)

internal open class TCServiceMessagesClient(
    private val results: TestResultProcessor,
    val settings: TCServiceMessagesClientSettings,
    val log: Logger
) : ServiceMessageParserCallback {
    lateinit var rootOperationId: OperationIdentifier
    var afterMessage = false

    inline fun root(operation: OperationIdentifier, actions: () -> Unit) {
        rootOperationId = operation

        val tsStart = System.currentTimeMillis()
        val root = RootNode(operation)
        open(tsStart, root)
        actions()
        ensureNodesClosed(root)
    }

    override fun parseException(e: ParseException, text: String) {
        log.error("Failed to parse test process messages: \"$text\"", e)
    }

    internal open fun testFailedMessage(execHandle: ExecHandle, exitValue: Int): String =
        "$execHandle exited with errors (exit code: $exitValue)"

    override fun serviceMessage(message: ServiceMessage) {

        // If a user uses TeamCity, this log may be treated by TC as an actual service message.
        // So, escape logged messages if the corresponding setting is specified.
        log.kotlinDebug {
            val messageString = if (settings.escapeTCMessagesInLog) {
                message.toString().replaceFirst("^##teamcity\\[".toRegex(), "##TC[")
            } else {
                message.toString()
            }
            "TCSM: $messageString"
        }

        when (message) {
            is TestSuiteStarted -> open(message.ts, SuiteNode(requireLeafGroup(), getSuiteName(message)))
            is TestStarted -> beginTest(message.ts, message.testName)
            is TestStdOut -> requireLeafTest().output(StdOut, message.stdOut)
            is TestStdErr -> requireLeafTest().output(StdErr, message.stdErr)
            is TestFailed -> requireLeafTest().failure(message)
            is TestFinished -> endTest(message.ts, message.testName)
            is TestIgnored -> {
                if (message.attributes["suite"] == "true") {
                    // non standard property for dealing with ignored test suites without visiting all inner tests
                    SuiteNode(requireLeafGroup(), message.testName).open(message.ts) { message.ts }
                } else {
                    beginTest(message.ts, message.testName, isIgnored = true)
                    endTest(message.ts, message.testName)
                }
            }
            is TestSuiteFinished -> close(message.ts, getSuiteName(message))
            is Message -> printNonTestOutput(message.text, LogType.byValueOrNull(message.attributes["type"]))
            else -> Unit
        }

        afterMessage = true
    }

    protected open fun getSuiteName(message: BaseTestSuiteMessage) = message.suiteName

    override fun regularText(text: String) {
        val actualText = if (afterMessage && settings.ignoreLineEndingAfterMessage)
            when {
                text.startsWith("\r\n") -> text.removePrefix("\r\n")
                else -> text.removePrefix("\n")
            }
        else text

        if (actualText.isNotEmpty()) {
            log.kotlinDebug { "TCSM stdout captured: $actualText" }

            val test = leaf as? TestNode
            if (test != null) {
                test.output(StdOut, actualText)
            } else {
                printNonTestOutput(actualText)
            }
        }
        afterMessage = false
    }

    protected open fun printNonTestOutput(text: String, type: LogType? = null) {
        print(text)
    }

    protected open fun processStackTrace(stackTrace: String): String =
        stackTrace

    protected open val testNameSuffix: String?
        get() = settings.testNameSuffix

    private fun beginTest(ts: Long, testName: String, isIgnored: Boolean = false) {
        val parent = requireLeafGroup()
        parent.requireReportingNode()

        val finalTestName = testName.let {
            if (settings.prependSuiteName) "${parent.fullNameWithoutRoot}.$it"
            else it
        }

        val parsedName = ParsedTestName(finalTestName, parent.localId)
        val fullTestName = if (testNameSuffix == null) parsedName.methodName
        else "${parsedName.methodName}[$testNameSuffix]"

        open(
            ts, TestNode(
                parent, parsedName.className, parsedName.classDisplayName, parsedName.methodName,
                displayName = fullTestName,
                localId = testName,
                ignored = isIgnored
            )
        )
    }

    private fun endTest(ts: Long, testName: String) {
        close(ts, testName)
    }

    private fun TestNode.failure(
        message: TestFailed
    ) {
        hasFailures = true

        val stacktrace = buildString {
            if (message.stacktrace != null) {
                append(message.stacktrace)
            }

            if (settings.treatFailedTestOutputAsStacktrace) {
                append(stackTraceOutput)
                stackTraceOutput.setLength(0)
            }
        }.let { processStackTrace(it) }

        val parsedStackTrace = settings.stackTraceParser(stacktrace)

        val failMessage = parsedStackTrace?.message ?: message.failureMessage
        results.failure(
            descriptor.id,
            KotlinTestFailure(
                failMessage?.let { extractExceptionClassName(it) }
                    ?: "Unknown",
                failMessage,
                stacktrace,
                patchStackTrace(this, parsedStackTrace?.stackTrace),
                message.expected,
                message.actual
            )
        )
    }

    private fun extractExceptionClassName(message: String): String =
        message.substringBefore(':').trim()

    /**
     * Required for org.gradle.api.internal.tasks.testing.logging.ShortExceptionFormatter.printException
     * In JS Stacktraces we have short class name, while filter using FQN
     * So, let replace short class name with FQN for current test
     */
    private fun patchStackTrace(node: TestNode, stackTrace: List<StackTraceElement>?): List<StackTraceElement>? =
        stackTrace?.map {
            if (it.className == node.classDisplayName) StackTraceElement(
                node.className,
                it.methodName,
                it.fileName,
                it.lineNumber
            ) else it
        }

    private fun TestNode.output(
        destination: TestOutputEvent.Destination,
        text: String
    ) {
        allOutput.append(text)
        if (settings.treatFailedTestOutputAsStacktrace) {
            stackTraceOutput.append(text)
        } else {
            results.output(descriptor.id, DefaultTestOutputEvent(destination, text))
        }
    }

    private inline fun <NodeType : Node> NodeType.open(contents: (NodeType) -> Unit) = open(System.currentTimeMillis()) {
        contents(it)
        System.currentTimeMillis()
    }

    private inline fun <NodeType : Node> NodeType.open(tsStart: Long, contents: (NodeType) -> Long) {
        val child = open(tsStart, this@open)
        val tsEnd = contents(child)
        assert(close(tsEnd, child.localId) === child)
    }

    private fun <NodeType : Node> open(ts: Long, new: NodeType): NodeType = new.also {
        log.kotlinDebug { "Test node opened: $it" }

        it.markStarted(ts)
        push(it)
    }

    private fun close(ts: Long, assertLocalId: String?) = pop().also {
        if (assertLocalId != null) {
            if (it.localId != assertLocalId && settings.ignoreOutOfRootNodes && it.parent == null) {
                push(it)
                return it
            }

            check(it.localId == assertLocalId) {
                "Bad TCSM: unexpected node to close `$assertLocalId`, expected `${it.localId}`, stack: ${
                leaf.collectParents().joinToString("") { item -> "\n - ${item.localId}" }
                }\n"
            }
        }

        log.kotlinDebug { "Test node closed: $it" }
        it.markCompleted(ts)
    }

    private fun Node?.collectParents(): MutableList<Node> {
        var i = this
        val items = mutableListOf<Node>()
        while (i != null) {
            items.add(i)
            i = i.parent
        }
        return items
    }


    class ParsedTestName(testName: String, parentName: String) {
        val hasClassName: Boolean
        val className: String
        val classDisplayName: String
        val methodName: String

        init {
            val methodNameCut = testName.lastIndexOf('.')
            hasClassName = methodNameCut != -1

            if (hasClassName) {
                className = testName.substring(0, methodNameCut)
                classDisplayName = className.substringAfterLast('.')
                methodName = testName.substring(methodNameCut + 1)
            } else {
                className = parentName
                classDisplayName = parentName
                methodName = testName
            }
        }
    }

    enum class NodeState {
        created, started, completed
    }

    /**
     * Node of tests tree.
     *
     */
    abstract inner class Node(
        var parent: Node? = null,
        val localId: String
    ) {
        val id: String = if (parent != null) "${parent!!.id}/$localId" else localId

        open val cleanName: String
            get() = localId

        abstract val descriptor: TestDescriptorInternal?

        var state: NodeState = NodeState.created

        var reportingParent: GroupNode? = null
            get() {
                checkReportingNodeCreated()
                return field
            }

        private fun checkReportingNodeCreated() {
            check(descriptor != null)
        }

        var hasFailures: Boolean = false
            set(value) {
                // traverse parents only on first failure
                if (!field) {
                    field = value
                    parent?.hasFailures = true
                }
            }

        /**
         * If all tests in group are ignored, then group marked as skipped.
         * This is workaround for absence of ignored test suite flag in TC service messages protocol.
         */
        var containsNotIgnored: Boolean = false
            set(value) {
                // traverse parents only on first test
                if (!field) {
                    field = value
                    parent?.containsNotIgnored = true
                }
            }

        val resultType: TestResult.ResultType
            get() = when {
                containsNotIgnored -> when {
                    hasFailures -> FAILURE
                    else -> SUCCESS
                }
                else -> SKIPPED
            }

        override fun toString(): String = id

        abstract fun markStarted(ts: Long)
        abstract fun markCompleted(ts: Long)

        fun checkState(state: NodeState) {
            check(this.state == state) {
                "$this should be in state $state"
            }
        }

        protected fun reportStarted(ts: Long) {
            checkState(NodeState.created)
            reportingParent?.checkState(NodeState.started)

            results.started(descriptor!!, TestStartEvent(ts, descriptor!!.parent?.id))

            state = NodeState.started
        }

        protected fun reportCompleted(ts: Long) {
            checkState(NodeState.started)
            reportingParent?.checkState(NodeState.started)

            results.completed(descriptor!!.id, TestCompleteEvent(ts, resultType))

            state = NodeState.completed
        }
    }

    abstract inner class GroupNode(parent: Node?, localId: String) : Node(parent, localId) {
        val fullNameWithoutRoot: String
            get() = collectParents().dropLast(1)
                .reversed()
                .map { it.localId }
                .filter { it.isNotBlank() }
                .joinToString(".") { it }

        abstract fun requireReportingNode(): TestDescriptorInternal
    }

    inner class RootNode(val ownerBuildOperationId: OperationIdentifier) : GroupNode(null, settings.rootNodeName) {
        override val descriptor: TestDescriptorInternal = object : DefaultTestSuiteDescriptor(settings.rootNodeName, localId) {
            override fun getOwnerBuildOperationId(): Any? = this@RootNode.ownerBuildOperationId
            override fun getParent(): TestDescriptorInternal? = null
            override fun toString(): String = name
        }

        override fun requireReportingNode(): TestDescriptorInternal = descriptor

        override fun markStarted(ts: Long) {
            reportStarted(ts)
        }

        override fun markCompleted(ts: Long) {
            reportCompleted(ts)
        }
    }

    fun cleanName(parent: GroupNode, name: String): String {
        // Some test reporters may report test suite in name (Kotlin/Native)
        val parentName = parent.fullNameWithoutRoot
        return name.removePrefix("$parentName.")
    }

    inner class SuiteNode(parent: GroupNode, name: String) : GroupNode(parent, name) {
        override val cleanName = cleanName(parent, name)

        private var shouldReportComplete = false

        override var descriptor: TestDescriptorInternal? = null
            private set

        override fun requireReportingNode(): TestDescriptorInternal = descriptor ?: createReportingNode()

        /**
         * Called when first test in suite started
         */
        private fun createReportingNode(): TestDescriptorInternal {
            val parents = collectParents()
            val fullName = parents.reversed()
                .map { it.cleanName }
                .filter { it.isNotBlank() }
                .joinToString(".")

            val reportingParent = parents.last() as RootNode
            this.reportingParent = reportingParent

            descriptor = object : DefaultTestSuiteDescriptor(id, fullName) {
                override fun getDisplayName(): String = fullNameWithoutRoot
                override fun getClassName(): String? = fullNameWithoutRoot
                override fun getOwnerBuildOperationId(): Any? = rootOperationId
                override fun getParent(): TestDescriptorInternal = reportingParent.descriptor
                override fun toString(): String = displayName
            }

            shouldReportComplete = true

            check(startedTs != 0L)
            reportStarted(startedTs)

            return descriptor!!
        }

        private var startedTs: Long = 0

        override fun markStarted(ts: Long) {
            check(descriptor == null)
            startedTs = ts
        }

        override fun markCompleted(ts: Long) {
            if (shouldReportComplete) {
                check(descriptor != null)
                reportCompleted(ts)
            }
        }
    }

    inner class TestNode(
        parent: GroupNode,
        val className: String,
        val classDisplayName: String,
        methodName: String,
        displayName: String,
        localId: String,
        ignored: Boolean = false
    ) : Node(parent, localId) {
        val stackTraceOutput by lazy { StringBuilder() }
        val allOutput by lazy { StringBuilder() }

        private val parentDescriptor = (this@TestNode.parent as GroupNode).requireReportingNode()

        override val descriptor: TestDescriptorInternal =
            object : DefaultTestDescriptor(id, className, methodName, classDisplayName, displayName) {
                override fun getOwnerBuildOperationId(): Any? = rootOperationId
                override fun getParent(): TestDescriptorInternal = parentDescriptor
            }

        override fun markStarted(ts: Long) {
            reportStarted(ts)
        }

        override fun markCompleted(ts: Long) {
            stackTraceOutput.setLength(0)
            allOutput.setLength(0)
            reportCompleted(ts)
        }

        init {
            if (!ignored) containsNotIgnored = true
        }
    }

    private var leaf: Node? = null

    private val ServiceMessage.ts: Long
        get() = creationTimestamp?.timestamp?.time ?: System.currentTimeMillis()

    private fun push(node: Node) = node.also { leaf = node }
    private fun pop() = leaf!!.also { leaf = it.parent }

    fun ensureNodesClosed(root: RootNode? = null, cause: Throwable? = null, throwError: Boolean = true): Error? {
        val ts = System.currentTimeMillis()

        when (leaf) {
            null -> return null
            root -> close(ts, leaf!!.localId)
            else -> {
                val output = StringBuilder()
                var currentTest: TestNode? = null

                while (leaf != null) {
                    val currentLeaf = leaf!!

                    if (currentLeaf is TestNode) {
                        currentTest = currentLeaf
                        output.append(currentLeaf.allOutput)
                        currentLeaf.failure(TestFailed(currentLeaf.cleanName, null as Throwable?))
                    }

                    close(ts, currentLeaf.localId)
                }

                @Suppress("ThrowableNotThrown")
                val error = Error(
                    buildString {
                        append("Test running process exited unexpectedly.\n")
                        if (currentTest != null) {
                            append("Current test: ${currentTest.cleanName}\n")
                        }
                        if (output.toString().isNotBlank()) {
                            append("Process output:\n $output")
                        }
                    },
                    cause
                )

                if (throwError) {
                    throw error
                } else {
                    return error
                }
            }
        }

        return null
    }

    private fun requireLeaf() = leaf ?: error("test out of group")
    private fun requireLeafGroup(): GroupNode = requireLeaf().let {
        it as? GroupNode ?: error("previous test `$it` not finished")
    }

    private fun requireLeafTest() = leaf as? TestNode
        ?: error("no running test")
}