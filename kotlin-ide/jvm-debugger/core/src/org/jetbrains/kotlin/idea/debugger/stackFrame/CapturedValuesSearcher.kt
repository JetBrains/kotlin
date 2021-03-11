/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.stackFrame

import com.intellij.debugger.impl.descriptors.data.DescriptorData
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.sun.jdi.Field
import com.sun.jdi.ObjectReference
import com.sun.jdi.Value
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.idea.debugger.safeFields
import java.util.*

private sealed class PendingValue {
    class Ordinary(val name: String, val field: Field, val container: Container) : PendingValue() {
        override fun addTo(existingVariables: ExistingVariables): CapturedValueData? {
            if (!existingVariables.add(ExistingVariable.Ordinary(name))) {
                return null
            }

            return CapturedValueData(name, container.value, field)
        }
    }

    class This(val label: String, val value: Value?) : PendingValue() {
        override fun addTo(existingVariables: ExistingVariables): DescriptorData<out ValueDescriptorImpl>? {
            val thisName = if (existingVariables.hasThisVariables) {
                if (!existingVariables.add(ExistingVariable.LabeledThis(label))) {
                    // Avoid item duplication
                    return null
                }

                getThisName(label)
            } else {
                if (!existingVariables.add(ExistingVariable.LabeledThis(label))) {
                    return null
                }
                AsmUtil.THIS
            }

            return LabeledThisData(label, thisName, value)
        }
    }

    class Container(val value: ObjectReference) : PendingValue() {
        fun getChildren(): List<PendingValue> {
            return value.referenceType().safeFields()
                .filter { it.isApplicable() }
                .mapNotNull { createPendingValue(this, it) }
        }

        private fun Field.isApplicable(): Boolean {
            val name = name()
            return name.startsWith(AsmUtil.CAPTURED_PREFIX) || name == AsmUtil.CAPTURED_THIS_FIELD
        }

        override fun addTo(existingVariables: ExistingVariables): DescriptorData<out ValueDescriptorImpl>? {
            throw IllegalStateException("Should not be called on a container")
        }
    }

    abstract fun addTo(existingVariables: ExistingVariables): DescriptorData<out ValueDescriptorImpl>?
}

internal fun attachCapturedValues(
    containerValue: ObjectReference,
    existingVariables: ExistingVariables,
    collector: (DescriptorData<out ValueDescriptorImpl>) -> Unit
) {
    val values = collectPendingValues(PendingValue.Container(containerValue))

    for (value in values) {
        val descriptorData = value.addTo(existingVariables) ?: continue
        collector(descriptorData)
    }
}

private fun collectPendingValues(container: PendingValue.Container): List<PendingValue> {
    val queue = ArrayDeque<PendingValue>()
    queue.offer(container)
    val values = mutableListOf<PendingValue>()
    collectValuesBfs(queue, values)
    return values
}

private tailrec fun collectValuesBfs(queue: Deque<PendingValue>, consumer: MutableList<PendingValue>) {
    val deeperValues = ArrayDeque<PendingValue>()

    while (queue.isNotEmpty()) {
        val value = queue.removeFirst() ?: break

        if (value is PendingValue.Container) {
            val children = value.getChildren()
            deeperValues.addAll(children)
            continue
        }

        consumer += value
    }

    if (deeperValues.isNotEmpty()) {
        collectValuesBfs(deeperValues, consumer)
    }
}

private fun createPendingValue(container: PendingValue.Container, field: Field): PendingValue? {
    val name = field.name()

    if (name == AsmUtil.CAPTURED_THIS_FIELD) {
        /*
         * Captured entities.
         * In case of captured lambda, we just add values captured to the lambda to the list.
         * In case of captured this (outer this, for example), we add the 'this' value.
         */
        val value = container.value.getValue(field) as? ObjectReference ?: return null
        return when (val label = getThisValueLabel(value)) {
            null -> PendingValue.Container(value)
            else -> PendingValue.This(label, value)
        }
    } else if (name.startsWith(AsmUtil.CAPTURED_LABELED_THIS_FIELD)) {
        // Extension receivers for a new scheme ($this_<label>)
        val value = container.value.getValue(field)
        val label = name.drop(AsmUtil.CAPTURED_LABELED_THIS_FIELD.length).takeIf { it.isNotEmpty() } ?: return null
        return PendingValue.This(label, value)
    }

    // Ordinary values (everything but 'this')
    assert(name.startsWith(AsmUtil.CAPTURED_PREFIX))
    val capturedValueName = name.drop(1).takeIf { it.isNotEmpty() } ?: return null
    return PendingValue.Ordinary(capturedValueName, field, container)
}