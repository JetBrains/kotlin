/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.NoDescriptorForDeclarationException

/**
 * This function throws exception when resolveToDescriptorIfAny returns null, otherwise works equivalently.
 */
fun KtDeclaration.unsafeResolveToDescriptor(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): DeclarationDescriptor =
    resolveToDescriptorIfAny(resolutionFacade, bodyResolveMode) ?: throw NoDescriptorForDeclarationException(this)


/**
 * This function first uses declaration resolvers to resolve this declaration and/or additional declarations (e.g. its parent),
 * and then takes the relevant descriptor from binding context.
 * The exact set of declarations to resolve depends on bodyResolveMode
 */
fun KtDeclaration.resolveToDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): DeclarationDescriptor? {
    //TODO: BodyResolveMode.PARTIAL is not quite safe!
    val context = analyze(resolutionFacade, bodyResolveMode)
    return if (this is KtParameter && hasValOrVar()) {
        context.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, this)
    } else {
        context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)
    }
}

fun KtAnnotationEntry.resolveToDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): AnnotationDescriptor? {
    //TODO: BodyResolveMode.PARTIAL is not quite safe!
    val context = analyze(resolutionFacade, bodyResolveMode)
    return context.get(BindingContext.ANNOTATION, this)
}

fun KtClassOrObject.resolveToDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): ClassDescriptor? {
    return (this as KtDeclaration).resolveToDescriptorIfAny(resolutionFacade, bodyResolveMode) as? ClassDescriptor
}

fun KtNamedFunction.resolveToDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): FunctionDescriptor? {
    return (this as KtDeclaration).resolveToDescriptorIfAny(resolutionFacade, bodyResolveMode) as? FunctionDescriptor
}

fun KtProperty.resolveToDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): VariableDescriptor? {
    return (this as KtDeclaration).resolveToDescriptorIfAny(resolutionFacade, bodyResolveMode) as? VariableDescriptor
}

fun KtParameter.resolveToParameterDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): ValueParameterDescriptor? {
    val context = analyze(resolutionFacade, bodyResolveMode)
    return context.get(BindingContext.VALUE_PARAMETER, this) as? ValueParameterDescriptor
}

fun KtElement.resolveToCall(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): ResolvedCall<out CallableDescriptor>? =
    getResolvedCall(analyze(resolutionFacade, bodyResolveMode))


@JvmOverloads
fun KtElement.analyze(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): BindingContext =
    resolutionFacade.analyze(this, bodyResolveMode)

fun KtElement.analyzeAndGetResult(resolutionFacade: ResolutionFacade): AnalysisResult {
    return AnalysisResult.success(resolutionFacade.analyze(this), resolutionFacade.moduleDescriptor)
}

// This function is used on declarations to make analysis not only declaration itself but also it content:
// body for declaration with body, initializer & accessors for properties
fun KtDeclaration.analyzeWithContent(resolutionFacade: ResolutionFacade): BindingContext =
    resolutionFacade.analyzeWithAllCompilerChecks(listOf(this)).bindingContext

// This function is used to make full analysis of declaration container.
// All its declarations, including their content (see above), are analyzed.
inline fun <reified T> T.analyzeWithContent(resolutionFacade: ResolutionFacade): BindingContext where T : KtDeclarationContainer, T : KtElement =
    resolutionFacade.analyzeWithAllCompilerChecks(listOf(this)).bindingContext

