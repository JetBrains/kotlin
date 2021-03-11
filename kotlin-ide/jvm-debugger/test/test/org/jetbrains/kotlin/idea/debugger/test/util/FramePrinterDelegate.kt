/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.util

import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl
import com.intellij.ui.treeStructure.Tree
import javax.swing.tree.TreeNode

interface FramePrinterDelegate {
    val debuggerContext: DebuggerContextImpl
    val evaluationContext: EvaluationContextImpl

    fun evaluate(suspendContext: SuspendContextImpl, textWithImports: TextWithImportsImpl)

    fun expandAll(tree: Tree, runnable: () -> Unit, filter: (TreeNode) -> Boolean, suspendContext: SuspendContextImpl)
    fun logDescriptor(descriptor: NodeDescriptorImpl, text: String)
}