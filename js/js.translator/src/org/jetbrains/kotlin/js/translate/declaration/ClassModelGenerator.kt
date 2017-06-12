/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.declaration

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.bridges.Bridge
import org.jetbrains.kotlin.backend.common.bridges.generateBridgesForFunctionDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.StaticContext
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.prototypeOf
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn
import org.jetbrains.kotlin.js.translate.utils.generateDelegateCall
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.hasOrInheritsParametersWithDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.hasOwnParametersWithDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.identity

class ClassModelGenerator(val context: StaticContext) {
    fun generateClassModel(descriptor: ClassDescriptor): JsClassModel {
        val superName = descriptor.getSuperClassNotAny()?.let { context.getInnerNameForDescriptor(it) }
        val model = JsClassModel(context.getInnerNameForDescriptor(descriptor), superName)
        if (descriptor.kind != ClassKind.ANNOTATION_CLASS && !AnnotationsUtils.isNativeObject(descriptor)) {
            copyDefaultMembers(descriptor, model)
            generateBridgeMethods(descriptor, model)
        }
        return model
    }

    private fun copyDefaultMembers(descriptor: ClassDescriptor, model: JsClassModel) {
        val members = descriptor.unsubstitutedMemberScope
                .getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
                .mapNotNull { it as? CallableMemberDescriptor }

        // Traverse fake non-abstract member. Current class does not provide their implementation,
        // it can be inherited from interface.
        for (member in members.filter { !it.kind.isReal && it.modality != Modality.ABSTRACT }) {
            if (member is FunctionDescriptor) {
                tryCopyWhenImplementingInterfaceWithDefaultArgs(member, model)
            }

            copySimpleMember(descriptor, member, model)

            // Copy *implementation* functions (i.e. those ones which end with `$default` suffix)
            // of Kotlin functions with optional parameters.
            if (member is FunctionDescriptor && !hasImplementationInPrototype(member)) {
                copyMemberWithOptionalArgs(descriptor, member, model, Namer.DEFAULT_PARAMETER_IMPLEMENTOR_SUFFIX)
            }
        }

        // Traverse non-fake non-abstract members. Current class provides their implementation, but the implementation
        // may override function with optional parameters. In this case we already copied *implementation* function
        // (with `$default` suffix) but we also need *dispatcher* function (without suffix).
        // Case of fake member is covered by previous loop.
        for (function in members.filterIsInstance<FunctionDescriptor>().filter { it.modality != Modality.ABSTRACT && it.kind.isReal }) {
            copyMemberWithOptionalArgs(descriptor, function, model, "")
        }
    }

    private fun hasImplementationInPrototype(member: CallableMemberDescriptor): Boolean {
        return member.overriddenDescriptors.any {
            it.modality != Modality.ABSTRACT && !DescriptorUtils.isInterface(it.containingDeclaration)
        }
    }

    // Cover very special case. Consider
    //
    //     open class B { fun foo(x: Int): Unit }
    //     interface I { fun foo(x: Int = ...): Unit }
    //     class D : B(), I
    //
    // Interface I provides dispatcher function, but no implementation function. It's expected that D
    // inherits dispatcher function from I (by copying it) and implementation function from B.
    // However, D inherits `foo` without suffix (i.e. it corresponds to I's dispatcher function).
    // We must copy B.foo to D.foo$default and then I.foo to D.foo
    private fun tryCopyWhenImplementingInterfaceWithDefaultArgs(member: FunctionDescriptor, model: JsClassModel) {
        val fromInterface = member.overriddenDescriptors.firstOrNull { it.hasOwnParametersWithDefaultValue() } ?: return
        if (!DescriptorUtils.isInterface(fromInterface.containingDeclaration)) return
        val fromClass = member.overriddenDescriptors.firstOrNull { !DescriptorUtils.isInterface(it.containingDeclaration) } ?: return

        val targetClass = member.containingDeclaration as ClassDescriptor
        val fromInterfaceName = context.getNameForDescriptor(fromInterface).ident

        copyMethod(context.getNameForDescriptor(fromClass).ident, fromInterfaceName + Namer.DEFAULT_PARAMETER_IMPLEMENTOR_SUFFIX,
                   fromClass.containingDeclaration as ClassDescriptor, targetClass, model.postDeclarationBlock)
        copyMethod(fromInterfaceName, context.getNameForDescriptor(member).ident,
                   fromInterface.containingDeclaration as ClassDescriptor, targetClass,
                   model.postDeclarationBlock)
    }

