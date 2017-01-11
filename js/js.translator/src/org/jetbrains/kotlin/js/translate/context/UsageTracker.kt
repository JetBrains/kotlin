/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.context

import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.backend.ast.JsScope
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.metadata.descriptor
import org.jetbrains.kotlin.js.descriptorUtils.isCoroutineLambda
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.*
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject

private val CAPTURED_RECEIVER_NAME_PREFIX : String = "this$"

class UsageTracker(
        private val parent: UsageTracker?,
        val containingDescriptor: MemberDescriptor,
        private val scope: JsScope
) {

    private val captured = linkedMapOf<DeclarationDescriptor, JsName>()

    // For readonly access from external places.
    val capturedDescriptorToJsName: Map<DeclarationDescriptor, JsName>
        get() = captured

    val capturedDescriptors: Set<DeclarationDescriptor>
        get() = captured.keys

    fun used(descriptor: DeclarationDescriptor) {
        if (isCaptured(descriptor)) return

        if (descriptor is FakeCallableDescriptorForObject) return

        // local named function
        if (descriptor is FunctionDescriptor && descriptor.visibility == Visibilities.LOCAL) {
            captureIfNeed(descriptor)
        }
        // local variable
        else if (descriptor is VariableDescriptor && descriptor !is PropertyDescriptor) {
            captureIfNeed(descriptor)
        }
        // this or receiver
        else if (descriptor is ReceiverParameterDescriptor) {
            captureIfNeed(descriptor)
        }
        else if (descriptor is TypeParameterDescriptor && descriptor.isReified) {
            captureIfNeed(descriptor)
        }
    }

    private fun captureIfNeed(descriptor: DeclarationDescriptor?) {
        if (descriptor == null || isCaptured(descriptor) || !isInLocalDeclaration() ||
            isAncestor(containingDescriptor, descriptor, /* strict = */ true) ||
            isReceiverAncestor(descriptor) || isSingletonReceiver(descriptor)
        ) {
            return
        }

        if (descriptor.isCoroutineLambda && descriptor == containingDescriptor) return

        parent?.captureIfNeed(descriptor)

        captured[descriptor] = descriptor.getJsNameForCapturedDescriptor()
    }

    private fun isInLocalDeclaration(): Boolean {
        val container = containingDescriptor
        return isDescriptorWithLocalVisibility(if (container is ConstructorDescriptor) container.containingDeclaration else container)
    }

    /**
     * We shouldn't capture current `this` or outer `this`. Assuming `C` is current translating class,
     * we have `descriptor == A::this` in the following cases:
     * * `A == C`
     * * `C` in inner class of `A`
     * * `A <: C`
     * * among outer classes of `C` there is `T` such that `A <: T`
     *
     * If fact, the latter case is the generalization of all previous cases, assuming that `is inner class of` and `<:` relations
     * are reflective. All this cases allow to refer to `this` directly or via sequence of `outer` fields.
     *
     * Note that the continuous sequence of inner classes may be interrupted by non-class descriptor. This means that
     * the last class of the sequence if a local class. We stop there, since this means that the next class in the sequence
     * is referred by closure variable rather than by dedicated `$outer` field.
     *
     * The nested classes are out of scope, since nested class can't refer to outer's class `this`, thus frontend will
     * never generate ReceiverParameterDescriptor for this case.
     */
    private fun isReceiverAncestor(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor !is ReceiverParameterDescriptor) return false
        if (containingDescriptor !is ClassDescriptor && containingDescriptor !is ConstructorDescriptor) return false

        // Class in which we are trying to capture variable
        val containingClass = getParentOfType(containingDescriptor, ClassDescriptor::class.java, false) ?: return false

        // Class which instance we are trying to capture
        val currentClass = descriptor.containingDeclaration as? ClassDescriptor ?: return false

        for (outerDeclaration in generateSequence(containingClass) { it.containingDeclaration as? ClassDescriptor }) {
            if (outerDeclaration == currentClass) return true
        }

        return false
    }

    /**
     * Test for the case like this:
     *
     * ```
     * object A {
     *     var x: Int
     *
     *     class B {
     *         fun foo() {
     *             { x }
     *         }
     *     }
     * }
     * ```
     *
     * We don't want to capture `A::this`, since we always can refer A by its FQN
     */
    private fun isSingletonReceiver(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor !is ReceiverParameterDescriptor) return false

        val container = descriptor.containingDeclaration
        if (!DescriptorUtils.isObject(container)) return false

        // This code is necessary for one use case. If we don't treat `O::this` as a free variable of lambda, we'll get
        // `this` in generated JS. `this` is generated since it's placed in aliasing context for `O::this`, so we will get
        // it instead of generating FQN. However, we can't refer to `this` from lambda, since `this` points not to an instance of `C`,
        // but to lambda function itself. We avoid it by treating `O::this` as a free variable.
        // Example is:
        //
        // object A(val x: Int) {
        //     fun foo() = { x }
        // }
        if (containingDescriptor !is ClassDescriptor) {
            val containingClass = getParentOfType(containingDescriptor, ClassDescriptor::class.java, false)
            if (containingClass == container) return false
        }

        return true
    }

    private fun DeclarationDescriptor.getJsNameForCapturedDescriptor(): JsName {
        val suggestedName = when (this) {
            is ReceiverParameterDescriptor -> getNameForCapturedReceiver()
            is TypeParameterDescriptor -> Namer.isInstanceSuggestedName(this)

            // Append 'closure$' prefix to avoid name clash between closure and member fields in case of local classes
            else -> {
                val mangled = NameSuggestion.sanitizeName(NameSuggestion().suggest(this)!!.names.last())
                "closure\$$mangled"
            }
        }

        return scope.declareTemporaryName(suggestedName).apply { descriptor = this@getJsNameForCapturedDescriptor }
    }
}

fun UsageTracker.getNameForCapturedDescriptor(descriptor: DeclarationDescriptor): JsName? = capturedDescriptorToJsName[descriptor]

fun UsageTracker.hasCapturedExceptContaining(): Boolean {
    val hasNotCaptured =
            capturedDescriptorToJsName.isEmpty() ||
            (capturedDescriptorToJsName.size == 1 && capturedDescriptorToJsName.containsKey(containingDescriptor))

    return !hasNotCaptured
}

fun UsageTracker.isCaptured(descriptor: DeclarationDescriptor): Boolean = capturedDescriptorToJsName.containsKey(descriptor)

// NOTE: don't use from other places to avoid name clashes! So, it is not in Namer.
private fun ReceiverParameterDescriptor.getNameForCapturedReceiver(): String {

    fun DeclarationDescriptor.getNameForCapturedDescriptor(namePostfix: String = ""): String {
        val name = this.name
        val nameAsString = if (name.isSpecial) "" else name.asString()

        return CAPTURED_RECEIVER_NAME_PREFIX + nameAsString + namePostfix
    }

    val containingDeclaration = this.containingDeclaration

    assert(containingDeclaration is MemberDescriptor) {
        "Unsupported descriptor type: ${containingDeclaration.javaClass}, " +
        "receiverDescriptor = $this, " +"containingDeclaration = $containingDeclaration"
    }

    if (DescriptorUtils.isCompanionObject(containingDeclaration)) {
        return containingDeclaration.containingDeclaration!!.getNameForCapturedDescriptor(namePostfix = "$")
    }

    return containingDeclaration.getNameForCapturedDescriptor()
}
