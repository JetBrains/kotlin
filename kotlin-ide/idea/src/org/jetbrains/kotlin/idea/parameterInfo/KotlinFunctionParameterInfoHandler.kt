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

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.completion.canBeUsedWithoutNameInCall
import org.jetbrains.kotlin.idea.core.OptionalParametersHelper
import org.jetbrains.kotlin.idea.core.resolveCandidates
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.util.ShadowedDeclarationsFilter
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.load.java.sam.SamAdapterDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.containsError
import org.jetbrains.kotlin.utils.checkWithAttachment
import java.awt.Color
import java.util.*
import kotlin.reflect.KClass

class KotlinFunctionParameterInfoHandler :
    KotlinParameterInfoWithCallHandlerBase<KtValueArgumentList, KtValueArgument>(KtValueArgumentList::class, KtValueArgument::class) {

    override fun getActualParameters(arguments: KtValueArgumentList) = arguments.arguments.toTypedArray()

    override fun getActualParametersRBraceType(): KtSingleValueToken = KtTokens.RPAR

    override fun getArgumentListAllowedParentClasses() = setOf(KtCallElement::class.java)
}

class KotlinLambdaParameterInfoHandler :
    KotlinParameterInfoWithCallHandlerBase<KtLambdaArgument, KtLambdaArgument>(KtLambdaArgument::class, KtLambdaArgument::class) {

    override fun getActualParameters(lambdaArgument: KtLambdaArgument) = arrayOf(lambdaArgument)

    override fun getActualParametersRBraceType(): KtSingleValueToken = KtTokens.RBRACE

    override fun getArgumentListAllowedParentClasses() = setOf(KtLambdaArgument::class.java)

    override fun getParameterIndex(context: UpdateParameterInfoContext, argumentList: KtLambdaArgument): Int {
        val size = (argumentList.parent as? KtCallElement)?.valueArguments?.size ?: 1
        return size - 1
    }
}

class KotlinArrayAccessParameterInfoHandler :
    KotlinParameterInfoWithCallHandlerBase<KtContainerNode, KtExpression>(KtContainerNode::class, KtExpression::class) {

    override fun getArgumentListAllowedParentClasses() = setOf(KtArrayAccessExpression::class.java)

    override fun getActualParameters(containerNode: KtContainerNode): Array<out KtExpression> =
        containerNode.allChildren.filterIsInstance<KtExpression>().toList().toTypedArray()

    override fun getActualParametersRBraceType(): KtSingleValueToken = KtTokens.RBRACKET
}

