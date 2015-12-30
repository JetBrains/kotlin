/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.*
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.engine.evaluation.expression.EvaluatorBuilderImpl
import com.intellij.debugger.impl.DescriptorTestCase
import com.intellij.debugger.settings.NodeRendererSettings
import com.intellij.debugger.ui.impl.watch.*
import com.intellij.debugger.ui.tree.FieldDescriptor
import com.intellij.debugger.ui.tree.LocalVariableDescriptor
import com.intellij.debugger.ui.tree.StackFrameDescriptor
import com.intellij.debugger.ui.tree.StaticDescriptor
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.xdebugger.impl.XDebugSessionImpl
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import com.intellij.xdebugger.impl.frame.XDebugViewSessionListener
import com.intellij.xdebugger.impl.frame.XVariablesView
import com.intellij.xdebugger.impl.frame.XWatchesViewImpl
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree
import com.intellij.xdebugger.impl.ui.tree.nodes.*
import com.sun.jdi.ObjectReference
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.spi.LoggingEvent
import org.jetbrains.eval4j.ObjectValue
import org.jetbrains.eval4j.Value
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerTestBase
import org.jetbrains.kotlin.idea.debugger.KotlinFrameExtraVariablesProvider
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert
import java.io.File
import java.util.*
import javax.swing.tree.TreeNode

abstract class AbstractKotlinEvaluateExpressionTest : KotlinDebuggerTestBase() {
    private val logger = Logger.getLogger(KotlinDebuggerCaches::class.java)!!

    private var appender: AppenderSkeleton? = null

    private var oldLogLevel: Level? = null
    private var oldShowFqTypeNames = false

    override fun setUp() {
        super.setUp()

        val classRenderer = NodeRendererSettings.getInstance()!!.classRenderer!!
        oldShowFqTypeNames = classRenderer.SHOW_FQ_TYPE_NAMES
        classRenderer.SHOW_FQ_TYPE_NAMES = true

        oldLogLevel = logger.level
        logger.level = Level.DEBUG

        appender = object : AppenderSkeleton() {
            override fun append(event: LoggingEvent?) {
                println(event?.renderedMessage, ProcessOutputTypes.SYSTEM)
            }
            override fun close() {}
            override fun requiresLayout() = false
        }

        logger.addAppender(appender)
    }

    override fun tearDown() {
        logger.level = oldLogLevel
        logger.removeAppender(appender)

        appender = null
        oldLogLevel = null

        NodeRendererSettings.getInstance()!!.classRenderer!!.SHOW_FQ_TYPE_NAMES = oldShowFqTypeNames

        super.tearDown()
    }

    fun doSingleBreakpointTest(path: String) {
        val file = File(path)
        val fileText = FileUtil.loadFile(file, true)

        configureSettings(fileText)
        createAdditionalBreakpoints(fileText)

        val shouldPrintFrame = InTextDirectivesUtils.isDirectiveDefined(fileText, "// PRINT_FRAME")

        val expressions = loadTestDirectivesPairs(fileText, "// EXPRESSION: ", "// RESULT: ")

        val blocks = findFilesWithBlocks(file).map { FileUtil.loadFile(it, true) }
        val expectedBlockResults = blocks.map { InTextDirectivesUtils.findLinesWithPrefixesRemoved(it, "// RESULT: ").joinToString("\n") }

        createDebugProcess(path)

        doStepping(path)

        var variablesView: XVariablesView? = null
        var watchesView: XWatchesViewImpl? = null

        ApplicationManager.getApplication().invokeAndWait({
            variablesView = createVariablesView()
            watchesView = createWatchesView()
        }, ModalityState.any())

        doOnBreakpoint {
            val exceptions = linkedMapOf<String, Throwable>()
            try {
                for ((expression, expected) in expressions) {
                    mayThrow(exceptions, expression) {
                        evaluate(expression, CodeFragmentKind.EXPRESSION, expected)
                    }
                }

                for ((i, block) in blocks.withIndex()) {
                    mayThrow(exceptions, block) {
                        evaluate(block, CodeFragmentKind.CODE_BLOCK, expectedBlockResults[i])
                    }
                }
            }
            finally {
                if (shouldPrintFrame) {
                    printFrame(variablesView!!, watchesView!!)
                    println(fileText, ProcessOutputTypes.SYSTEM)
                }
                else {
                    resume(this)
                }
            }

            checkExceptions(exceptions)
        }

        finish()
    }

    fun doMultipleBreakpointsTest(path: String) {
        val file = File(path)
        val fileText = FileUtil.loadFile(file, true)

        createAdditionalBreakpoints(fileText)

        createDebugProcess(path)

        val expressions = loadTestDirectivesPairs(fileText, "// EXPRESSION: ", "// RESULT: ")

        val exceptions = linkedMapOf<String, Throwable>()
        for ((expression, expected) in expressions) {
            mayThrow(exceptions, expression) {
                doOnBreakpoint {
                    try {
                        evaluate(expression, CodeFragmentKind.EXPRESSION, expected)
                    }
                    finally {
                        resume(this)
                    }
                }
            }
        }

        checkExceptions(exceptions)

        finish()
    }

