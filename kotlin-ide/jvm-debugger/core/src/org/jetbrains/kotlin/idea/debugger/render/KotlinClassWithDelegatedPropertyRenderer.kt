/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.render

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.DebuggerManagerThreadImpl
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.settings.NodeRendererSettings
import com.intellij.debugger.ui.impl.watch.MessageDescriptor
import com.intellij.debugger.ui.impl.watch.NodeManagerImpl
import com.intellij.debugger.ui.tree.DebuggerTreeNode
import com.intellij.debugger.ui.tree.ValueDescriptor
import com.intellij.debugger.ui.tree.render.ChildrenBuilder
import com.intellij.debugger.ui.tree.render.ClassRenderer
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.settings.XDebuggerSettingsManager
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.load.java.JvmAbi
import java.util.*

private val LOG = Logger.getInstance(KotlinClassWithDelegatedPropertyRenderer::class.java)
private fun notPreparedClassMessage(referenceType: ReferenceType) =
    "$referenceType ${referenceType.isPrepared} ${referenceType.sourceName()}"

class KotlinClassWithDelegatedPropertyRenderer : ClassRenderer() {
    private val rendererSettings = NodeRendererSettings.getInstance()

    override fun isApplicable(jdiType: Type?): Boolean {
        if (!super.isApplicable(jdiType) || jdiType !is ReferenceType || !jdiType.isPrepared || !jdiType.isInKotlinSources()) {
            return false
        }

        try {
            return jdiType.allFields().any { it.name().endsWith(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX) }
        } catch (notPrepared: ClassNotPreparedException) {
            LOG.error(notPreparedClassMessage(jdiType), notPrepared)
        }

        return false
    }

    override fun calcLabel(
        descriptor: ValueDescriptor,
        evaluationContext: EvaluationContext,
        listener: DescriptorLabelListener
    ): String {
        val res = calcToStringLabel(descriptor, evaluationContext, listener)
        if (res != null) {
            return res
        }

        return super.calcLabel(descriptor, evaluationContext, listener)
    }

    private fun calcToStringLabel(
        descriptor: ValueDescriptor, evaluationContext: EvaluationContext,
        listener: DescriptorLabelListener
    ): String? {
        val toStringRenderer = rendererSettings.toStringRenderer
        if (toStringRenderer.isEnabled && DebuggerManagerEx.getInstanceEx(evaluationContext.project).context.canRunEvaluation) {
            if (toStringRenderer.isApplicable(descriptor.type)) {
                return toStringRenderer.calcLabel(descriptor, evaluationContext, listener)
            }
        }
        return null
    }

    override fun buildChildren(value: Value?, builder: ChildrenBuilder, context: EvaluationContext) {
        DebuggerManagerThreadImpl.assertIsManagerThread()

        if (value !is ObjectReference) return

        val nodeManager = builder.nodeManager!!
        val nodeDescriptorFactory = builder.descriptorManager!!

        val fields = value.referenceType().allFields()
        if (fields.isEmpty()) {
            builder.setChildren(listOf(nodeManager.createMessageNode(MessageDescriptor.CLASS_HAS_NO_FIELDS.label)))
            return
        }

        val children = ArrayList<DebuggerTreeNode>()
        for (field in fields) {
            if (!shouldDisplay(context, value, field)) {
                continue
            }

            val fieldDescriptor = nodeDescriptorFactory.getFieldDescriptor(builder.parentDescriptor, value, field)

            if (field.name().endsWith(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX)) {
                val shouldRenderDelegatedProperty = KotlinDebuggerSettings.getInstance().DEBUG_RENDER_DELEGATED_PROPERTIES
                if (shouldRenderDelegatedProperty && !ToggleKotlinVariablesState.getService().kotlinVariableView) {
                    children.add(nodeManager.createNode(fieldDescriptor, context))
                }

                val delegatedPropertyDescriptor = DelegatedPropertyFieldDescriptor(
                    context.debugProcess.project!!,
                    value,
                    field,
                    shouldRenderDelegatedProperty
                )
                children.add(nodeManager.createNode(delegatedPropertyDescriptor, context))
            } else {
                children.add(nodeManager.createNode(fieldDescriptor, context))
            }
        }

        if (XDebuggerSettingsManager.getInstance()!!.dataViewSettings.isSortValues) {
            children.sortedWith(NodeManagerImpl.getNodeComparator())
        }

        builder.setChildren(children)
    }

}