abstract class KotlinParameterInfoWithCallHandlerBase<TArgumentList : KtElement, TArgument : KtElement>(
    private val argumentListClass: KClass<TArgumentList>,
    private val argumentClass: KClass<TArgument>
) : ParameterInfoHandlerWithTabActionSupport<TArgumentList, KotlinParameterInfoWithCallHandlerBase.CallInfo, TArgument> {

    companion object {
        @JvmField
        val GREEN_BACKGROUND: Color = JBColor(Color(231, 254, 234), Gray._100)

        val STOP_SEARCH_CLASSES: Set<Class<out KtElement>> = setOf(
            KtNamedFunction::class.java,
            KtVariableDeclaration::class.java,
            KtValueArgumentList::class.java,
            KtLambdaArgument::class.java,
            KtContainerNode::class.java,
            KtTypeArgumentList::class.java
        )

        private val RENDERER = DescriptorRenderer.SHORT_NAMES_IN_TYPES.withOptions {
            enhancedTypes = true
            renderUnabbreviatedType = false
        }
    }

    private fun findCall(argumentList: TArgumentList, bindingContext: BindingContext): Call? {
        return (argumentList.parent as? KtElement)?.getCall(bindingContext)
    }

    override fun getActualParameterDelimiterType(): KtSingleValueToken = KtTokens.COMMA

    override fun getArgListStopSearchClasses(): Set<Class<out KtElement>> = STOP_SEARCH_CLASSES

    override fun getArgumentListClass() = argumentListClass.java

    override fun showParameterInfo(element: TArgumentList, context: CreateParameterInfoContext) {
        context.showHint(element, element.textRange.startOffset, this)
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): TArgumentList? {
        val element = context.file.findElementAt(context.offset) ?: return null
        val argumentList = PsiTreeUtil.getParentOfType(element, argumentListClass.java) ?: return null
        val argument = element.parents.takeWhile { it != argumentList }.lastOrNull()
        if (argument != null && !argumentClass.java.isInstance(argument)) {
            val arguments = getActualParameters(argumentList)
            val index = arguments.indexOf(element)
            context.setCurrentParameter(index)
            context.highlightedParameter = element
        }
        return argumentList
    }

    override fun findElementForParameterInfo(context: CreateParameterInfoContext): TArgumentList? {
        //todo: calls to this constructors, when we will have auxiliary constructors
        val file = context.file as? KtFile ?: return null

        val token = file.findElementAt(context.offset) ?: return null
        val argumentList = PsiTreeUtil.getParentOfType(token, argumentListClass.java, true, *STOP_SEARCH_CLASSES.toTypedArray())
            ?: return null

        val bindingContext = argumentList.analyze(BodyResolveMode.PARTIAL)
        val call = findCall(argumentList, bindingContext) ?: return null

        val resolutionFacade = file.getResolutionFacade()
        val candidates =
            call.resolveCandidates(bindingContext, resolutionFacade)
                .map { it.resultingDescriptor }
                .distinctBy { it.original }

        val shadowedDeclarationsFilter = ShadowedDeclarationsFilter(
            bindingContext,
            resolutionFacade,
            call.callElement,
            call.explicitReceiver as? ReceiverValue
        )

        context.itemsToShow = shadowedDeclarationsFilter.filter(candidates).map { CallInfo(it) }.toTypedArray()
        return argumentList
    }

    override fun updateParameterInfo(argumentList: TArgumentList, context: UpdateParameterInfoContext) {
        if (context.parameterOwner !== argumentList) {
            context.removeHint()
        }
        val parameterIndex = getParameterIndex(context, argumentList)
        context.setCurrentParameter(parameterIndex)

        runReadAction {
            val bindingContext = argumentList.analyze(BodyResolveMode.PARTIAL)
            val call = findCall(argumentList, bindingContext) ?: return@runReadAction
            val resolutionFacade = argumentList.getResolutionFacade()

            context.objectsToView.forEach { resolveCallInfo(it as CallInfo, call, bindingContext, resolutionFacade) }
        }
    }

    protected open fun getParameterIndex(context: UpdateParameterInfoContext, argumentList: TArgumentList): Int {
        val offset = context.offset
        return argumentList.allChildren
            .takeWhile { it.startOffset < offset }
            .count { it.node.elementType == KtTokens.COMMA }
    }

    override fun updateUI(itemToShow: CallInfo, context: ParameterInfoUIContext) {
        if (!updateUIOrFail(itemToShow, context)) {
            context.isUIComponentEnabled = false
            return
        }
    }

    private fun updateUIOrFail(itemToShow: CallInfo, context: ParameterInfoUIContext): Boolean {
        if (context.parameterOwner == null || !context.parameterOwner.isValid) return false
        if (!argumentListClass.java.isInstance(context.parameterOwner)) return false
        val call = itemToShow.call ?: return false

        val supportsMixedNamedArgumentsInTheirOwnPosition =
            call.callElement.languageVersionSettings.supportsFeature(LanguageFeature.MixedNamedArgumentsInTheirOwnPosition)

        @Suppress("UNCHECKED_CAST")
        val argumentList = context.parameterOwner as TArgumentList

        val currentArgumentIndex = context.currentParameterIndex
        if (currentArgumentIndex < 0) return false // by some strange reason we are invoked with currentParameterIndex == -1 during initialization

        val project = argumentList.project

        val (substitutedDescriptor, argumentToParameter, highlightParameterIndex, isGrey) = matchCallWithSignature(
            itemToShow, currentArgumentIndex
        ) ?: return false

        var boldStartOffset = -1
        var boldEndOffset = -1
        var disabledBeforeHighlight = false
        val text = buildString {
            val usedParameterIndices = HashSet<Int>()
            var namedMode = false
            var argumentIndex = 0

            if (call.callType == Call.CallType.ARRAY_SET_METHOD) {
                // for set-operator the last parameter is used for the value assigned
                usedParameterIndices.add(substitutedDescriptor.valueParameters.lastIndex)
            }

            val includeParameterNames = !substitutedDescriptor.hasSynthesizedParameterNames()

            fun appendParameter(
                parameter: ValueParameterDescriptor,
                named: Boolean = false,
                markUsedUnusedParameterBorder: Boolean = false
            ) {
                argumentIndex++

                if (length > 0) {
                    append(", ")
                    if (markUsedUnusedParameterBorder) {
                        // mark the space after the comma as bold; bold text needs to be at least one character long
                        boldStartOffset = length - 1
                        boldEndOffset = length
                        disabledBeforeHighlight = true
                    }
                }

                val highlightParameter = parameter.index == highlightParameterIndex
                if (highlightParameter) {
                    boldStartOffset = length
                }

                append(renderParameter(parameter, includeParameterNames, named || namedMode, project))

                if (highlightParameter) {
                    boldEndOffset = length
                }
            }

            for (argument in call.valueArguments) {
                if (argument is LambdaArgument) continue
                val parameter = argumentToParameter(argument) ?: continue
                if (!usedParameterIndices.add(parameter.index)) continue

                if (argument.isNamed() &&
                    !(supportsMixedNamedArgumentsInTheirOwnPosition && argument.canBeUsedWithoutNameInCall(itemToShow))
                ) {
                    namedMode = true
                }

                appendParameter(parameter, argument.isNamed())
            }

            for (parameter in substitutedDescriptor.valueParameters) {
                if (parameter.index !in usedParameterIndices) {
                    if (argumentIndex != parameter.index) {
                        namedMode = true
                    }
                    appendParameter(parameter, markUsedUnusedParameterBorder = highlightParameterIndex == null && boldStartOffset == -1)
                }
            }

            if (length == 0) {
                append(CodeInsightBundle.message("parameter.info.no.parameters"))
            }
        }


        val color = if (itemToShow.isResolvedToDescriptor) GREEN_BACKGROUND else context.defaultParameterColor

        context.setupUIComponentPresentation(
            text,
            boldStartOffset,
            boldEndOffset,
            isGrey,
            itemToShow.isDeprecatedAtCallSite,
            disabledBeforeHighlight,
            color
        )

        return true
    }

    //TODO
    override fun couldShowInLookup() = false

    override fun getParametersForLookup(item: LookupElement, context: ParameterInfoContext) = emptyArray<Any>()

    private fun renderParameter(parameter: ValueParameterDescriptor, includeName: Boolean, named: Boolean, project: Project): String {
        return buildString {
            if (named) append("[")

            parameter
                .annotations
                .filterNot { it.fqName in NULLABILITY_ANNOTATIONS }
                .forEach {
                    it.fqName?.let { fqName -> append("@${fqName.shortName().asString()} ") }
                }

            if (parameter.varargElementType != null) {
                append("vararg ")
            }

            if (includeName) {
                append(parameter.name)
                append(": ")
            }

            append(RENDERER.renderType(parameterTypeToRender(parameter)))

            if (parameter.hasDefaultValue()) {
                append(" = ")
                append(parameter.renderDefaultValue(project))
            }

            if (named) append("]")
        }
    }

    private fun ValueParameterDescriptor.renderDefaultValue(project: Project): String {
        val expression = OptionalParametersHelper.defaultParameterValueExpression(this, project)
        if (expression != null) {
            val text = expression.text
            if (text.length <= 32) {
                return text
            }

            if (expression is KtConstantExpression || expression is KtStringTemplateExpression) {
                if (text.startsWith("\"")) {
                    return "\"...\""
                } else if (text.startsWith("\'")) {
                    return "\'...\'"
                }
            }
        }
        return "..."
    }

    private fun parameterTypeToRender(descriptor: ValueParameterDescriptor): KotlinType {
        var type = descriptor.varargElementType ?: descriptor.type
        if (type.containsError()) {
            val original = descriptor.original
            type = original.varargElementType ?: original.type
        }
        return type
    }

    private fun isResolvedToDescriptor(
        call: Call,
        functionDescriptor: FunctionDescriptor,
        bindingContext: BindingContext
    ): Boolean {
        val target = call.getResolvedCall(bindingContext)?.resultingDescriptor as? FunctionDescriptor
        return target != null && descriptorsEqual(target, functionDescriptor)
    }

    private data class SignatureInfo(
        val substitutedDescriptor: FunctionDescriptor,
        val argumentToParameter: (ValueArgument) -> ValueParameterDescriptor?,
        val highlightParameterIndex: Int?,
        val isGrey: Boolean
    )

    data class CallInfo(
        val overload: FunctionDescriptor? = null,
        var call: Call? = null,
        var resolvedCall: ResolvedCall<FunctionDescriptor>? = null,
        var arguments: List<ValueArgument> = emptyList(),
        var dummyArgument: ValueArgument? = null,
        var dummyResolvedCall: ResolvedCall<FunctionDescriptor>? = null,
        var isResolvedToDescriptor: Boolean = false,
        var isGreyArgumentIndex: Int = -1,
        var isDeprecatedAtCallSite: Boolean = false
    )

    private fun resolveCallInfo(
        info: CallInfo,
        call: Call,
        bindingContext: BindingContext,
        resolutionFacade: ResolutionFacade
    ) {
        val overload = info.overload ?: return
        val isArraySetMethod = call.callType == Call.CallType.ARRAY_SET_METHOD

        fun calculateArgument(c: Call) = c.valueArguments.let { args ->
            // For array set method call, we're only interested in the arguments in brackets which are all except the last one
            if (c.callType == Call.CallType.ARRAY_SET_METHOD) args.dropLast(1) else args
        }

        val arguments = calculateArgument(call)

        val resolvedCall = resolvedCall(call, bindingContext, resolutionFacade, overload) ?: return

        // add dummy current argument if we don't have one
        val dummyArgument = object : ValueArgument {
            override fun getArgumentExpression(): KtExpression? = null
            override fun getArgumentName(): ValueArgumentName? = null
            override fun isNamed(): Boolean = false
            override fun asElement(): KtElement = call.callElement // is a hack but what to do?
            override fun getSpreadElement(): LeafPsiElement? = null
            override fun isExternal() = false
        }

        val dummyResolvedCall =
            dummyResolvedCall(call, arguments, dummyArgument, isArraySetMethod, bindingContext, resolutionFacade, overload)

        val resultingDescriptor = resolvedCall.resultingDescriptor

        val resolvedToDescriptor = isResolvedToDescriptor(call, resultingDescriptor, bindingContext)

        // grey out if not all arguments are matched
        val isGreyArgumentIndex = arguments.indexOfFirst { argument ->
            resolvedCall.getArgumentMapping(argument).isError() &&
                    !argument.hasError(bindingContext) /* ignore arguments that have error type */
        }

        val isDeprecated = resolutionFacade.frontendService<DeprecationResolver>().getDeprecations(resultingDescriptor).isNotEmpty()

        with(info) {
            this.call = call
            this.resolvedCall = resolvedCall
            this.arguments = arguments
            this.dummyArgument = dummyArgument
            this.dummyResolvedCall = dummyResolvedCall
            this.isResolvedToDescriptor = resolvedToDescriptor
            this.isGreyArgumentIndex = isGreyArgumentIndex
            this.isDeprecatedAtCallSite = isDeprecated
        }
    }

    private fun dummyResolvedCall(
        call: Call,
        arguments: List<ValueArgument>,
        dummyArgument: ValueArgument,
        isArraySetMethod: Boolean,
        bindingContext: BindingContext,
        resolutionFacade: ResolutionFacade,
        overload: FunctionDescriptor
    ): ResolvedCall<FunctionDescriptor>? {
        val callToUse = object : DelegatingCall(call) {
            val argumentsWithCurrent =
                arguments + dummyArgument +
                        // For array set method call, also add the argument in the right-hand side
                        (if (isArraySetMethod) listOf(call.valueArguments.last()) else listOf())

            override fun getValueArguments() = argumentsWithCurrent
            override fun getFunctionLiteralArguments() = emptyList<LambdaArgument>()
            override fun getValueArgumentList(): KtValueArgumentList? = null
        }

        return resolvedCall(callToUse, bindingContext, resolutionFacade, overload)
    }

    private fun resolvedCall(
        call: Call,
        bindingContext: BindingContext,
        resolutionFacade: ResolutionFacade,
        overload: FunctionDescriptor
    ): ResolvedCall<FunctionDescriptor>? {
        val candidates = call.resolveCandidates(bindingContext, resolutionFacade)

        // First try to find strictly matching descriptor, then one with the same declaration.
        // The second way is needed for the case when the descriptor was invalidated and new one has been built.
        // See testLocalFunctionBug().

        return candidates.firstOrNull { it.resultingDescriptor.original == overload.original }
            ?: candidates.firstOrNull { descriptorsEqual(it.resultingDescriptor, overload) }
            ?: null
    }

    private fun matchCallWithSignature(
        info: CallInfo,
        currentArgumentIndex: Int
    ): SignatureInfo? {
        val call = info.call ?: return null
        val resolvedCall = info.resolvedCall ?: return null
        if (currentArgumentIndex == 0 && call.valueArguments.isEmpty() && resolvedCall.resultingDescriptor.valueParameters.isEmpty()) {
            return SignatureInfo(resolvedCall.resultingDescriptor, { null }, null, isGrey = false)
        }

        val arguments = info.arguments

        checkWithAttachment(
            arguments.size >= currentArgumentIndex,
            lazyMessage = { "currentArgumentIndex: $currentArgumentIndex has to be not more than number of arguments ${arguments.size}" },
            attachments = {
                it.withAttachment("info.txt", info)
            }
        )

        val callToUse: ResolvedCall<FunctionDescriptor>
        val currentArgument = if (arguments.size > currentArgumentIndex) {
            callToUse = resolvedCall
            arguments[currentArgumentIndex]
        } else {
            callToUse = info.dummyResolvedCall ?: return null
            info.dummyArgument ?: return null
        }

        val resultingDescriptor = callToUse.resultingDescriptor

        fun argumentToParameter(argument: ValueArgument): ValueParameterDescriptor? {
            return (callToUse.getArgumentMapping(argument) as? ArgumentMatch)?.valueParameter
        }

        val currentParameter = argumentToParameter(currentArgument)
        val highlightParameterIndex = currentParameter?.index

        val argumentsBeforeCurrent = arguments.subList(0, currentArgumentIndex)
        if (argumentsBeforeCurrent.any { argumentToParameter(it) == null }) {
            // some of arguments before the current one are not mapped to any of the parameters
            return SignatureInfo(resultingDescriptor, ::argumentToParameter, highlightParameterIndex, isGrey = true)
        }

        if (currentParameter == null) {
            if (currentArgumentIndex < arguments.lastIndex) {
                // the current argument is not the last one and it is not mapped to any of the parameters
                return SignatureInfo(resultingDescriptor, ::argumentToParameter, highlightParameterIndex, isGrey = true)
            }

            val usedParameters = argumentsBeforeCurrent.mapNotNull { argumentToParameter(it) }
            val availableParameters = if (call.callType == Call.CallType.ARRAY_SET_METHOD) {
                resultingDescriptor.valueParameters.dropLast(1)
            } else {
                resultingDescriptor.valueParameters
            } 
            val noUnusedParametersLeft = (availableParameters - usedParameters).isEmpty()

            if (currentArgument == info.dummyArgument) {
                val supportsTrailingCommas = call.callElement.languageVersionSettings.supportsFeature(LanguageFeature.TrailingCommas)
                if (!supportsTrailingCommas && noUnusedParametersLeft) {
                    // current argument is empty but there are no unused parameters left and trailing commas are not supported
                    return SignatureInfo(resultingDescriptor, ::argumentToParameter, highlightParameterIndex, isGrey = true)
                }
            } else if (noUnusedParametersLeft) {
                // there are no unused parameters left to which this argument could be matched
                return SignatureInfo(resultingDescriptor, ::argumentToParameter, highlightParameterIndex, isGrey = true)
            }
        }

        // grey out if not all arguments before the current are matched
        val isGrey = info.isGreyArgumentIndex in 0 until currentArgumentIndex
        return SignatureInfo(resultingDescriptor, ::argumentToParameter, highlightParameterIndex, isGrey)
    }

    private fun ValueArgument.hasError(bindingContext: BindingContext) =
        getArgumentExpression()?.let { bindingContext.getType(it) }?.isError ?: true

    private fun ValueArgument.canBeUsedWithoutNameInCall(callInfo: CallInfo) =
        this is KtValueArgument && this.canBeUsedWithoutNameInCall(callInfo.resolvedCall as ResolvedCall<out CallableDescriptor>)

    // we should not compare descriptors directly because partial resolve is involved
    private fun descriptorsEqual(descriptor1: FunctionDescriptor, descriptor2: FunctionDescriptor): Boolean {
        if (descriptor1.original == descriptor2.original) return true
        val isSamDescriptor1 = descriptor1 is SamAdapterDescriptor<*>
        val isSamDescriptor2 = descriptor2 is SamAdapterDescriptor<*>

        // Previously it worked because of different order
        // If descriptor1 is SamAdapter and descriptor2 isn't, this function shouldn't return `true` because of equal declaration
        if (isSamDescriptor1 xor isSamDescriptor2) return false

        val declaration1 = DescriptorToSourceUtils.descriptorToDeclaration(descriptor1) ?: return false
        val declaration2 = DescriptorToSourceUtils.descriptorToDeclaration(descriptor2)
        return declaration1 == declaration2
    }
}
