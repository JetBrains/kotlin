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
import com.intellij.debugger.engine.ContextUtil
import com.intellij.debugger.engine.SourcePositionProvider
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.engine.evaluation.expression.EvaluatorBuilderImpl
import com.intellij.debugger.settings.NodeRendererSettings
import com.intellij.debugger.ui.impl.FrameVariablesTree
import com.intellij.debugger.ui.impl.watch.*
import com.intellij.debugger.ui.tree.FieldDescriptor
import com.intellij.debugger.ui.tree.LocalVariableDescriptor
import com.intellij.debugger.ui.tree.StackFrameDescriptor
import com.intellij.debugger.ui.tree.StaticDescriptor
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup
import com.sun.jdi.ObjectReference
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.spi.LoggingEvent
import org.jetbrains.eval4j.ObjectValue
import org.jetbrains.eval4j.Value
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerTestBase
import org.jetbrains.kotlin.idea.debugger.KotlinFrameExtraVariablesProvider
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert
import java.io.File
import java.util.Collections
import kotlin.test.fail

public abstract class AbstractKotlinEvaluateExpressionTest : KotlinDebuggerTestBase() {
    private val logger = Logger.getLogger(javaClass<KotlinEvaluateExpressionCache>())!!

    private var appender: AppenderSkeleton? = null

    private var oldLogLevel: Level? = null
    private var oldShowFqTypeNames = false

    override fun setUp() {
        super.setUp()

        val classRenderer = NodeRendererSettings.getInstance()!!.getClassRenderer()!!
        oldShowFqTypeNames = classRenderer.SHOW_FQ_TYPE_NAMES
        classRenderer.SHOW_FQ_TYPE_NAMES = true

        oldLogLevel = logger.getLevel()
        logger.setLevel(Level.DEBUG)

        appender = object : AppenderSkeleton() {
            override fun append(event: LoggingEvent?) {
                println(event?.getRenderedMessage(), ProcessOutputTypes.SYSTEM)
            }
            override fun close() {}
            override fun requiresLayout() = false
        }

        logger.addAppender(appender)
    }