    private fun copySimpleMember(descriptor: ClassDescriptor, member: CallableMemberDescriptor, model: JsClassModel) {
        // Special case: fake descriptor denotes (possible multiple) private members from different super interfaces
        if (member.visibility == Visibilities.INVISIBLE_FAKE) return copyInvisibleFakeMember(descriptor, member, model)

        val memberToCopy = findMemberToCopy(member) ?: return
        val classToCopyFrom = memberToCopy.containingDeclaration as ClassDescriptor
        if (classToCopyFrom.kind != ClassKind.INTERFACE || AnnotationsUtils.isNativeObject(classToCopyFrom)) return

        val name = context.getNameForDescriptor(member).ident
        when (member) {
            is FunctionDescriptor -> {
                copyMethod(name, name, classToCopyFrom, descriptor, model.postDeclarationBlock)
            }
            is PropertyDescriptor -> copyProperty(name, classToCopyFrom, descriptor, model.postDeclarationBlock)
        }
    }

    private fun copyInvisibleFakeMember(descriptor: ClassDescriptor, member: CallableMemberDescriptor, model: JsClassModel) {
        for (overriddenMember in member.overriddenDescriptors) {
            val memberToCopy = if (overriddenMember.kind.isReal) overriddenMember else findMemberToCopy(overriddenMember) ?: continue
            val classToCopyFrom = memberToCopy.containingDeclaration as ClassDescriptor
            if (classToCopyFrom.kind != ClassKind.INTERFACE) continue

            val name = context.getNameForDescriptor(memberToCopy).ident
            when (member) {
                is FunctionDescriptor -> {
                    copyMethod(name, name, classToCopyFrom, descriptor, model.postDeclarationBlock)
                }
                is PropertyDescriptor -> copyProperty(name, classToCopyFrom, descriptor, model.postDeclarationBlock)
            }
        }
    }

    private fun copyMemberWithOptionalArgs(descriptor: ClassDescriptor, member: FunctionDescriptor, model: JsClassModel, suffix: String) {
        val memberToCopy = findOptionalArgsMemberToCopy(member) ?: return
        val classToCopyFrom = memberToCopy.containingDeclaration as ClassDescriptor
        if (classToCopyFrom.kind != ClassKind.INTERFACE || AnnotationsUtils.isNativeObject(classToCopyFrom)) return

        val name = context.getNameForDescriptor(member).ident + suffix
        copyMethod(name, name, classToCopyFrom, descriptor, model.postDeclarationBlock)
    }

    private fun findMemberToCopy(member: CallableMemberDescriptor): CallableMemberDescriptor? {
        // If one of overridden members is non-abstract, copy it.
        // When none found, we have nothing to copy, ignore.
        // When multiple found, our current class should provide implementation, ignore.
        val memberToCopy = member.findNonRepeatingOverriddenDescriptors({ overriddenDescriptors }, { original })
                .filter { it.modality != Modality.ABSTRACT }
                .singleOrNull() ?: return null

        // If found member is not from interface, we don't need to copy it, it's already in prototype
        if ((memberToCopy.containingDeclaration as ClassDescriptor).kind != ClassKind.INTERFACE) return null

        // If found member is fake itself, repeat search for it, until we find actual implementation
        return if (!memberToCopy.kind.isReal) findMemberToCopy(memberToCopy) else memberToCopy
    }

    private fun findOptionalArgsMemberToCopy(member: FunctionDescriptor): FunctionDescriptor? {
        // If one of overridden members has parameters with default value, copy it.
        // When non found, we have nothing to copy, ignore.
        // When multiple found, our current class should provide implementation, ignore.
        val memberToCopy = member.findNonRepeatingOverriddenDescriptors({ overriddenDescriptors }, { original })
                .filter { it.hasOrInheritsParametersWithDefaultValue() }
                .singleOrNull() ?: return null

        // If found member is not from interface, we don't need to copy it, it's already in prototype
        if ((memberToCopy.containingDeclaration as ClassDescriptor).kind != ClassKind.INTERFACE) return null

        // If found member is fake itself, repeat search for it, until we find actual implementation
        return if (!memberToCopy.kind.isReal) findOptionalArgsMemberToCopy(memberToCopy) else memberToCopy
    }

