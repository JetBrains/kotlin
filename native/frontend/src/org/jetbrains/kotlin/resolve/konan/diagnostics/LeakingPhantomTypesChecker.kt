/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.konan.PHANTOM_TYPES
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.ClassifierUsageChecker
import org.jetbrains.kotlin.resolve.checkers.ClassifierUsageCheckerContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.konan.diagnostics.ErrorsNative.LEAKING_PHANTOM_TYPE_IN_SUPERTYPES
import org.jetbrains.kotlin.resolve.konan.diagnostics.ErrorsNative.LEAKING_PHANTOM_TYPE_IN_TYPE_PARAMETERS
import org.jetbrains.kotlin.synthetic.isVisibleOutside
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object LeakingPhantomTypesChecker {
    object Declaration : DeclarationChecker {
        override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
            if (descriptor !is MemberDescriptor
                || DescriptorUtils.isLocal(descriptor)
                || !descriptor.visibility.isVisibleOutside()
            ) return
            
            when {
                descriptor is CallableDescriptor && declaration is KtCallableDeclaration -> {
                    checkReturnType(descriptor, declaration, context)
                    checkValueParameters(descriptor, declaration, context)
                    checkTypeParameters(descriptor, declaration, context)
                }
                descriptor is ClassifierDescriptor && declaration is KtClassOrObject -> {
                    if (descriptor is ClassifierDescriptorWithTypeParameters) {
                        checkTypeParameters(descriptor, declaration, context)
                    }
                    checkSupertypes(descriptor, declaration, context)
                }
            }
        }

        private fun checkReturnType(descriptor: CallableDescriptor, declaration: KtCallableDeclaration, ctx: DeclarationCheckerContext) {
            descriptor.returnType?.findPhantom()?.let { phantomType ->
                report(declaration, phantomType, ctx.trace)
            }
        }

        private fun checkValueParameters(
            descriptor: CallableDescriptor,
            declaration: KtCallableDeclaration,
            context: DeclarationCheckerContext,
        ) {
            for ((index, valueParameter) in descriptor.valueParameters.withIndex()) {
                valueParameter.type.findPhantom()?.let { phantomType ->
                    declaration.valueParameterList?.parameters?.get(index)?.let { parameter ->
                        report(parameter, phantomType, context.trace)
                    }
                }
            }
        }

        private fun checkTypeParameters(
            descriptor: CallableDescriptor,
            declaration: KtCallableDeclaration,
            context: DeclarationCheckerContext,
        ) {
            doCheckTypeParameters(
                descriptor.typeParameters,
                declaration.typeParameterList ?: return,
                context,
            )
        }

        private fun checkTypeParameters(
            descriptor: ClassifierDescriptorWithTypeParameters,
            declaration: KtTypeParameterListOwner,
            context: DeclarationCheckerContext,
        ) {
            doCheckTypeParameters(
                descriptor.declaredTypeParameters,
                declaration.typeParameterList ?: return,
                context
            )
        }
        
        private fun doCheckTypeParameters(
            typeParameterList: List<TypeParameterDescriptor>,
            ktTypeParameterList: KtTypeParameterList,
            context: DeclarationCheckerContext,
        ) {
            for ((index, typeParameterDescriptor) in typeParameterList.withIndex()) {
                typeParameterDescriptor.upperBounds.firstNotNullOfOrNull { upperBound ->
                    upperBound?.findPhantom()
                }?.let { phantomType ->
                    ktTypeParameterList.parameters[index]?.let { ktTypeParameter ->
                        context.trace.report(LEAKING_PHANTOM_TYPE_IN_TYPE_PARAMETERS.on(ktTypeParameter, phantomType))
                    }
                }
            }
        }

        private fun checkSupertypes(
            descriptor: ClassifierDescriptor,
            declaration: KtClassOrObject,
            context: DeclarationCheckerContext
        ) {
            descriptor.typeConstructor.supertypes
                .firstNotNullOfOrNull { supertype -> supertype.findPhantom() }
                ?.let { phantomType ->
                    val target = declaration.safeAs<KtClassOrObject>() ?: return@let
                    context.trace.report(LEAKING_PHANTOM_TYPE_IN_SUPERTYPES.on(target, phantomType))
                }
        }
    }

    object ClassifierUsage : ClassifierUsageChecker {
        override fun check(targetDescriptor: ClassifierDescriptor, element: PsiElement, context: ClassifierUsageCheckerContext) {
            targetDescriptor.defaultType.findPhantom()?.let { phantomType ->
                context.trace.reportDiagnosticOnce(ErrorsNative.PHANTOM_CLASSIFIER.on(element, phantomType))
            }
        }
    }

    private fun report(target: KtCallableDeclaration, type: KotlinType, trace: BindingTrace) {
        trace.reportDiagnosticOnce(ErrorsNative.LEAKING_PHANTOM_TYPE.on(target, type))
    }

    private fun KotlinType.findPhantom(): KotlinType? {
        var phantom: KotlinType? = null
        contains { type ->
            if (type.constructor.declarationDescriptor?.isPhantom == true) {
                phantom = type
                true
            } else
                false
        }
        return phantom
    }

    private val DeclarationDescriptor.isPhantom: Boolean
        get() = fqNameSafe in PHANTOM_FQ_NAMES

    private val PHANTOM_FQ_NAMES = PHANTOM_TYPES.map { it.asSingleFqName() }.toSet()
}