    override fun tearDown() {
        logger.setLevel(oldLogLevel)
        logger.removeAppender(appender)

        appender = null
        oldLogLevel = null

        NodeRendererSettings.getInstance()!!.getClassRenderer()!!.SHOW_FQ_TYPE_NAMES = oldShowFqTypeNames

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

        val count = InTextDirectivesUtils.getPrefixedInt(fileText, "// STEP_INTO: ") ?: 0
        if (count > 0) {
            for (i in 1..count) {
                doOnBreakpoint { this@AbstractKotlinEvaluateExpressionTest.stepInto(this) }
            }
        }

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
                    printFrame()
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

    private fun SuspendContextImpl.printFrame() {
        val tree = FrameVariablesTree(getProject()!!)
        Disposer.register(getTestRootDisposable()!!, tree);

        invokeRatherLater(this) {
            tree.rebuild(debuggerContext)
            expandAll(tree, Runnable {
                try {
                    val printer = Printer()
                    printer.printTree(tree)
                    for (extra in getExtraVars()) {
                        printer.printDescriptor(tree.getNodeFactory().getWatchItemDescriptor(null, extra, null), 2)
                    }
                }
                finally {
                    resume(this@printFrame)
                }
            })
        }
    }

    fun getExtraVars(): Set<TextWithImports> {
        return KotlinFrameExtraVariablesProvider().collectVariables(debuggerContext!!.getSourcePosition(), evaluationContext, hashSetOf())!!
    }

    private inner class Printer() {
        fun printTree(tree: DebuggerTree) {
            val root = tree.getMutableModel()!!.getRoot() as DebuggerTreeNodeImpl
            printNode(root, 0)
        }

        private fun printNode(node: DebuggerTreeNodeImpl, indent: Int) {
            val descriptor: NodeDescriptorImpl = node.getDescriptor()!!

            if (printDescriptor(descriptor, indent)) return

            printChildren(node, indent + 2)
        }

        fun printDescriptor(descriptor: NodeDescriptorImpl, indent: Int): Boolean {
            if (descriptor is DefaultNodeDescriptor) return true

            val label = descriptor.getLabel()!!.replace("Package\\$[\\w]*\\$[0-9a-f]+".toRegex(), "Package\\$@packagePartHASH")
            if (label.endsWith(XDebuggerUIConstants.COLLECTING_DATA_MESSAGE)) return true

            val curIndent = " ".repeat(indent)
            when (descriptor) {
                is StackFrameDescriptor ->    logDescriptor(descriptor, "$curIndent frame    = $label\n")
                is WatchItemDescriptor ->     logDescriptor(descriptor, "$curIndent extra    = ${descriptor.calcValueName()}\n")
                is LocalVariableDescriptor -> logDescriptor(descriptor, "$curIndent local    = $label"
                                                    + " (sp = ${render(SourcePositionProvider.getSourcePosition(descriptor, myProject, debuggerContext!!))})\n")
                is StaticDescriptor ->        logDescriptor(descriptor, "$curIndent static   = $label\n")
                is ThisDescriptorImpl ->      logDescriptor(descriptor, "$curIndent this     = $label\n")
                is FieldDescriptor ->         logDescriptor(descriptor, "$curIndent field    = $label"
                                                    + " (sp = ${render(SourcePositionProvider.getSourcePosition(descriptor, myProject, debuggerContext!!))})\n")
                else ->                       logDescriptor(descriptor, "$curIndent unknown  = $label\n")
            }
            return false
        }

        private fun printChildren(node: DebuggerTreeNodeImpl, indent: Int) {
            val e = node.rawChildren()!!
            while (e.hasMoreElements()) {
                printNode(e.nextElement() as DebuggerTreeNodeImpl, indent)
            }
        }

        private fun render(sp: SourcePosition?): String {
            return renderSourcePosition(sp).replace(":", ", ")
        }
    }

    private fun checkExceptions(exceptions: MutableMap<String, Throwable>) {
        if (!exceptions.isEmpty()) {
            for (exc in exceptions.values()) {
                exc.printStackTrace()
            }
            throw AssertionError("Test failed:\n" + exceptions.map { "expression: ${it.key}, exception: ${it.value.getMessage()}" }.joinToString("\n"))
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
        assert(directives.size() == expected.size(), "Sizes of test directives are different")
        return directives.zip(expected)
    }

    private fun findFilesWithBlocks(mainFile: File): List<File> {
        val mainFileName = mainFile.getName()
        return mainFile.getParentFile()?.listFiles()?.filter { it.name.startsWith(mainFileName) && it.name != mainFileName } ?: Collections.emptyList()
    }

    private fun createContextElement(context: SuspendContextImpl): PsiElement {
        val contextElement = ContextUtil.getContextElement(debuggerContext)
        Assert.assertTrue("KotlinCodeFragmentFactory should be accepted for context element otherwise default evaluator will be called. ContextElement = ${contextElement?.getText() ?: "null"}",
                          KotlinCodeFragmentFactory().isContextAccepted(contextElement))

        if (contextElement != null) {
            val labelsAsText = InTextDirectivesUtils.findLinesWithPrefixesRemoved(contextElement.getContainingFile().getText(), "// DEBUG_LABEL: ")
            if (labelsAsText.isEmpty()) return contextElement

            val markupMap = hashMapOf<ObjectReference, ValueMarkup>()
            for (labelAsText in labelsAsText) {
                val labelParts = labelAsText.splitBy("=")
                assert(labelParts.size() == 2) { "Wrong format for DEBUG_LABEL directive: // DEBUG_LABEL: {localVariableName} = {labelText}"}
                val localVariableName = labelParts[0].trim()
                val labelName = labelParts[1].trim()
                val localVariable = context.getFrameProxy()!!.visibleVariableByName(localVariableName)
                assert(localVariable != null) { "Couldn't find localVariable for label: name = $localVariableName" }
                val localVariableValue = context.getFrameProxy()!!.getValue(localVariable) as? ObjectReference
                assert(localVariableValue != null) { "Local variable $localVariableName should be an ObjectReference" }
                localVariableValue!!
                markupMap.put(localVariableValue, ValueMarkup(labelName, null, labelName))
            }

            val (text, labels) = KotlinCodeFragmentFactory.createCodeFragmentForLabeledObjects(markupMap)
            return KotlinCodeFragmentFactory().createWrappingContext(text, labels, KotlinCodeFragmentFactory.getContextElement(contextElement), getProject())!!
        }

        return contextElement!!
    }

    private fun SuspendContextImpl.evaluate(text: String, codeFragmentKind: CodeFragmentKind, expectedResult: String) {
        runReadAction {
            val sourcePosition = ContextUtil.getSourcePosition(this)
            val contextElement = createContextElement(this)

            try {

                val evaluator =
                        EvaluatorBuilderImpl.build(TextWithImportsImpl(codeFragmentKind, text, "", JetFileType.INSTANCE),
                                                   contextElement,
                                                   sourcePosition)


                if (evaluator == null) throw AssertionError("Cannot create an Evaluator for Evaluate Expression")

                val value = evaluator.evaluate(evaluationContext)
                val actualResult = value.asValue().asString()

                Assert.assertTrue("Evaluate expression returns wrong result for $text:\nexpected = $expectedResult\nactual   = $actualResult\n", expectedResult == actualResult)
            }
            catch (e: EvaluateException) {
                Assert.assertTrue("Evaluate expression throws wrong exception for $text:\nexpected = $expectedResult\nactual   = ${e.getMessage()}\n", expectedResult == e.getMessage()?.replaceFirst(ID_PART_REGEX, "id=ID"))
            }
        }
    }

    private fun Value.asString(): String {
        if (this is ObjectValue && this.value is ObjectReference) {
            return this.toString().replace(PACKAGE_PART_REGEX, "$1@packagePartHASH").replaceFirst(ID_PART_REGEX, "id=ID")
        }
        return this.toString()
    }
}

private val PACKAGE_PART_REGEX = "(Package\\$[\\w]*\\$)([0-9a-f]+)".toRegex()
private val ID_PART_REGEX = "id=[0-9]*".toRegex()