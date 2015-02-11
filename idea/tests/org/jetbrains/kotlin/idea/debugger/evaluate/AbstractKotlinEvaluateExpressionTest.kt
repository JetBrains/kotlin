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

import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.engine.evaluation.expression.EvaluatorBuilderImpl
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.debugger.engine.ContextUtil
import com.intellij.debugger.engine.SuspendContextImpl
import org.jetbrains.kotlin.idea.debugger.*
import org.junit.Assert
import org.jetbrains.kotlin.idea.JetFileType
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.eval4j.Value
import org.jetbrains.eval4j.ObjectValue
import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.psi.JetCodeFragment
import java.util.Collections
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.execution.process.ProcessOutputTypes
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent
import org.apache.log4j.Level
import org.apache.log4j.Logger
import com.intellij.debugger.ui.impl.FrameVariablesTree
import com.intellij.openapi.util.Disposer
import com.intellij.debugger.ui.impl.watch.DebuggerTree
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl
import com.intellij.debugger.ui.impl.watch.DefaultNodeDescriptor
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants
import com.intellij.debugger.ui.tree.LocalVariableDescriptor
import com.intellij.debugger.ui.tree.StackFrameDescriptor
import com.intellij.debugger.ui.tree.StaticDescriptor
import com.intellij.debugger.ui.impl.watch.ThisDescriptorImpl
import com.intellij.debugger.ui.tree.FieldDescriptor
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.PsiManager
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.psi.PsiDocumentManager
import com.intellij.openapi.application.ModalityState
import com.intellij.debugger.settings.NodeRendererSettings
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.SourcePositionProvider
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.debugger.ui.impl.watch.WatchItemDescriptor
import java.util.regex.Matcher
import java.util.regex.Pattern

public abstract class AbstractKotlinEvaluateExpressionTest : KotlinDebuggerTestBase() {
    private val logger = Logger.getLogger(javaClass<KotlinEvaluateExpressionCache>())!!

    private val appender = object : AppenderSkeleton() {
        override fun append(event: LoggingEvent?) {
            println(event?.getRenderedMessage(), ProcessOutputTypes.SYSTEM)
        }
        override fun close() {}
        override fun requiresLayout() = false
    }

    private var oldLogLevel: Level? = null
    private var oldShowFqTypeNames = false

    override fun setUp() {
        super.setUp()

        val classRenderer = NodeRendererSettings.getInstance()!!.getClassRenderer()!!
        oldShowFqTypeNames = classRenderer.SHOW_FQ_TYPE_NAMES
        classRenderer.SHOW_FQ_TYPE_NAMES = true

        oldLogLevel = logger.getLevel()
        logger.setLevel(Level.DEBUG)
        logger.addAppender(appender)
    }

    override fun tearDown() {
        logger.setLevel(oldLogLevel)
        logger.removeAppender(appender)

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
                onBreakpoint { stepInto() }
            }
        }

        onBreakpoint {
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
                onBreakpoint {
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

    private fun createAdditionalBreakpoints(fileText: String) {
        val breakpoints = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// ADDITIONAL_BREAKPOINT: ")
        for (breakpoint in breakpoints) {
            val position = breakpoint.split(".kt:")
            assert(position.size() == 2, "Couldn't parse position from test directive: directive = ${breakpoint}")
            createBreakpoint(position[0], position[1])
        }
    }

    private fun createBreakpoint(fileName: String, lineMarker: String) {
        val project = getProject()!!
        val sourceFiles = FilenameIndex.getAllFilesByExt(project, "kt").filter {
            it.getName().contains(fileName) &&
                    it.contentsToByteArray().toString("UTF-8").contains(lineMarker)
        }

        assert(sourceFiles.size() == 1, "One source file should be found: name = $fileName, sourceFiles = $sourceFiles")

        val runnable = Runnable() {
            val psiSourceFile = PsiManager.getInstance(project).findFile(sourceFiles.first())!!

            val breakpointManager = DebuggerManagerEx.getInstanceEx(project)?.getBreakpointManager()!!
            val document = PsiDocumentManager.getInstance(project).getDocument(psiSourceFile)!!

            val index = psiSourceFile.getText()!!.indexOf(lineMarker)
            val lineNumber = document.getLineNumber(index) + 1

            val breakpoint = breakpointManager.addLineBreakpoint(document, lineNumber)
            if (breakpoint != null) {
                println("LineBreakpoint created at " + psiSourceFile.getName() + ":" + lineNumber, ProcessOutputTypes.SYSTEM);
            }
        }

        DebuggerInvocationUtil.invokeAndWait(project, runnable, ModalityState.defaultModalityState())
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
        return KotlinFrameExtraVariablesProvider().collectVariables(debuggerContext.getSourcePosition(), evaluationContext, hashSetOf())!!
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

            val label = descriptor.getLabel()!!.replaceAll("Package\\$[\\w]*\\$[0-9a-f]+", "Package\\$@packagePartHASH")
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

    private fun SuspendContextImpl.evaluate(text: String, codeFragmentKind: CodeFragmentKind, expectedResult: String) {
        ApplicationManager.getApplication()?.runReadAction {
            val sourcePosition = ContextUtil.getSourcePosition(this)
            val contextElement = ContextUtil.getContextElement(sourcePosition)!!
            Assert.assertTrue("KotlinCodeFragmentFactory should be accepted for context element otherwise default evaluator will be called. ContextElement = ${contextElement.getText()}",
                              KotlinCodeFragmentFactory().isContextAccepted(contextElement))

            try {

                val evaluator =
                        EvaluatorBuilderImpl.build(TextWithImportsImpl(
                                codeFragmentKind,
                                text,
                                JetCodeFragment.getImportsForElement(contextElement),
                                JetFileType.INSTANCE),
                                                   contextElement,
                                                   sourcePosition)


                if (evaluator == null) throw AssertionError("Cannot create an Evaluator for Evaluate Expression")

                val value = evaluator.evaluate(evaluationContext)
                val actualResult = value.asValue().asString()

                Assert.assertTrue("Evaluate expression returns wrong result for $text:\nexpected = $expectedResult\nactual   = $actualResult\n", expectedResult == actualResult)
            }
            catch (e: EvaluateException) {
                Assert.assertTrue("Evaluate expression throws wrong exception for $text:\nexpected = $expectedResult\nactual   = ${e.getMessage()}\n", expectedResult == e.getMessage()?.replaceFirst("id=[0-9]*", "id=ID"))
            }
        }
    }

    private fun Value.asString(): String {
        if (this is ObjectValue && this.value is ObjectReference) {
            val regexMatcher = PACKAGE_PART_PATTERN.matcher(this.toString())
            return regexMatcher.replaceAll("$1@packagePartHASH").replaceFirst("id=[0-9]*", "id=ID")
        }
        return this.toString()
    }
}

private val PACKAGE_PART_PATTERN = Pattern.compile("(Package\\$[\\w]*\\$)([0-9a-f]+)");