    private fun createWatchesView(): XWatchesViewImpl {
        val session = myDebuggerSession.xDebugSession  as XDebugSessionImpl
        val watchesView = XWatchesViewImpl(session)
        Disposer.register(testRootDisposable, watchesView)
        session.addSessionListener(XDebugViewSessionListener(watchesView), testRootDisposable)
        return watchesView
    }

    private fun createVariablesView(): XVariablesView {
        val session = myDebuggerSession.xDebugSession as XDebugSessionImpl
        val variablesView = XVariablesView(session)
        Disposer.register(testRootDisposable, variablesView)
        session.addSessionListener(XDebugViewSessionListener(variablesView), testRootDisposable)
        return variablesView
    }

    private fun SuspendContextImpl.printFrame(variablesView: XVariablesView, watchesView: XWatchesViewImpl) {
        val tree = variablesView.tree!!
        expandAll(
                tree,
                Runnable {
                    try {
                        Printer().printTree(tree)

                        for (extra in getExtraVars()) {
                            watchesView.addWatchExpression(XExpressionImpl.fromText(extra.text), -1, false);
                        }
                        Printer().printTree(watchesView.tree)
                    }
                    finally {
                        resume(this)
                    }
                },
                hashSetOf(),
                // TODO why this is needed? Otherwise some tests are never ended
                DescriptorTestCase.NodeFilter { it !is XValueNodeImpl || it.name != "cause" },
                this
        )
    }

    fun getExtraVars(): Set<TextWithImports> {
        return KotlinFrameExtraVariablesProvider().collectVariables(debuggerContext.sourcePosition, evaluationContext, hashSetOf())
    }

    private inner class Printer() {
        fun printTree(tree: XDebuggerTree) {
            val root = tree.treeModel.root as TreeNode
            printNode(root, 0)
        }

       private fun printNode(node: TreeNode, indent: Int) {
            val descriptor = when {
                node is DebuggerTreeNodeImpl -> node.descriptor
                node is XValueNodeImpl -> (node.valueContainer as? JavaValue)?.descriptor ?: MessageDescriptor(node.text.toString())
                node is XStackFrameNode -> (node.valueContainer as? JavaStackFrame)?.descriptor
                node is XValueGroupNodeImpl -> (node.valueContainer as? JavaStaticGroup)?.descriptor
                node is WatchesRootNode -> null
                node is WatchMessageNode -> WatchItemDescriptor(project, TextWithImportsImpl(CodeFragmentKind.EXPRESSION, node.expression.expression))
                node is MessageTreeNode -> MessageDescriptor(node.text.toString())
                else -> MessageDescriptor(node.toString())
            }

            if (descriptor != null && printDescriptor(descriptor, indent)) return

            printChildren(node, indent + 2)
        }

        fun printDescriptor(descriptor: NodeDescriptorImpl, indent: Int): Boolean {
            if (descriptor is DefaultNodeDescriptor) return true

            var label = descriptor.label
            // TODO: update presentation before calc label
            if (label == NodeDescriptorImpl.UNKNOWN_VALUE_MESSAGE && descriptor is StaticDescriptor) {
                label = "static = " + NodeRendererSettings.getInstance().classRenderer.renderTypeName(descriptor.type.name())
            }
            if (label.endsWith(XDebuggerUIConstants.COLLECTING_DATA_MESSAGE)) return true

            val curIndent = " ".repeat(indent)
            when (descriptor) {
                is StackFrameDescriptor ->    logDescriptor(descriptor, "$curIndent frame    = $label\n")
                is WatchItemDescriptor ->     logDescriptor(descriptor, "$curIndent extra    = ${descriptor.calcValueName()}\n")
                is LocalVariableDescriptor -> logDescriptor(descriptor, "$curIndent local    = $label"
                                                    + " (sp = ${render(SourcePositionProvider.getSourcePosition(descriptor, myProject, debuggerContext))})\n")
                is StaticDescriptor ->        logDescriptor(descriptor, "$curIndent static   = $label\n")
                is ThisDescriptorImpl ->      logDescriptor(descriptor, "$curIndent this     = $label\n")
                is FieldDescriptor ->         logDescriptor(descriptor, "$curIndent field    = $label"
                                                    + " (sp = ${render(SourcePositionProvider.getSourcePosition(descriptor, myProject, debuggerContext))})\n")
                is MessageDescriptor ->       logDescriptor(descriptor, "$curIndent          - $label\n")
                else ->                       logDescriptor(descriptor, "$curIndent unknown  = $label\n")
            }
            return false
        }

        private fun printChildren(node: TreeNode, indent: Int) {
            val e = node.children()
            while (e.hasMoreElements()) {
                printNode(e.nextElement() as TreeNode, indent)
            }
        }

        private fun render(sp: SourcePosition?): String {
            return renderSourcePosition(sp).replace(":", ", ")
        }
    }

