/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.context

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.DescriptorUtils.isAncestor
import com.google.dart.compiler.backend.js.ast.JsName
import com.google.dart.compiler.backend.js.ast.JsScope

private val CAPTURED_RECEIVER_NAME_PREFIX : String = "this$"

class UsageTracker(
        private val parent: UsageTracker?,
        val containingDescriptor: MemberDescriptor,
        private val scope: JsScope,
        private val staticContext: StaticContext
) {

    private val captured = hashMapOf<CallableDescriptor, JsName>()

    // For readonly access from external places.
    val capturedDescriptorToJsName: Map<CallableDescriptor, JsName>
        get() = captured

    public fun used(descriptor: CallableDescriptor) {
        if (isCaptured(descriptor)) return

        when (descriptor) {
            is CallableMemberDescriptor -> {
                // local named function
                if (descriptor.getVisibility() == Visibilities.LOCAL) {
                    captureIfNeed(descriptor)
                }
            }
            is VariableDescriptor -> {
                if (descriptor !is PropertyDescriptor) {
                    captureIfNeed(descriptor)
                }
            }
            is ReceiverParameterDescriptor -> {
                captureIfNeed(descriptor)
            }
        }
    }

    private fun captureIfNeed(descriptor: CallableDescriptor?) {
        if (descriptor == null || isCaptured(descriptor) || isAncestor(containingDescriptor, descriptor, strict = true)) return

        parent?.captureIfNeed(descriptor)

        captured[descriptor] = descriptor.getJsNameForCapturedDescriptor()
    }

    private fun CallableDescriptor.getJsNameForCapturedDescriptor(): JsName {
        val suggestedName =
                when (this) {
                    is ReceiverParameterDescriptor -> {
                        this.getNameForCapturedReceiver()
                    }
                    else -> {
                        // TODO: drop temporary HACK or add description
                        val name = staticContext.getNameForDescriptor(this)
                        name.getIdent()
                    }
                }

        return scope.declareFreshName(suggestedName)
    }
}

public fun UsageTracker.getNameForCapturedDescriptor(descriptor: CallableDescriptor): JsName? = capturedDescriptorToJsName.get(descriptor)

public fun UsageTracker.hasCaptured(): Boolean {
    val hasNotCaptured =
            capturedDescriptorToJsName.isEmpty() ||
            (capturedDescriptorToJsName.size == 1 && capturedDescriptorToJsName.containsKey(containingDescriptor))

    return !hasNotCaptured
}

public fun UsageTracker.isCaptured(descriptor: CallableDescriptor): Boolean = capturedDescriptorToJsName.containsKey(descriptor)

// NOTE: don't use from other places to avoid name clashes! So, it is not in Namer.
private fun ReceiverParameterDescriptor.getNameForCapturedReceiver(): String {

    fun DeclarationDescriptor.getNameForCapturedDescriptor(namePostfix: String = ""): String {
        val name = this.getName()
        val nameAsString = if (name.isSpecial()) "" else name.asString()

        return CAPTURED_RECEIVER_NAME_PREFIX + nameAsString + namePostfix
    }

    val containingDeclaration = this.getContainingDeclaration()

    assert(containingDeclaration is MemberDescriptor) {
        "Unsupported descriptor type: ${containingDeclaration.getClass()}, " +
        "receiverDescriptor = $this, " +"containingDeclaration = $containingDeclaration"
    }

    if (containingDeclaration is ClassDescriptor && containingDeclaration.getKind() == ClassKind.CLASS_OBJECT) {
        return containingDeclaration.getContainingDeclaration().getNameForCapturedDescriptor(namePostfix = "$")
    }

    return containingDeclaration.getNameForCapturedDescriptor()
}