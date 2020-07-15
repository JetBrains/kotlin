/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.FunctionGenerationStrategy.CodegenBased
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmOverloadsAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Opcodes


class CliNoArgExpressionCodegenExtension(private val annotations: List<String>, invokeInitializers: Boolean = false) :
    AbstractNoArgExpressionCodegenExtension(invokeInitializers) {

    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> = annotations
}

abstract class AbstractNoArgExpressionCodegenExtension(val invokeInitializers: Boolean) : ExpressionCodegenExtension,
    AnnotationBasedExtension {

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) = with(codegen) {
        if (shouldGenerateNoArgConstructor()) {
            generateNoArgConstructor()
        }
    }

    private fun ImplementationBodyCodegen.generateNoArgConstructor() {
        val superClassInternalName = typeMapper.mapClass(descriptor.getSuperClassOrAny()).internalName

        val constructorDescriptor = createNoArgConstructorDescriptor(descriptor)

        val superClass = descriptor.getSuperClassOrAny()

        // If a parent sealed class has not a zero-parameter constructor, user must write @NoArg annotation for the parent class as well,
        // and then we generate <init>()V
        val isParentASealedClassWithDefaultConstructor =
            superClass.modality == Modality.SEALED && superClass.constructors.any { isZeroParameterConstructor(it) }

        functionCodegen.generateMethod(JvmDeclarationOrigin.NO_ORIGIN, constructorDescriptor, object : CodegenBased(state) {
            override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                codegen.v.load(0, AsmTypes.OBJECT_TYPE)

                if (isParentASealedClassWithDefaultConstructor) {
                    codegen.v.aconst(null)
                    codegen.v.visitMethodInsn(
                        Opcodes.INVOKESPECIAL, superClassInternalName, "<init>",
                        "(Lkotlin/jvm/internal/DefaultConstructorMarker;)V", false
                    )
                } else {
                    codegen.v.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassInternalName, "<init>", "()V", false)
                }

                if (invokeInitializers) {
                    generateInitializers(codegen)
                }
                codegen.v.visitInsn(Opcodes.RETURN)
            }
        })
    }

    private fun ImplementationBodyCodegen.shouldGenerateNoArgConstructor(): Boolean {
        val origin = myClass as? KtClass ?: return false

        if (descriptor.kind != ClassKind.CLASS || !descriptor.hasSpecialAnnotation(origin)) {
            return false
        }

        return descriptor.constructors.none { isZeroParameterConstructor(it) }
    }

    override val shouldGenerateClassSyntheticPartsInLightClassesMode = true

    companion object {

        fun isZeroParameterConstructor(constructor: ClassConstructorDescriptor): Boolean {
            val parameters = constructor.valueParameters
            return parameters.isEmpty() ||
                    (parameters.all { it.declaresDefaultValue() } && (constructor.isPrimary || constructor.findJvmOverloadsAnnotation() != null))
        }

        fun createNoArgConstructorDescriptor(containingClass: ClassDescriptor): ConstructorDescriptor =
            ClassConstructorDescriptorImpl.createSynthesized(containingClass, Annotations.EMPTY, false, SourceElement.NO_SOURCE).apply {
                initialize(
                    null,
                    calculateDispatchReceiverParameter(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    containingClass.builtIns.unitType,
                    Modality.OPEN,
                    DescriptorVisibilities.PUBLIC
                )
            }
    }
}
