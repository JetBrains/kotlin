/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.psi.PsiCall
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.slicer.SliceUsage
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ReturnValueInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.processAllUsages
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinValVar
import org.jetbrains.kotlin.idea.refactoring.changeSignature.toValVar
import org.jetbrains.kotlin.idea.references.KtPropertyDelegationMethodsReference
import org.jetbrains.kotlin.idea.references.ReferenceAccess
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.readWriteAccessWithFullExpression
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinTargetElementEvaluator
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.util.OperatorNameConventions

class InflowSlicer(
    element: KtElement,
    processor: Processor<in SliceUsage>,
    parentUsage: KotlinSliceUsage
) : Slicer(element, processor, parentUsage) {

    override fun processChildren(forcedExpressionMode: Boolean) {
        if (forcedExpressionMode) {
            (element as? KtExpression)?.let { processExpression(it) }
            return
        }

        when (element) {
            is KtProperty -> processProperty(element)

            // include overriders only when invoked on the parameter declaration
            is KtParameter -> processParameter(parameter = element, includeOverriders = parentUsage.parent == null)

            is KtDeclarationWithBody -> element.processBody()

            is KtTypeReference -> {
                val parent = element.parent
                require(parent is KtCallableDeclaration)
                require(element == parent.receiverTypeReference)
                // include overriders only when invoked on receiver type in the declaration
                processExtensionReceiver(parent, includeOverriders = parentUsage.parent == null)
            }

            is KtExpression -> processExpression(element)
        }
    }

    private fun processProperty(property: KtProperty) {
        if (property.hasDelegateExpression()) {
            val getter = (property.unsafeResolveToDescriptor() as VariableDescriptorWithAccessors).getter ?: return
            val bindingContext = property.analyzeWithContent()
            val delegateGetterResolvedCall = bindingContext[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, getter]
            delegateGetterResolvedCall?.resultingDescriptor?.toPsi()?.passToProcessor()
            return
        }

        property.initializer?.passToProcessor()

        property.getter?.processBody()

        val isDefaultGetter = property.getter?.bodyExpression == null
        val isDefaultSetter = property.setter?.bodyExpression == null
        if (isDefaultGetter) {
            if (isDefaultSetter) {
                property.processPropertyAssignments()
            } else {
                property.setter!!.processBackingFieldAssignments()
            }
        }
    }

    private fun processParameter(parameter: KtParameter, includeOverriders: Boolean) {
        if (!canProcessParameter(parameter)) return

        val function = parameter.ownerFunction ?: return

        if (function is KtPropertyAccessor && function.isSetter) {
            function.property.processPropertyAssignments()
            return
        }

        if (function is KtNamedFunction
            && function.name == OperatorNameConventions.SET_VALUE.asString()
            && function.hasModifier(KtTokens.OPERATOR_KEYWORD)
        ) {

            ReferencesSearch.search(function, analysisScope)
                .forEach(Processor { reference ->
                    if (reference is KtPropertyDelegationMethodsReference) {
                        val property = reference.element.parent as? KtProperty
                        property?.processPropertyAssignments()
                    }
                    true
                })
        }

        val parameterDescriptor = parameter.resolveToParameterDescriptorIfAny() ?: return

        if (function is KtFunction) {
            processCalls(function, includeOverriders, ArgumentSliceProducer(parameterDescriptor))
        }

        val valVar = parameter.valOrVarKeyword.toValVar()
        if (valVar != KotlinValVar.None) {
            val classOrObject = (parameter.ownerFunction as? KtPrimaryConstructor)?.getContainingClassOrObject()
            if (classOrObject is KtClass && classOrObject.isData()) {
                // Search usages of constructor parameter in form of named argument of call to "copy" function.
                // We will miss calls of "copy" with positional parameters but it's unlikely someone write such code.
                // Also, we will find named arguments of constructor calls but we need them anyway (already found above).
                val options = KotlinPropertyFindUsagesOptions(project).apply {
                    searchScope = analysisScope
                }
                //TODO: optimizations to search only in files where "copy" word is present and also not resolve anything except named arguments
                parameter.processAllUsages(options) { usageInfo ->
                    (((usageInfo.element as? KtNameReferenceExpression)
                        ?.parent as? KtValueArgumentName)
                        ?.parent as? KtValueArgument)
                        ?.getArgumentExpression()
                        ?.passToProcessorAsValue()
                }
            }

            if (valVar == KotlinValVar.Var) {
                processAssignments(parameter, analysisScope)
            }
        }
    }

    private fun processExtensionReceiver(declaration: KtCallableDeclaration, includeOverriders: Boolean) {
        processCalls(declaration, includeOverriders, ReceiverSliceProducer)
    }

    private fun processExpression(expression: KtExpression) {
        val lambda = when (expression) {
            is KtLambdaExpression -> expression.functionLiteral
            is KtNamedFunction -> expression.takeIf { expression.name == null }
            else -> null
        }
        val currentBehaviour = mode.currentBehaviour
        if (lambda != null) {
            when (currentBehaviour) {
                is LambdaResultInflowBehaviour -> {
                    lambda.passToProcessor(mode.dropBehaviour())
                }

                is LambdaParameterInflowBehaviour -> {
                    val valueParameters = lambda.valueParameters
                    if (valueParameters.isEmpty() && lambda is KtFunctionLiteral) {
                        if (currentBehaviour.parameterIndex == 0) {
                            lambda.implicitItUsages().forEach {
                                it.passToProcessor(mode.dropBehaviour())
                            }
                        }
                    } else {
                        valueParameters.getOrNull(currentBehaviour.parameterIndex)?.passToProcessor(mode.dropBehaviour())
                    }
                }

                is LambdaReceiverInflowBehaviour -> {
                    processExtensionReceiverUsages(lambda, lambda, mode.dropBehaviour())
                }
            }
            return
        }

        val pseudocode = pseudocodeCache[expression] ?: return
        val expressionValue = pseudocode.getElementValue(expression) ?: return
        when (val createdAt = expressionValue.createdAt) {
            is ReadValueInstruction -> {
                if (createdAt.target == AccessTarget.BlackBox) {
                    val originalElement = expressionValue.element as? KtExpression ?: return
                    if (originalElement != expression) {
                        processExpression(originalElement)
                    }
                    return
                }

                val accessedDescriptor = createdAt.target.accessedDescriptor ?: return
                val accessedDeclaration = accessedDescriptor.toPsi()
                when (accessedDescriptor) {
                    is SyntheticFieldDescriptor -> {
                        val property = accessedDeclaration as? KtProperty ?: return
                        if (accessedDescriptor.propertyDescriptor.setter?.isDefault != false) {
                            property.processPropertyAssignments()
                        } else {
                            property.setter?.processBackingFieldAssignments()
                        }
                    }

                    is ReceiverParameterDescriptor -> {
                        //TODO: handle non-extension receivers?
                        val callable = accessedDescriptor.containingDeclaration as? CallableDescriptor ?: return
                        when (val declaration = callable.toPsi()) {
                            is KtFunctionLiteral -> {
                                declaration.passToProcessorAsValue(mode.withBehaviour(LambdaCallsBehaviour(ReceiverSliceProducer)))
                            }

                            is KtCallableDeclaration -> {
                                declaration.receiverTypeReference?.passToProcessor()
                            }
                        }
                    }

                    is ValueParameterDescriptor -> {
                        if (accessedDeclaration == null) {
                            val anonymousFunction = accessedDescriptor.containingDeclaration as? AnonymousFunctionDescriptor
                            if (anonymousFunction != null && accessedDescriptor.name.asString() == "it") {
                                val functionLiteral = anonymousFunction.source.getPsi() as KtFunctionLiteral
                                val parameterDescriptor = anonymousFunction.valueParameters.first()
                                processCalls(functionLiteral, false, ArgumentSliceProducer(parameterDescriptor))
                            }
                        } else {
                            accessedDeclaration.passDeclarationToProcessorWithOverriders()
                        }
                    }

                    else -> {
                        accessedDeclaration?.passDeclarationToProcessorWithOverriders()
                    }
                }
            }

            is MergeInstruction -> createdAt.passInputsToProcessor()

            is MagicInstruction -> when (createdAt.kind) {
                MagicKind.NOT_NULL_ASSERTION, MagicKind.CAST -> createdAt.passInputsToProcessor()

                MagicKind.BOUND_CALLABLE_REFERENCE, MagicKind.UNBOUND_CALLABLE_REFERENCE -> {
                    val callableRefExpr = expressionValue.element as? KtCallableReferenceExpression ?: return
                    val bindingContext = expression.analyze(BodyResolveMode.PARTIAL)
                    val referencedDescriptor = bindingContext[BindingContext.REFERENCE_TARGET, callableRefExpr.callableReference] ?: return
                    val referencedDeclaration = (referencedDescriptor as? DeclarationDescriptorWithSource)?.toPsi() ?: return
                    when (currentBehaviour) {
                        is LambdaResultInflowBehaviour -> {
                            referencedDeclaration.passToProcessor(mode.dropBehaviour())
                        }

                        is LambdaParameterInflowBehaviour -> {
                            val parameter = (referencedDeclaration as? KtCallableDeclaration)
                                ?.valueParameters?.getOrNull(currentBehaviour.parameterIndex)
                            parameter?.passToProcessor(mode.dropBehaviour())
                        }

                        is LambdaReceiverInflowBehaviour -> {
                            val parameter = (referencedDeclaration as? KtCallableDeclaration)
                                ?.valueParameters?.getOrNull(0)
                            parameter?.passToProcessor(mode.dropBehaviour())
                        }
                    }
                }

                else -> return
            }

            is CallInstruction -> {
                val resolvedCall = createdAt.resolvedCall
                val resultingDescriptor = resolvedCall.resultingDescriptor
                if (resultingDescriptor is FunctionInvokeDescriptor) {
                    (resolvedCall.dispatchReceiver as? ExpressionReceiver)?.expression
                        ?.passToProcessorAsValue(mode.withBehaviour(LambdaResultInflowBehaviour))
                } else {
                    resultingDescriptor.toPsi()?.passToProcessorInCallMode(createdAt.element, withOverriders = true)
                }
            }
        }
    }

    private fun KtProperty.processPropertyAssignments() {
        val accessSearchScope = if (isVar) {
            analysisScope
        } else {
            val containerScope = LocalSearchScope(getStrictParentOfType<KtDeclaration>() ?: return)
            analysisScope.intersectWith(containerScope)
        }
        processAssignments(this, accessSearchScope)
    }

    private fun processAssignments(variable: KtCallableDeclaration, accessSearchScope: SearchScope) {
        fun processVariableAccess(usageInfo: UsageInfo) {
            val refElement = usageInfo.element ?: return
            val refParent = refElement.parent

            val rhsValue = when {
                refElement is KtExpression -> {
                    val (accessKind, accessExpression) = refElement.readWriteAccessWithFullExpression(true)
                    if (accessKind == ReferenceAccess.WRITE && accessExpression is KtBinaryExpression && accessExpression.operationToken == KtTokens.EQ) {
                        accessExpression.right
                    } else {
                        accessExpression
                    }
                }

                refParent is PsiCall -> refParent.argumentList?.expressions?.getOrNull(0)

                else -> null
            }
            rhsValue?.passToProcessorAsValue()
        }

        processVariableAccesses(variable, accessSearchScope, AccessKind.WRITE_WITH_OPTIONAL_READ, ::processVariableAccess)
    }

    private fun KtPropertyAccessor.processBackingFieldAssignments() {
        forEachDescendantOfType<KtBinaryExpression> body@{
            if (it.operationToken != KtTokens.EQ) return@body
            val lhs = it.left?.let { expression -> KtPsiUtil.safeDeparenthesize(expression) } ?: return@body
            val rhs = it.right ?: return@body
            if (!lhs.isBackingFieldReference()) return@body
            rhs.passToProcessor()
        }
    }

    private fun KtExpression.isBackingFieldReference(): Boolean {
        return this is KtSimpleNameExpression &&
                getReferencedName() == SyntheticFieldDescriptor.NAME.asString() &&
                resolveToCall()?.resultingDescriptor is SyntheticFieldDescriptor
    }

    private fun KtDeclarationWithBody.processBody() {
        val bodyExpression = bodyExpression ?: return
        val pseudocode = pseudocodeCache[bodyExpression] ?: return
        pseudocode.traverse(TraversalOrder.FORWARD) { instr ->
            if (instr is ReturnValueInstruction && instr.subroutine == this) {
                (instr.returnExpressionIfAny?.returnedExpression ?: instr.element as? KtExpression)?.passToProcessorAsValue()
            }
        }
    }

    private fun Instruction.passInputsToProcessor() {
        inputValues.forEach {
            if (it.createdAt != null) {
                it.element?.passToProcessorAsValue()
            }
        }
    }

    override fun processCalls(callable: KtCallableDeclaration, includeOverriders: Boolean, sliceProducer: SliceProducer) {
        if (callable is KtNamedFunction) {
            val (newMode, callElement) = mode.popInlineFunctionCall(callable)
            if (newMode != null && callElement != null) {
                val sliceUsage = KotlinSliceUsage(callElement, parentUsage, newMode, false)
                sliceProducer.produceAndProcess(sliceUsage, newMode, parentUsage, processor)
                return
            }
        }

        super.processCalls(callable, includeOverriders, sliceProducer)
    }

    private fun KtFunctionLiteral.implicitItUsages(): Collection<KtSimpleNameExpression> {
        return collectDescendantsOfType(fun(expression: KtSimpleNameExpression): Boolean {
            if (expression.getQualifiedExpressionForSelector() != null || expression.getReferencedName() != "it") return false
            val lBrace = KotlinTargetElementEvaluator.findLambdaOpenLBraceForGeneratedIt(expression.mainReference) ?: return false
            return lBrace == this.lBrace.node.treeNext.psi
        })
    }
}