    private fun checkExceptions(exceptions: MutableMap<String, Throwable>) {
        if (!exceptions.isEmpty()) {
            for (exc in exceptions.values) {
                exc.printStackTrace()
            }
            throw AssertionError("Test failed:\n" + exceptions.map { "expression: ${it.key}, exception: ${it.value.message}" }.joinToString("\n"))
        }
    }

    private fun mayThrow(map: MutableMap<String, Throwable>, expression: String, f: () -> Unit) {
        try {
            f()
        }
        catch (e: Throwable) {
            map.put(expression, e)
        }
    }

    private fun loadTestDirectivesPairs(fileContent: String, directivePrefix: String, expectedPrefix: String): List<Pair<String, String>> {
        val directives = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileContent, directivePrefix)
        val expected = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileContent, expectedPrefix)
        assert(directives.size == expected.size) { "Sizes of test directives are different" }
        return directives.zip(expected)
    }

    private fun findFilesWithBlocks(mainFile: File): List<File> {
        val mainFileName = mainFile.name
        return mainFile.parentFile?.listFiles()?.filter { it.name.startsWith(mainFileName) && it.name != mainFileName } ?: Collections.emptyList()
    }

    private fun createContextElement(context: SuspendContextImpl): PsiElement {
        val contextElement = ContextUtil.getContextElement(debuggerContext)!!
        Assert.assertTrue("KotlinCodeFragmentFactory should be accepted for context element otherwise default evaluator will be called. ContextElement = ${contextElement.text}",
                          KotlinCodeFragmentFactory().isContextAccepted(contextElement))

        val labelsAsText = InTextDirectivesUtils.findLinesWithPrefixesRemoved(contextElement.containingFile.text, "// DEBUG_LABEL: ")
        if (labelsAsText.isEmpty()) return contextElement

        val markupMap = hashMapOf<com.sun.jdi.Value, ValueMarkup>()
        for (labelAsText in labelsAsText) {
            val labelParts = labelAsText.split("=")
            assert(labelParts.size == 2) { "Wrong format for DEBUG_LABEL directive: // DEBUG_LABEL: {localVariableName} = {labelText}"}
            val localVariableName = labelParts[0].trim()
            val labelName = labelParts[1].trim()
            val localVariable = context.frameProxy!!.visibleVariableByName(localVariableName)
            assert(localVariable != null) { "Couldn't find localVariable for label: name = $localVariableName" }
            val localVariableValue = context.frameProxy!!.getValue(localVariable)
            assert(localVariableValue != null) { "Local variable $localVariableName should be an ObjectReference" }
            localVariableValue!!
            markupMap.put(localVariableValue, ValueMarkup(labelName, null, labelName))
        }

        val (text, labels) = KotlinCodeFragmentFactory.createCodeFragmentForLabeledObjects(contextElement.project, markupMap)
        return KotlinCodeFragmentFactory().createWrappingContext(text, labels, KotlinCodeFragmentFactory.getContextElement(contextElement), project)!!
    }

    private fun SuspendContextImpl.evaluate(text: String, codeFragmentKind: CodeFragmentKind, expectedResult: String) {
        runReadAction {
            val sourcePosition = ContextUtil.getSourcePosition(this)
            val contextElement = createContextElement(this)

            contextElement.putCopyableUserData(KotlinCodeFragmentFactory.DEBUG_FRAME_FOR_TESTS, this@AbstractKotlinEvaluateExpressionTest.evaluationContext.frameProxy)

            try {

                val evaluator =
                        EvaluatorBuilderImpl.build(TextWithImportsImpl(codeFragmentKind, text, "", KotlinFileType.INSTANCE),
                                                   contextElement,
                                                   sourcePosition,
                                                   project)


                if (evaluator == null) throw AssertionError("Cannot create an Evaluator for Evaluate Expression")

                val value = evaluator.evaluate(this@AbstractKotlinEvaluateExpressionTest.evaluationContext)
                val actualResult = value.asValue().asString()

                Assert.assertTrue("Evaluate expression returns wrong result for $text:\nexpected = $expectedResult\nactual   = $actualResult\n", expectedResult == actualResult)
            }
            catch (e: EvaluateException) {
                Assert.assertTrue("Evaluate expression throws wrong exception for $text:\nexpected = $expectedResult\nactual   = ${e.message}\n", expectedResult == e.message?.replaceFirst(ID_PART_REGEX, "id=ID"))
            }
        }
    }

    private fun Value.asString(): String {
        if (this is ObjectValue && this.value is ObjectReference) {
            return this.toString().replaceFirst(ID_PART_REGEX, "id=ID")
        }
        return this.toString()
    }
}

private val ID_PART_REGEX = "id=[0-9]*".toRegex()