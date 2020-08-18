/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInliner

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInliner.CommentHolder.CommentNode.Companion.mergeComments
import org.jetbrains.kotlin.idea.codeInliner.CommentHolder.Companion.collectComments
import org.jetbrains.kotlin.idea.core.asExpression
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.InsertExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.intentions.SpecifyExplicitLambdaSignatureIntention
import org.jetbrains.kotlin.idea.references.canBeResolvedViaImport
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.sure
import java.util.*

class CodeToInlineBuilder(
    private val targetCallable: CallableDescriptor,
    private val resolutionFacade: ResolutionFacade
) {
    private val psiFactory = KtPsiFactory(resolutionFacade.project)

    fun prepareCodeToInlineWithAdvancedResolution(
        contextDeclaration: KtDeclaration,
        bodyOrExpression: KtExpression,
        expressionMapper: (bodyOrExpression: KtExpression) -> Pair<KtExpression?, List<KtExpression>>?,
    ): CodeToInline? {
        val (mainExpression, statementsBefore) = expressionMapper(bodyOrExpression) ?: return null
        val codeToInline = prepareMutableCodeToInline(
            mainExpression = mainExpression,
            statementsBefore = statementsBefore,
            analyze = { it.analyze() },
            reformat = true,
            contextDeclaration = contextDeclaration,
        )

        val copyOfBodyOrExpression = bodyOrExpression.copied()
        val (resultMainExpression, resultStatementsBefore) = expressionMapper(copyOfBodyOrExpression) ?: return null
        codeToInline.mainExpression = resultMainExpression
        codeToInline.statementsBefore.clear()
        codeToInline.statementsBefore.addAll(resultStatementsBefore)

        return codeToInline.toNonMutable()
    }

    private fun prepareMutableCodeToInline(
        mainExpression: KtExpression?,
        statementsBefore: List<KtExpression>,
        analyze: (KtExpression) -> BindingContext,
        reformat: Boolean,
        contextDeclaration: KtDeclaration? = null,
    ): MutableCodeToInline {
        val alwaysKeepMainExpression =
            when (val descriptor = mainExpression?.getResolvedCall(analyze(mainExpression))?.resultingDescriptor) {
                is PropertyDescriptor -> descriptor.getter?.isDefault == false
                else -> false
            }

        val codeToInline = MutableCodeToInline(
            mainExpression,
            statementsBefore.toMutableList(),
            mutableSetOf(),
            alwaysKeepMainExpression,
            extraComments = null,
        )

        if (contextDeclaration != null) {
            saveComments(codeToInline, contextDeclaration)
        }

        insertExplicitTypeArguments(codeToInline, analyze)
        processReferences(codeToInline, analyze, reformat)
        removeContracts(codeToInline, analyze)

        when {
            mainExpression == null -> Unit
            mainExpression.isNull() -> targetCallable.returnType?.let { returnType ->
                codeToInline.addPreCommitAction(mainExpression) {
                    codeToInline.replaceExpression(
                        it,
                        psiFactory.createExpression(
                            "null as ${
                                IdeDescriptorRenderers.FQ_NAMES_IN_TYPES_WITH_NORMALIZER.renderType(returnType)
                            }"
                        ),
                    )
                }
            }

            else -> {
                val functionLiteralExpression = mainExpression.unpackFunctionLiteral(true)
                if (functionLiteralExpression != null) {
                    val functionLiteralParameterTypes = getParametersForFunctionLiteral(functionLiteralExpression, analyze)
                    if (functionLiteralParameterTypes != null) {
                        codeToInline.addPostInsertionAction(mainExpression) { inlinedExpression ->
                            addFunctionLiteralParameterTypes(functionLiteralParameterTypes, inlinedExpression)
                        }
                    }
                }
            }
        }

        return codeToInline
    }

    private fun removeContracts(codeToInline: MutableCodeToInline, analyze: (KtExpression) -> BindingContext) {
        for (statement in codeToInline.statementsBefore) {
            val context = analyze(statement)
            if (statement.getResolvedCall(context)?.resultingDescriptor?.fqNameOrNull()?.asString() == "kotlin.contracts.contract") {
                codeToInline.addPreCommitAction(statement) {
                    codeToInline.statementsBefore.remove(it)
                }
            }
        }
    }

    fun prepareCodeToInline(
        mainExpression: KtExpression?,
        statementsBefore: List<KtExpression>,
        analyze: (KtExpression) -> BindingContext,
        reformat: Boolean,
        contextDeclaration: KtDeclaration? = null,
    ): CodeToInline = prepareMutableCodeToInline(mainExpression, statementsBefore, analyze, reformat, contextDeclaration).toNonMutable()

    private fun saveComments(codeToInline: MutableCodeToInline, contextDeclaration: KtDeclaration) {
        val topLevelLeadingComments = contextDeclaration.collectDescendantsOfType<PsiComment>(
            canGoInside = { it == contextDeclaration || it !is KtExpression },
            predicate = { it.parent != contextDeclaration || it.getNextSiblingIgnoringWhitespaceAndComments() != null }
        ).map { CommentHolder.CommentNode.create(it) }

        val topLevelTrailingComments = contextDeclaration.children
            .lastOrNull { it !is PsiComment && it !is PsiWhiteSpace }
            ?.siblings(forward = true, withItself = false)
            ?.collectComments()
            ?: emptyList()

        val bodyBlockExpression = contextDeclaration.safeAs<KtDeclarationWithBody>()?.bodyBlockExpression
        if (bodyBlockExpression != null) {
            addCommentHoldersForStatements(codeToInline, bodyBlockExpression)

            val expressions = codeToInline.expressions
            if (expressions.isEmpty()) {
                codeToInline.addExtraComments(CommentHolder(topLevelLeadingComments, topLevelTrailingComments))
            } else {
                expressions.first().mergeComments(CommentHolder(topLevelLeadingComments, emptyList()))
                codeToInline.addCommentHolderToExpressionOrToThis(
                    codeToInline.mainExpression,
                    CommentHolder(emptyList(), topLevelTrailingComments)
                )
            }
        } else {
            codeToInline.addCommentHolderToExpressionOrToThis(
                codeToInline.mainExpression,
                CommentHolder(topLevelLeadingComments, topLevelTrailingComments)
            )
        }
    }

    private fun MutableCodeToInline.addCommentHolderToExpressionOrToThis(expression: KtExpression?, commentHolder: CommentHolder) {
        expression?.mergeComments(commentHolder) ?: this.addExtraComments(commentHolder)
    }

    private fun addCommentHoldersForStatements(mutableCodeToInline: MutableCodeToInline, blockExpression: KtBlockExpression) {
        val expressions = mutableCodeToInline.expressions

        for ((indexOfIteration, commentHolder) in CommentHolder.extract(blockExpression).withIndex()) {
            if (commentHolder.isEmpty) continue

            if (expressions.isEmpty()) {
                mutableCodeToInline.addExtraComments(commentHolder)
            } else {
                val expression = expressions.elementAtOrNull(indexOfIteration)
                if (expression != null) {
                    expression.mergeComments(commentHolder)
                } else {
                    expressions.last().mergeComments(
                        CommentHolder(emptyList(), trailingComments = commentHolder.leadingComments + commentHolder.trailingComments)
                    )
                }
            }
        }
    }

    private fun getParametersForFunctionLiteral(
        functionLiteralExpression: KtLambdaExpression,
        analyze: (KtExpression) -> BindingContext
    ): String? {
        val context = analyze(functionLiteralExpression)
        val lambdaDescriptor = context.get(BindingContext.FUNCTION, functionLiteralExpression.functionLiteral)
        if (lambdaDescriptor == null ||
            ErrorUtils.containsErrorTypeInParameters(lambdaDescriptor) ||
            ErrorUtils.containsErrorType(lambdaDescriptor.returnType)
        ) return null

        return lambdaDescriptor.valueParameters.joinToString {
            it.name.render() + ": " + IdeDescriptorRenderers.SOURCE_CODE.renderType(it.type)
        }
    }

    private fun addFunctionLiteralParameterTypes(parameters: String, inlinedExpression: KtExpression) {
        val containingFile = inlinedExpression.containingKtFile
        val resolutionFacade = containingFile.getResolutionFacade()
        val lambdaExpr = inlinedExpression.unpackFunctionLiteral(true).sure {
            "can't find function literal expression for " + inlinedExpression.text
        }

        if (!needToAddParameterTypes(lambdaExpr, resolutionFacade)) return
        SpecifyExplicitLambdaSignatureIntention.applyWithParameters(lambdaExpr, parameters)
    }

    private fun needToAddParameterTypes(
        lambdaExpression: KtLambdaExpression,
        resolutionFacade: ResolutionFacade
    ): Boolean {
        val functionLiteral = lambdaExpression.functionLiteral
        val context = resolutionFacade.analyze(lambdaExpression, BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
        return context.diagnostics.any { diagnostic ->
            val factory = diagnostic.factory
            val element = diagnostic.psiElement
            val hasCantInferParameter = factory == Errors.CANNOT_INFER_PARAMETER_TYPE && element.parent.parent == functionLiteral
            val hasUnresolvedItOrThis = factory == Errors.UNRESOLVED_REFERENCE &&
                    element.text == "it" &&
                    element.getStrictParentOfType<KtFunctionLiteral>() == functionLiteral

            hasCantInferParameter || hasUnresolvedItOrThis
        }
    }

    private fun insertExplicitTypeArguments(
        codeToInline: MutableCodeToInline,
        analyze: (KtExpression) -> BindingContext,
    ) = codeToInline.forEachDescendantOfType<KtCallExpression> {
        val bindingContext = analyze(it)
        if (InsertExplicitTypeArgumentsIntention.isApplicableTo(it, bindingContext)) {
            val typeArguments = InsertExplicitTypeArgumentsIntention.createTypeArguments(it, bindingContext)!!
            codeToInline.addPreCommitAction(it) { callExpression ->
                callExpression.addAfter(typeArguments, callExpression.calleeExpression)
            }
        }
    }

    private fun findDescriptorAndContext(
        expression: KtSimpleNameExpression,
        analyzer: (KtExpression) -> BindingContext
    ): Pair<BindingContext, DeclarationDescriptor>? {
        val currentContext = analyzer(expression)
        val descriptor = currentContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, expression]
            ?: currentContext[BindingContext.REFERENCE_TARGET, expression]

        if (descriptor != null) return currentContext to descriptor
        return findCallableDescriptorAndContext(expression, analyzer)
    }

    private fun findCallableDescriptorAndContext(
        expression: KtSimpleNameExpression,
        analyzer: (KtExpression) -> BindingContext
    ): Pair<BindingContext, CallableDescriptor>? {
        val callExpression = expression.parent as? KtCallExpression ?: return null
        val context = analyzer(callExpression)
        return callExpression.getResolvedCall(context)?.resultingDescriptor?.let { context to it }
    }

    private fun findResolvedCall(
        expression: KtSimpleNameExpression,
        bindingContext: BindingContext,
        analyzer: (KtExpression) -> BindingContext,
    ): Pair<KtExpression, ResolvedCall<out CallableDescriptor>>? {
        return getResolvedCallIfReallySuccess(expression, bindingContext)?.let { expression to it }
            ?: expression.parent?.safeAs<KtCallExpression>()?.let { callExpression ->
                getResolvedCallIfReallySuccess(callExpression, analyzer(callExpression))?.let { callExpression to it }
            }
    }

    private fun getResolvedCallIfReallySuccess(
        expression: KtExpression,
        bindingContext: BindingContext
    ): ResolvedCall<out CallableDescriptor>? {
        return expression.getResolvedCall(bindingContext)?.takeIf { it.isReallySuccess() }
    }

    private fun processReferences(codeToInline: MutableCodeToInline, analyze: (KtExpression) -> BindingContext, reformat: Boolean) {
        val targetDispatchReceiverType = targetCallable.dispatchReceiverParameter?.value?.type
        val targetExtensionReceiverType = targetCallable.extensionReceiverParameter?.value?.type

        codeToInline.forEachDescendantOfType<KtSimpleNameExpression> { expression ->
            val parent = expression.parent
            if (parent is KtValueArgumentName || parent is KtCallableReferenceExpression) return@forEachDescendantOfType
            val (bindingContext, target) = findDescriptorAndContext(expression, analyze) ?: return@forEachDescendantOfType

            //TODO: other types of references ('[]' etc)
            if (expression.canBeResolvedViaImport(target, bindingContext)) {
                val importableFqName = target.importableFqName
                if (importableFqName != null) {
                    val lexicalScope = (expression.containingFile as? KtFile)?.getResolutionScope(bindingContext, resolutionFacade)
                    val lookupName = lexicalScope?.findClassifier(importableFqName.shortName(), NoLookupLocation.FROM_IDE)
                        ?.typeConstructor
                        ?.declarationDescriptor
                        ?.fqNameOrNull()

                    codeToInline.fqNamesToImport.add(lookupName ?: importableFqName)
                }
            }

            val callableDescriptor = targetCallable.safeAs<ImportedFromObjectCallableDescriptor<*>>()?.callableFromObject ?: targetCallable
            val receiverExpression = expression.getReceiverExpression()
            if (receiverExpression != null &&
                parent is KtCallExpression &&
                target is ValueParameterDescriptor &&
                target.type.isExtensionFunctionType &&
                target.containingDeclaration == callableDescriptor
            ) {
                codeToInline.addPreCommitAction(parent) { callExpression ->
                    val qualifiedExpression = callExpression.parent as KtDotQualifiedExpression
                    val valueArgumentList = callExpression.getOrCreateValueArgumentList()
                    val newArgument = psiFactory.createArgument(qualifiedExpression.receiverExpression)
                    valueArgumentList.addArgumentBefore(newArgument, valueArgumentList.arguments.firstOrNull())
                    val newExpression = qualifiedExpression.replaced(callExpression)
                    if (qualifiedExpression in codeToInline) {
                        codeToInline.replaceExpression(qualifiedExpression, newExpression)
                    }
                }

                expression.putCopyableUserData(CodeToInline.PARAMETER_USAGE_KEY, target.name)
            } else if (receiverExpression == null) {
                if (target is ValueParameterDescriptor && target.containingDeclaration == callableDescriptor) {
                    expression.putCopyableUserData(CodeToInline.PARAMETER_USAGE_KEY, target.name)
                } else if (target is TypeParameterDescriptor && target.containingDeclaration == callableDescriptor) {
                    expression.putCopyableUserData(CodeToInline.TYPE_PARAMETER_USAGE_KEY, target.name)
                }

                if (targetCallable !is ImportedFromObjectCallableDescriptor<*>) {
                    val (expressionToResolve, resolvedCall) = findResolvedCall(expression, bindingContext, analyze)
                        ?: return@forEachDescendantOfType

                    val receiver = if (resolvedCall.resultingDescriptor.isExtension)
                        resolvedCall.extensionReceiver
                    else
                        resolvedCall.dispatchReceiver

                    if (receiver is ImplicitReceiver) {
                        val resolutionScope = expression.getResolutionScope(bindingContext, resolutionFacade)
                        val receiverExpressionToInline = receiver.asExpression(resolutionScope, psiFactory)
                        if (receiverExpressionToInline != null) {
                            val receiverType = receiver.type
                            codeToInline.addPreCommitAction(expressionToResolve) { expr ->
                                val expressionToReplace = expr.parent as? KtCallExpression ?: expr
                                val replaced = codeToInline.replaceExpression(
                                    expressionToReplace,
                                    psiFactory.createExpressionByPattern(
                                        "$0.$1", receiverExpressionToInline, expressionToReplace,
                                        reformat = reformat
                                    )
                                ) as? KtQualifiedExpression

                                if (receiverType != targetDispatchReceiverType && receiverType != targetExtensionReceiverType) {
                                    replaced?.receiverExpression?.putCopyableUserData(CodeToInline.SIDE_RECEIVER_USAGE_KEY, Unit)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
