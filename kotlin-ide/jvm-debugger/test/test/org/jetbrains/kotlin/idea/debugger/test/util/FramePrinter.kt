package org.jetbrains.kotlin.idea.debugger.test.util

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SourcePositionProvider
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.ui.impl.watch.*
import com.intellij.debugger.ui.tree.*
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.concurrency.Semaphore
import com.intellij.xdebugger.XDebuggerTestUtil
import com.intellij.xdebugger.XTestValueNode
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants
import org.jetbrains.kotlin.idea.debugger.coroutine.data.ContinuationVariableValueDescriptorImpl
import org.jetbrains.kotlin.idea.debugger.invokeInManagerThread
import org.jetbrains.kotlin.idea.debugger.test.KOTLIN_LIBRARY_NAME
import org.jetbrains.kotlin.psi.KtFile
import java.lang.Appendable
import java.util.concurrent.TimeUnit

class FramePrinter(private val debugProcess: DebugProcessImpl, private val stackFrame: StackFrameProxyImpl) {
    fun print(frame: XStackFrame): String {
        return buildString { append(frame, 0) }
    }

    private fun Appendable.append(container: XValueContainer, indent: Int = 0) {
        appendIndent(indent)

        append(container.javaClass.simpleName)

        val info = computeInfo(container)

        info.kind?.let { append("[$it]") }
        info.name?.let { append(" $it") }
        info.type?.let { append(": $it") }
        info.value?.let { append(" = $it") }
        info.sourcePosition?.let { append(" (" + it.render() + ")") }
        val mightHaveChildren = info.mightHaveChildren

        appendLine()

        if (mightHaveChildren) {
            for (child in collectChildren(container)) {
                append(child, indent + 1)
            }
        }
    }

    private class ValueInfo(
        val name: String?,
        val kind: String?,
        val type: String?,
        val value: String?,
        val sourcePosition: SourcePosition?,
        val mightHaveChildren: Boolean
    )

    private fun computeInfo(container: XValueContainer): ValueInfo {
        val name = if (container is XNamedValue) container.name.takeIf { it.isNotEmpty() } else null

        when (container) {
            is XValue -> {
                val node = XTestValueNode()
                container.computePresentation(node, XValuePlace.TREE)
                node.waitFor(XDebuggerTestUtil.TIMEOUT_MS.toLong())

                val descriptor = if (container is NodeDescriptorProvider) container.descriptor else null
                val kind = getLabel(descriptor)
                val type = (descriptor as? ValueDescriptorImpl)?.declaredType ?: node.myType?.takeIf { it.isNotEmpty() }
                val value = (computeValue(descriptor) ?: node.myValue).takeIf { it.isNotEmpty() }
                val sourcePosition = computeSourcePosition(descriptor)
                val mightHaveChildren = node.myHasChildren && (descriptor == null || descriptor.isExpandable)

                return ValueInfo(name, kind, type, value, sourcePosition, mightHaveChildren)
            }
            is XStackFrame -> {
                val sourcePosition = DebuggerUtilsEx.toSourcePosition(container.sourcePosition, debugProcess.project)
                return ValueInfo(name, kind = null, type = null, value = null, sourcePosition, mightHaveChildren = true)
            }
            else -> {
                return ValueInfo(name, kind = null, type = null, value = null, sourcePosition = null, mightHaveChildren = true)
            }
        }
    }

    private fun computeValue(descriptor: NodeDescriptorImpl?): String? {
        val valueDescriptor = descriptor as? ValueDescriptorImpl ?: return null

        val renderer = debugProcess
            .invokeInManagerThread { debuggerContext -> valueDescriptor.getRenderer(debuggerContext.debugProcess) }
            ?.get(XDebuggerTestUtil.TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            ?: return null

        val semaphore = Semaphore()
        semaphore.down()

        val immediateValue = debugProcess.invokeInManagerThread { debuggerContext ->
            val suspendContext = debuggerContext.suspendContext ?: error("Can't get suspend context")
            val evaluationContext = EvaluationContextImpl(suspendContext, stackFrame)
            renderer.calcLabel(descriptor, evaluationContext) { semaphore.up() }
        }

        return when {
            immediateValue != XDebuggerUIConstants.getCollectingDataMessage() -> immediateValue
            semaphore.waitFor(XDebuggerTestUtil.TIMEOUT_MS.toLong()) -> descriptor.valueText
            else -> null
        }
    }

    private fun computeSourcePosition(descriptor: NodeDescriptorImpl?): SourcePosition? {
        if (descriptor == null) {
            return null
        }

        return debugProcess.invokeInManagerThread { debuggerContext ->
            SourcePositionProvider.getSourcePosition(descriptor, debugProcess.project, debuggerContext)
        }
    }

    private fun getLabel(descriptor: NodeDescriptorImpl?): String? {
        return when (descriptor) {
            is StackFrameDescriptor -> "frame"
            is WatchItemDescriptor -> "watch"
            is LocalVariableDescriptor -> "local"
            is StaticDescriptor -> "static"
            is ThisDescriptorImpl -> "this"
            is FieldDescriptor -> "field"
            is ArrayElementDescriptor -> "element"
            is ContinuationVariableValueDescriptorImpl -> "continuation"
            else -> null
        }
    }

    private fun collectChildren(container: XValueContainer): List<XValue> {
        val isExpandable = when (container) {
            is XStackFrame -> true
            is NodeDescriptorProvider -> container.descriptor.isExpandable
            else -> false
        }

        if (isExpandable) {
            return XDebuggerTestUtil.collectChildren(container)
        } else {
            return emptyList()
        }
    }

    private fun Appendable.appendIndent(indent: Int) {
        repeat(indent) { append("    ") }
    }
}

fun SourcePosition.render(): String {
    val virtualFile = file.originalFile.virtualFile ?: file.viewProvider.virtualFile

    val libraryEntry = LibraryUtil.findLibraryEntry(virtualFile, file.project)
    if (libraryEntry != null && (libraryEntry is JdkOrderEntry || libraryEntry.presentableName == KOTLIN_LIBRARY_NAME)) {
        val suffix = if (isInCompiledFile()) "COMPILED" else "EXT"
        return FileUtil.getNameWithoutExtension(virtualFile.name) + ".!$suffix!"
    }

    return virtualFile.name + ":" + (line + 1)
}

private fun SourcePosition.isInCompiledFile(): Boolean {
    val ktFile = file as? KtFile ?: return false
    return ktFile.isCompiled
}