/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.stubs.model

import org.jetbrains.kaptlite.signature.SigParameter
import org.jetbrains.kaptlite.signature.SigType
import org.jetbrains.kaptlite.signature.SigTypeParameter
import org.jetbrains.kaptlite.signature.SignatureParser
import org.jetbrains.kaptlite.stubs.StubGeneratorContext
import org.jetbrains.kaptlite.stubs.util.*
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.state.getInlineClassSignatureManglingSuffix
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.JvmAbi.DEFAULT_IMPLS_SUFFIX
import org.jetbrains.kotlin.load.java.JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver.isSpecializedEqualsMethod
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver.isSynthesizedBoxOrUnboxMethod
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.isInlineClassThatRequiresMangling
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.kotlin.kaptlite.kdoc.KDocParsingHelper
import javax.lang.model.element.Modifier

data class JavaMethodStub(
    val name: String,
    val modifiers: Set<Modifier>,
    val isConstructor: Boolean,
    val data: MethodData,
    private val annotations: List<JavaAnnotationStub>,
    private val javadoc: String?,
    private val constructorData: ConstructorData
) : Renderable {
    class ConstructorData(val uninitializedFields: List<UninitializedField>, val superConstructorArgs: List<JavaValue>)

    class MethodData(
        val typeParameters: List<SigTypeParameter>,
        val parameters: List<JavaParameterStub>,
        val returnType: SigType,
        val exceptionTypes: List<SigType>
    )

    companion object {
        fun parse(
            context: StubGeneratorContext, node: MethodNode,
            owner: ClassNode, ownerName: JavaClassName, ownerIsInner: Boolean, constructorData: ConstructorData
        ): JavaMethodStub? {
            if (node.isStaticInitializer || node.isBridge || node.isSynthetic || isEnumGeneratedMember(node, owner)) {
                return null
            }

            val isConstructor = node.isConstructor
            val name = if (isConstructor) ownerName.className else node.name

            val origin = context.getMethodOrigin(node)
            val descriptor = origin.descriptor
            if (descriptor != null && hasInlineClassesInSignature(descriptor, node)) {
                return null
            }

            val data = parseData(context, node, owner, ownerIsInner, origin)

            val annotations = JavaAnnotationStub.parse(context, node.access, node.visibleAnnotations, node.invisibleAnnotations)
            val javadoc = context.getKDocComment(KDocParsingHelper.DeclarationKind.METHOD, origin)

            if (!context.checker.checkMethod(name, data, annotations, origin)) {
                return null
            }

            return JavaMethodStub(name, parseModifiers(node), isConstructor, data, annotations, javadoc, constructorData)
        }

        private fun hasInlineClassesInSignature(descriptor: DeclarationDescriptor, node: MethodNode): Boolean {
            if (descriptor is CallableMemberDescriptor) {
                if (isSynthesizedBoxOrUnboxMethod(descriptor) || isSpecializedEqualsMethod(descriptor)) {
                    return true
                }

                val manglingSuffix = getInlineClassSignatureManglingSuffix(descriptor)
                if (manglingSuffix != null) {
                    return true
                }
            }

            if (descriptor is CallableDescriptor) {
                val containingClass = descriptor.containingDeclaration
                if (node.name.endsWith(IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS) && containingClass.isInlineClassThatRequiresMangling()) {
                    return true
                }
            }

            return false
        }

        private fun isEnumGeneratedMember(node: MethodNode, owner: ClassNode): Boolean {
            if (!owner.isEnum) {
                return false
            }

            val enumName = owner.name
            return when {
                node.name == "values" && node.desc == "()[L$enumName;" -> true
                node.name == "valueOf" && node.desc == "(Ljava/lang/String;)L$enumName;" -> true
                else -> false
            }
        }

        private fun parseData(
            context: StubGeneratorContext,
            node: MethodNode, owner: ClassNode, ownerIsInner: Boolean,
            origin: JvmDeclarationOrigin
        ): MethodData {
            val methodType = Type.getMethodType(node.desc)
            val rawParameterTypes = parseArgumentTypes(context, methodType, node, owner, ownerIsInner)
            val parameterNames = getParameterNamesFromSource(context, node, owner, rawParameterTypes.size, origin)
                ?: (rawParameterTypes.indices).map { "p$it" }
            val rawParameters = rawParameterTypes.mapIndexed { index, type -> SigParameter(parameterNames[index], type) }

            return if (node.signature == null) {
                val parameters = rawParameters.mapIndexed { index, param ->
                    parseParameter(context, node, param, index, rawParameters.size)
                }
                val returnType = parseType(context, methodType.returnType)
                val exceptionTypes = node.exceptions.map { parseType(context, it) }
                MethodData(emptyList(), parameters, returnType, exceptionTypes)
            } else {
                val signature = SignatureParser.parseMethodSignature(node.signature, rawParameters)
                val parameters = signature.parameters.mapIndexed { index, param ->
                    parseParameter(context, node, param.copy(type = param.type.patchType(context)), index, rawParameters.size)
                }
                MethodData(
                    signature.typeParameters.map { it.copy(bounds = it.bounds.patchTypes(context)) },
                    parameters,
                    signature.returnType.patchType(context),
                    signature.exceptionTypes.patchTypes(context)
                )
            }
        }

        private fun parseArgumentTypes(
            context: StubGeneratorContext,
            type: Type, node: MethodNode,
            owner: ClassNode, ownerIsInner: Boolean
        ): List<SigType> {
            val types = type.argumentTypes.map { parseType(context, it) }

            if (node.isConstructor) {
                if (ownerIsInner) {
                    assert(type.argumentTypes.isNotEmpty())
                    return types.drop(1)
                } else if (owner.isEnum) {
                    assert(type.argumentTypes.size >= 2)
                    assert(type.argumentTypes[0].descriptor == "Ljava/lang/String;")
                    assert(type.argumentTypes[1].sort == Type.INT)
                    return types.drop(2)
                }
            }

            return types
        }

        private fun getParameterNamesFromSource(
            context: StubGeneratorContext,
            node: MethodNode, owner: ClassNode,
            parameterCount: Int,
            origin: JvmDeclarationOrigin
        ): List<String>? {
            val descriptor = origin.descriptor as? FunctionDescriptor ?: return null

            fun getReceiverParameterName(): String {
                return AsmUtil.getNameForReceiverParameter(descriptor, context.bindingContext, context.languageVersionSettings)
            }

            if (descriptor.valueParameters.any { it.name.isSpecial }) {
                return null
            }

            val parameterNames = when (descriptor) {
                is PropertyGetterDescriptor -> if (descriptor.isExtension) listOf(getReceiverParameterName()) else emptyList()
                is PropertySetterDescriptor -> {
                    val parameterName = descriptor.valueParameters.singleOrNull()?.name?.takeIf { !it.isSpecial } ?: return null

                    when {
                        descriptor.isExtension -> {
                            val receiverParameterName = getReceiverParameterName()
                            if (receiverParameterName == parameterName.asString()) {
                                return null
                            }
                            listOf(receiverParameterName, parameterName.asString())
                        }
                        else -> listOf(parameterName.asString())
                    }
                }
                else -> {
                    val ordinaryParameterNames = descriptor.valueParameters.map { it.name.asString() }
                    when {
                        descriptor.isExtension -> {
                            val receiverParameterName = getReceiverParameterName()
                            if (receiverParameterName in ordinaryParameterNames) {
                                return null
                            }
                            listOf(getReceiverParameterName()) + ordinaryParameterNames
                        }
                        else -> ordinaryParameterNames
                    }
                }
            }

            val prefix = ArrayList<String>(1)

            if (isDefaultImplsMethod(descriptor, node, owner)) {
                if (descriptor.valueParameters.any { it.name.asString() == AsmUtil.THIS_IN_DEFAULT_IMPLS }) {
                    // In theory, we can invent some good unique name here, but... nevermind.
                    return null
                }
                prefix += AsmUtil.THIS_IN_DEFAULT_IMPLS
            }

            val delta = when {
                isEnumConstructor(descriptor) -> 2
                isInnerClassConstructor(descriptor) -> 1
                else -> 0
            }

            val expectedNameCount = parameterNames.size + prefix.size + delta
            if (expectedNameCount != parameterCount) {
                return null
            }

            return prefix + parameterNames
        }

        private fun parseParameter(
            context: StubGeneratorContext,
            node: MethodNode,
            param: SigParameter,
            index: Int, count: Int
        ): JavaParameterStub {
            val isVarargs = node.isVarargs && index == count - 1 && param.type is SigType.Array
            val visibleAnnotations = node.visibleParameterAnnotations?.getOrNull(index)
            val invisibleAnnotations = node.invisibleParameterAnnotations?.getOrNull(index)
            val annotations = JavaAnnotationStub.parse(context, 0, visibleAnnotations, invisibleAnnotations)
            val patchedType = param.type.patchType(context).fixVarargType(isVarargs)
            return JavaParameterStub(param.name, patchedType, 0, isVarargs, annotations)
        }

        private fun SigType.fixVarargType(isVarargs: Boolean): SigType {
            if (isVarargs) {
                val arrayType = this as SigType.Array
                return arrayType.elementType
            }

            return this
        }

        private fun isEnumConstructor(descriptor: FunctionDescriptor): Boolean {
            val containingClass = descriptor.containingDeclaration as? ClassDescriptor ?: return false
            return containingClass.kind == ClassKind.ENUM_CLASS && descriptor is ConstructorDescriptor
        }

        private fun isInnerClassConstructor(descriptor: FunctionDescriptor): Boolean {
            val containingClass = descriptor.containingDeclaration as? ClassDescriptor ?: return false
            return containingClass.kind == ClassKind.CLASS && containingClass.isInner && descriptor is ConstructorDescriptor
        }

        private fun isDefaultImplsMethod(descriptor: FunctionDescriptor, node: MethodNode, owner: ClassNode): Boolean {
            if (!owner.name.endsWith(DEFAULT_IMPLS_SUFFIX) || node.isAbstract) {
                return false
            }

            val containingClass = descriptor.containingDeclaration as? ClassDescriptor ?: return false
            return containingClass.kind == ClassKind.INTERFACE
        }
    }

    override fun CodeScope.render() {
        appendJavadoc(javadoc)
        appendList(annotations, "\n", postfix = "\n")
        appendModifiers(modifiers).append(' ')
        appendList(data.typeParameters, prefix = "<", postfix = "> ") { append(it) }
        if (!isConstructor) {
            append(data.returnType).append(' ')
        }
        append(name)
        append('(')
        appendList(data.parameters)
        append(')')

        if (data.exceptionTypes.isNotEmpty()) {
            appendList(data.exceptionTypes, prefix = " throws ") { append(it) }
        }

        if (Modifier.ABSTRACT !in modifiers) {
            val uninitializedFields = constructorData.uninitializedFields
            val superConstructorArgs = constructorData.superConstructorArgs

            if (!data.returnType.isVoid || isConstructor && (uninitializedFields.isNotEmpty() || superConstructorArgs.isNotEmpty())) {
                append(' ').block {
                    if (isConstructor) {
                        if (superConstructorArgs.isNotEmpty()) {
                            append("super(")
                            appendList(superConstructorArgs)
                            append(");")
                        }

                        appendList(uninitializedFields, "\n", prefix = if (superConstructorArgs.isNotEmpty()) "\n" else "") {
                            append("this.").append(it.name).append(" = ").append(it.type.defaultValue).append(';')
                        }
                    }
                    if (!data.returnType.isVoid) {
                        newLineIfNeeded()
                        append("return ").append(data.returnType.defaultValue).append(';')
                    }
                }
            } else {
                append(" {}")
            }
        } else {
            append(';')
        }
    }
}

class UninitializedField(val name: String, val type: SigType)

class JavaParameterStub(
    val name: String,
    val type: SigType,
    private val access: Int,
    private val isVarargs: Boolean,
    private val annotations: List<JavaAnnotationStub>
) : Renderable {
    override fun CodeScope.render() {
        appendList(annotations, " ", postfix = " ")
        if ((access and Opcodes.ACC_FINAL) != 0) {
            append("final ")
        }
        append(type)
        if (isVarargs) {
            append("...")
        }
        append(' ')
        append(name)
    }
}