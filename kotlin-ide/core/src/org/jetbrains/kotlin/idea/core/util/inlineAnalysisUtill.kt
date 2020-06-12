package org.jetbrains.kotlin.idea.core.util

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompositeBindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.ArrayList
import java.util.HashSet
import java.util.LinkedHashSet

fun analyzeInlinedFunctions(
    resolutionFacadeForFile: ResolutionFacade,
    file: KtFile,
    analyzeOnlyReifiedInlineFunctions: Boolean,
    bindingContext: BindingContext? = null
): Pair<BindingContext, List<KtFile>> {
    val analyzedElements = HashSet<KtElement>()
    val context = analyzeElementWithInline(
        resolutionFacadeForFile,
        file,
        1,
        analyzedElements,
        !analyzeOnlyReifiedInlineFunctions, bindingContext
    )

    //We processing another files just to annotate anonymous classes within their inline functions
    //Bytecode not produced for them cause of filtering via generateClassFilter
    val toProcess = LinkedHashSet<KtFile>()
    toProcess.add(file)

    for (collectedElement in analyzedElements) {
        val containingFile = collectedElement.containingKtFile
        toProcess.add(containingFile)
    }

    return Pair<BindingContext, List<KtFile>>(context, ArrayList(toProcess))
}

private fun analyzeElementWithInline(
    resolutionFacade: ResolutionFacade,
    element: KtElement,
    deep: Int,
    analyzedElements: MutableSet<KtElement>,
    analyzeInlineFunctions: Boolean,
    fullResolveContext: BindingContext? = null
): BindingContext {
    val project = element.project
    val declarationsWithBody = HashSet<KtDeclarationWithBody>()

    val innerContexts = ArrayList<BindingContext>()
    innerContexts.addIfNotNull(fullResolveContext)

    element.accept(object : KtTreeVisitorVoid() {
        override fun visitExpression(expression: KtExpression) {
            super.visitExpression(expression)

            val bindingContext = resolutionFacade.analyze(expression)
            innerContexts.add(bindingContext)

            val call = bindingContext.get(BindingContext.CALL, expression) ?: return

            val resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, call)
            checkResolveCall(resolvedCall)
        }

        override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
            super.visitDestructuringDeclaration(destructuringDeclaration)

            val bindingContext = resolutionFacade.analyze(destructuringDeclaration)
            innerContexts.add(bindingContext)

            for (entry in destructuringDeclaration.entries) {
                val resolvedCall = bindingContext.get(BindingContext.COMPONENT_RESOLVED_CALL, entry)
                checkResolveCall(resolvedCall)
            }
        }

        override fun visitForExpression(expression: KtForExpression) {
            super.visitForExpression(expression)

            val bindingContext = resolutionFacade.analyze(expression)
            innerContexts.add(bindingContext)

            checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, expression.loopRange))
            checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, expression.loopRange))
            checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, expression.loopRange))
        }

        private fun checkResolveCall(resolvedCall: ResolvedCall<*>?) {
            if (resolvedCall == null) return

            val descriptor = resolvedCall.resultingDescriptor
            if (descriptor is DeserializedSimpleFunctionDescriptor) return

            isAdditionalResolveNeededForDescriptor(descriptor)

            if (descriptor is PropertyDescriptor) {
                for (accessor in descriptor.accessors) {
                    isAdditionalResolveNeededForDescriptor(accessor)
                }
            }
        }

        private fun isAdditionalResolveNeededForDescriptor(descriptor: CallableDescriptor) {
            if (!(InlineUtil.isInline(descriptor) && (analyzeInlineFunctions || hasReifiedTypeParameters(descriptor)))) {
                return
            }

            val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
            if (declaration != null && declaration is KtDeclarationWithBody && !analyzedElements.contains(declaration)) {
                declarationsWithBody.add(declaration)
                return
            }
        }
    })

    analyzedElements.add(element)

    if (declarationsWithBody.isNotEmpty() && deep < 10) {
        for (inlineFunction in declarationsWithBody) {
            val body = inlineFunction.bodyExpression
            if (body != null) {
                innerContexts.add(
                    analyzeElementWithInline(
                        resolutionFacade,
                        inlineFunction,
                        deep + 1,
                        analyzedElements,
                        analyzeInlineFunctions
                    )
                )
            }
        }

        analyzedElements.addAll(declarationsWithBody)
    }

    return CompositeBindingContext.create(innerContexts)
}

private fun hasReifiedTypeParameters(descriptor: CallableDescriptor): Boolean {
    return descriptor.typeParameters.any { it.isReified }
}