    private fun <T : CallableMemberDescriptor> T.findNonRepeatingOverriddenDescriptors(
            getTypedOverriddenDescriptors: T.() -> Collection<T>,
            getOriginalDescriptor: T.() -> T
    ): List<T> {
        val allDescriptors = mutableSetOf<T>()
        val repeatedDescriptors = mutableSetOf<T>()
        fun walk(descriptor: T) {
            val original = descriptor.getOriginalDescriptor()
            if (!allDescriptors.add(original)) return
            val overridden = original.getTypedOverriddenDescriptors().map { it.getOriginalDescriptor() }
            repeatedDescriptors += overridden
            overridden.forEach { walk(it) }
        }

        val directOverriddenDescriptors = getTypedOverriddenDescriptors()
        directOverriddenDescriptors.forEach { walk(it) }
        return directOverriddenDescriptors.filter { it.getOriginalDescriptor() !in repeatedDescriptors }
    }

    private fun generateBridgeMethods(descriptor: ClassDescriptor, model: JsClassModel) {
        generateBridgesToTraitImpl(descriptor, model)
        generateOtherBridges(descriptor, model)
    }

    private fun generateBridgesToTraitImpl(descriptor: ClassDescriptor, model: JsClassModel) {
        val translationContext = TranslationContext.rootContext(context)
        for ((key, value) in CodegenUtil.getNonPrivateTraitMethods(descriptor)) {
            val sourceName = context.getNameForDescriptor(key).ident
            val targetName = context.getNameForDescriptor(value).ident
            if (sourceName != targetName) {
                val statement = generateDelegateCall(descriptor, key, value, JsThisRef(), translationContext, false,
                                                     descriptor.source.getPsi())
                model.postDeclarationBlock.statements += statement
            }
        }
    }

    private fun generateOtherBridges(descriptor: ClassDescriptor, model: JsClassModel) {
        for (memberDescriptor in descriptor.defaultType.memberScope.getContributedDescriptors()) {
            if (memberDescriptor is FunctionDescriptor) {
                val bridgesToGenerate = generateBridgesForFunctionDescriptor(memberDescriptor, identity()) {
                    //There is no DefaultImpls in js backend so if method non-abstract it should be recognized as non-abstract on bridges calculation
                    false
                }

                for (bridge in bridgesToGenerate) {
                    generateBridge(descriptor, model, bridge)
                }
            }
        }
    }

    private fun generateBridge(descriptor: ClassDescriptor, model: JsClassModel, bridge: Bridge<FunctionDescriptor>) {
        val fromDescriptor = bridge.from
        val toDescriptor = bridge.to

        if (toDescriptor.visibility == Visibilities.INVISIBLE_FAKE) return

        val sourceName = context.getNameForDescriptor(fromDescriptor).ident
        val targetName = context.getNameForDescriptor(toDescriptor).ident
        if (sourceName == targetName) return

        if ((fromDescriptor.containingDeclaration as ClassDescriptor).kind != ClassKind.INTERFACE) {
            if (fromDescriptor.kind.isReal && fromDescriptor.modality != Modality.ABSTRACT && !toDescriptor.kind.isReal) return
        }

        val translationContext = TranslationContext.rootContext(context)
        model.postDeclarationBlock.statements += generateDelegateCall(descriptor, fromDescriptor, toDescriptor, JsThisRef(),
                                                                      translationContext, false, descriptor.source.getPsi())
    }

    private fun copyMethod(
            sourceName: String,
            targetName: String,
            sourceDescriptor: ClassDescriptor,
            targetDescriptor: ClassDescriptor,
            block: JsBlock
    ) {
        if (targetDescriptor.module != context.currentModule) return

        val targetPrototype = prototypeOf(pureFqn(context.getInnerNameForDescriptor(targetDescriptor), null))
        val sourcePrototype = prototypeOf(pureFqn(context.getInnerNameForDescriptor(sourceDescriptor), null))
        val targetFunction = JsNameRef(targetName, targetPrototype)
        val sourceFunction = JsNameRef(sourceName, sourcePrototype)
        block.statements += JsAstUtils.assignment(targetFunction, sourceFunction).makeStmt()
    }

    private fun copyProperty(
            name: String,
            sourceDescriptor: ClassDescriptor,
            targetDescriptor: ClassDescriptor,
            block: JsBlock
    ) {
        if (targetDescriptor.module != context.currentModule) return

        val targetPrototype = prototypeOf(pureFqn(context.getInnerNameForDescriptor(targetDescriptor), null))
        val sourcePrototype = prototypeOf(pureFqn(context.getInnerNameForDescriptor(sourceDescriptor), null))
        val nameLiteral = JsStringLiteral(name)

        val getPropertyDescriptor = JsInvocation(JsNameRef("getOwnPropertyDescriptor", "Object"), sourcePrototype, nameLiteral)
        val defineProperty = JsAstUtils.defineProperty(targetPrototype, name, getPropertyDescriptor)

        block.statements += defineProperty.makeStmt()
    }
}