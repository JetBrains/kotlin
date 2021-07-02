/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.compiler.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderersScripting
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import org.jetbrains.kotlin.scripting.ide_services.compiler.completion
import org.jetbrains.kotlin.scripting.ide_services.compiler.filterOutShadowedDescriptors
import org.jetbrains.kotlin.scripting.ide_services.compiler.nameFilter
import org.jetbrains.kotlin.scripting.resolve.classId
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.isFlexible
import java.io.File
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCodeCompletionVariant

fun getKJvmCompletion(
    ktScript: KtFile,
    bindingContext: BindingContext,
    resolutionFacade: KotlinResolutionFacadeForRepl,
    moduleDescriptor: ModuleDescriptor,
    cursor: Int,
    configuration: ScriptCompilationConfiguration
) = KJvmReplCompleter(
    ktScript,
    bindingContext,
    resolutionFacade,
    moduleDescriptor,
    cursor,
    configuration
).getCompletion()

// Insert a constant string right after a cursor position to make this identifiable as a simple reference
// For example, code line
//   import java.
//               ^
// is converted to
//   import java.ABCDEF
// and it makes token after dot (for which reference variants are looked) discoverable in PSI
fun prepareCodeForCompletion(code: String, cursor: Int) =
    code.substring(0, cursor) + KJvmReplCompleter.INSERTED_STRING + code.substring(cursor)

private inline fun <reified T> PsiElement.thisOrParent() = when {
    this is T -> this
    this.parent is T -> (this.parent as T)
    else -> null
}

private class KJvmReplCompleter(
    private val ktScript: KtFile,
    private val bindingContext: BindingContext,
    private val resolutionFacade: KotlinResolutionFacadeForRepl,
    private val moduleDescriptor: ModuleDescriptor,
    private val cursor: Int,
    private val configuration: ScriptCompilationConfiguration
) {

    private fun getElementAt(cursorPos: Int): PsiElement? {
        var element: PsiElement? = ktScript.findElementAt(cursorPos)
        while (element !is KtExpression && element != null) {
            element = element.parent
        }
        return element
    }

    private val getDescriptorsQualified = ResultGetter { element, options ->
        val expression = element.thisOrParent<KtQualifiedExpression>() ?: return@ResultGetter null

        val receiverExpression = expression.receiverExpression
        val expressionType = bindingContext.get(
            BindingContext.EXPRESSION_TYPE_INFO,
            receiverExpression
        )?.type

        DescriptorsResult(targetElement = expression).apply {
            if (expressionType != null) {
                sortNeeded = false
                descriptors.addAll(
                    getVariantsHelper { true }
                        .getReferenceVariants(
                            receiverExpression,
                            CallTypeAndReceiver.DOT(receiverExpression),
                            DescriptorKindFilter.ALL,
                            ALL_NAME_FILTER,
                            filterOutShadowed = options.filterOutShadowedDescriptors,
                        )
                )
            }
        }
    }

    private val getDescriptorsSimple = ResultGetter { element, options ->
        val expression = element.thisOrParent<KtSimpleNameExpression>() ?: return@ResultGetter null

        val result = DescriptorsResult(targetElement = expression)
        val inDescriptor: DeclarationDescriptor = expression.getResolutionScope(bindingContext, resolutionFacade).ownerDescriptor
        val prefix = element.text.substring(0, cursor - element.startOffset)

        val elementParent = element.parent
        if (prefix.isEmpty() && elementParent is KtBinaryExpression) {
            val parentChildren = elementParent.children
            if (parentChildren.size == 3 &&
                parentChildren[1] is KtOperationReferenceExpression &&
                parentChildren[1].text == INSERTED_STRING
            ) return@ResultGetter result
        }

        val containingArgument = expression.thisOrParent<KtValueArgument>()
        val containingCall = containingArgument?.getParentOfType<KtCallExpression>(true)
        val containingQualifiedExpression = containingCall?.parent as? KtDotQualifiedExpression
        val containingCallId = containingCall?.calleeExpression?.text
        fun Name.test(checkAgainstContainingCall: Boolean): Boolean {
            if (isSpecial) return false
            if (options.nameFilter(identifier, prefix)) return true
            return checkAgainstContainingCall && containingCallId?.let { options.nameFilter(identifier, it) } == true
        }

        DescriptorsResult(targetElement = element).apply {
            sortNeeded = false

            descriptors.apply {
                fun addParameters(descriptor: DeclarationDescriptor) {
                    if (containingCallId == descriptor.name.identifier) {
                        val params = when (descriptor) {
                            is CallableDescriptor -> descriptor.valueParameters
                            is ClassDescriptor -> descriptor.constructors.flatMap { it.valueParameters }
                            else -> emptyList()
                        }
                        val valueParams = params.filter { it.name.test(false) }
                        addAll(valueParams)
                        containingCallParameters.addAll(valueParams)
                    }
                }

                getVariantsHelper(
                    VisibilityFilter(inDescriptor)
                ).getReferenceVariants(
                    expression,
                    DescriptorKindFilter.ALL,
                    { it.test(true) },
                    filterOutJavaGettersAndSetters = true,
                    filterOutShadowed = options.filterOutShadowedDescriptors, // setting to true makes it slower up to 4 times
                    excludeNonInitializedVariable = true,
                    useReceiverType = null
                ).forEach { descriptor ->
                    if (descriptor.name.test(false)) add(descriptor)
                    addParameters(descriptor)
                }

                if (containingQualifiedExpression != null) {
                    val receiverExpression = containingQualifiedExpression.receiverExpression
                    getVariantsHelper { true }
                        .getReferenceVariants(
                            receiverExpression,
                            CallTypeAndReceiver.DOT(receiverExpression),
                            DescriptorKindFilter.CALLABLES,
                            ALL_NAME_FILTER,
                            filterOutShadowed = options.filterOutShadowedDescriptors,
                        )
                        .forEach { descriptor ->
                            addParameters(descriptor)
                        }
                }
            }
        }
    }

    private val getDescriptorsString = ResultGetter { element, _ ->
        if (element !is KtStringTemplateExpression) return@ResultGetter null

        val stringVal = element.entries.joinToString("") {
            val t = it.text
            if (it.startOffset <= cursor && cursor <= it.endOffset) {
                val s = cursor - it.startOffset
                val e = s + INSERTED_STRING.length
                t.substring(0, s) + t.substring(e)
            } else t
        }

        val separatorIndex = stringVal.lastIndexOfAny(charArrayOf('/', '\\'))
        val dir = if (separatorIndex != -1) {
            stringVal.substring(0, separatorIndex + 1)
        } else {
            "."
        }
        val namePrefix = stringVal.substring(separatorIndex + 1)

        val file = File(dir)
        DescriptorsResult(targetElement = element).also { result ->
            result.variants = sequence {
                file.listFiles { p, f -> p == file && f.startsWith(namePrefix, true) }?.forEach {
                    yield(SourceCodeCompletionVariant(it.name, it.name, "file", "file"))
                }
            }
        }
    }

    private val getDescriptorsDefault = ResultGetter { element, _ ->
        val resolutionScope = bindingContext.get(
            BindingContext.LEXICAL_SCOPE,
            element as KtExpression?
        )
        DescriptorsResult(targetElement = element).also { result ->
            resolutionScope?.getContributedDescriptors(
                DescriptorKindFilter.ALL,
                ALL_NAME_FILTER
            )?.let { descriptors ->
                result.descriptors.addAll(descriptors)
            }
        }
    }

    private fun renderResult(
        element: PsiElement,
        options: DescriptorsOptions,
        result: DescriptorsResult?
    ): Sequence<SourceCodeCompletionVariant> {
        if (result == null) return emptySequence()
        result.variants?.let { return it }

        with(result) {
            val prefixEnd = cursor - targetElement.startOffset
            var prefix = targetElement.text.substring(0, prefixEnd)

            val cursorWithinElement = cursor - element.startOffset
            val dotIndex = prefix.lastIndexOf('.', cursorWithinElement)

            prefix = if (dotIndex >= 0) {
                prefix.substring(dotIndex + 1, cursorWithinElement)
            } else {
                prefix.substring(0, cursorWithinElement)
            }

            return sequence {
                descriptors
                    .map {
                        val presentation =
                            getPresentation(
                                it, result.containingCallParameters
                            )
                        Triple(it, presentation, (presentation.presentableText + presentation.tailText).lowercase())
                    }
                    .let {
                        if (sortNeeded) it.sortedBy { descTriple -> descTriple.third } else it
                    }
                    .forEach { resultTriple ->
                        val descriptor = resultTriple.first
                        val (rawName, presentableText, tailText, completionText) = resultTriple.second
                        if (options.nameFilter(rawName, prefix)) {
                            val fullName: String =
                                formatName(
                                    presentableText
                                )
                            val deprecationLevel = descriptor.annotations
                                .findAnnotation(FqName("kotlin.Deprecated"))
                                ?.let { annotationDescriptor ->
                                    val valuePair = annotationDescriptor.argumentValue("level")?.value as? Pair<*, *>
                                    val valueClass = (valuePair?.first as? ClassId)?.takeIf { DeprecationLevel::class.classId == it }
                                    val valueName = (valuePair?.second as? Name)?.identifier
                                    if (valueClass == null || valueName == null) return@let DeprecationLevel.WARNING
                                    DeprecationLevel.valueOf(valueName)
                                }
                            yield(
                                SourceCodeCompletionVariant(
                                    completionText,
                                    fullName,
                                    tailText,
                                    getIconFromDescriptor(
                                        descriptor
                                    ),
                                    deprecationLevel,
                                )
                            )
                        }
                    }

                yieldAll(
                    keywordsCompletionVariants(
                        KtTokens.KEYWORDS,
                        prefix
                    )
                )
                yieldAll(
                    keywordsCompletionVariants(
                        KtTokens.SOFT_KEYWORDS,
                        prefix
                    )
                )
            }
        }
    }

    fun getCompletion(): Sequence<SourceCodeCompletionVariant> {
        val filterOutShadowedDescriptors = configuration[ScriptCompilationConfiguration.completion.filterOutShadowedDescriptors]!!
        val nameFilter = configuration[ScriptCompilationConfiguration.completion.nameFilter]!!
        val options = DescriptorsOptions(
            nameFilter, filterOutShadowedDescriptors
        )

        val element = getElementAt(cursor) ?: return emptySequence()

        val descriptorsGetters = listOf(
            getDescriptorsSimple,
            getDescriptorsString,
            getDescriptorsQualified,
            getDescriptorsDefault,
        )

        val result = descriptorsGetters.firstNotNullOfOrNull { it.get(element, options) }
        return renderResult(element, options, result)
    }

    private fun getVariantsHelper(visibilityFilter: (DeclarationDescriptor) -> Boolean) = ReferenceVariantsHelper(
        bindingContext,
        resolutionFacade,
        moduleDescriptor,
        visibilityFilter,
    )

    private fun interface ResultGetter {
        fun get(element: PsiElement, options: DescriptorsOptions): DescriptorsResult?
    }

    private class DescriptorsResult(
        val descriptors: MutableList<DeclarationDescriptor> = mutableListOf(),
        var variants: Sequence<SourceCodeCompletionVariant>? = null,
        var sortNeeded: Boolean = true,
        var targetElement: PsiElement,
        val containingCallParameters: MutableList<ValueParameterDescriptor> = mutableListOf(),
    )

    private class DescriptorsOptions(
        val nameFilter: (String, String) -> Boolean,
        val filterOutShadowedDescriptors: Boolean,
    )

    private class VisibilityFilter(
        private val inDescriptor: DeclarationDescriptor
    ) : (DeclarationDescriptor) -> Boolean {
        override fun invoke(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor is TypeParameterDescriptor && !isTypeParameterVisible(descriptor)) return false

            if (descriptor is DeclarationDescriptorWithVisibility) {
                return try {
                    descriptor.visibility.isVisible(null, descriptor, inDescriptor)
                } catch (e: IllegalStateException) {
                    true
                }
            }

            return true
        }

        private fun isTypeParameterVisible(typeParameter: TypeParameterDescriptor): Boolean {
            val owner = typeParameter.containingDeclaration
            var parent: DeclarationDescriptor? = inDescriptor
            while (parent != null) {
                if (parent == owner) return true
                if (parent is ClassDescriptor && !parent.isInner) return false
                parent = parent.containingDeclaration
            }
            return true
        }
    }

    companion object {
        const val INSERTED_STRING = "ABCDEF"
        private const val NUMBER_OF_CHAR_IN_COMPLETION_NAME = 40

        private fun keywordsCompletionVariants(
            keywords: TokenSet,
            prefix: String
        ) = sequence {
            keywords.types.forEach {
                val token = (it as KtKeywordToken).value
                if (token.startsWith(prefix)) yield(
                    SourceCodeCompletionVariant(
                        token,
                        token,
                        "keyword",
                        "keyword"
                    )
                )
            }
        }

        private val RENDERER =
            IdeDescriptorRenderersScripting.SOURCE_CODE.withOptions {
                this.classifierNamePolicy =
                    ClassifierNamePolicy.SHORT
                this.typeNormalizer =
                    IdeDescriptorRenderersScripting.APPROXIMATE_FLEXIBLE_TYPES
                this.parameterNameRenderingPolicy =
                    ParameterNameRenderingPolicy.NONE
                this.renderDefaultAnnotationArguments = false
                this.typeNormalizer = lambda@{ kotlinType: KotlinType ->
                    if (kotlinType.isFlexible()) {
                        return@lambda kotlinType.asFlexibleType().upperBound
                    }
                    kotlinType
                }
            }

        private fun getIconFromDescriptor(descriptor: DeclarationDescriptor): String = when (descriptor) {
            is FunctionDescriptor -> "method"
            is PropertyDescriptor -> "property"
            is LocalVariableDescriptor -> "property"
            is ClassDescriptor -> "class"
            is PackageFragmentDescriptor -> "package"
            is PackageViewDescriptor -> "package"
            is ValueParameterDescriptor -> "parameter"
            is TypeParameterDescriptorImpl -> "class"
            else -> ""
        }

        private fun formatName(builder: String, symbols: Int = NUMBER_OF_CHAR_IN_COMPLETION_NAME): String {
            return if (builder.length > symbols) {
                builder.substring(0, symbols) + "..."
            } else builder
        }

        data class DescriptorPresentation(
            val rawName: String,
            val presentableText: String,
            val tailText: String,
            val completionText: String
        )

        fun getPresentation(
            descriptor: DeclarationDescriptor,
            callParameters: Collection<ValueParameterDescriptor>
        ): DescriptorPresentation {
            val rawDescriptorName = descriptor.name.asString()
            val descriptorName = rawDescriptorName.quoteIfNeeded()
            var presentableText = descriptorName
            var typeText = ""
            var tailText = ""
            var completionText = ""
            if (descriptor is FunctionDescriptor) {
                val returnType = descriptor.returnType
                typeText =
                    if (returnType != null) RENDERER.renderType(returnType) else ""
                presentableText += RENDERER.renderFunctionParameters(
                    descriptor
                )
                val parameters = descriptor.valueParameters
                if (parameters.size == 1 && parameters.first().type.isFunctionType)
                    completionText = "$descriptorName { "

                val extensionFunction = descriptor.extensionReceiverParameter != null
                val containingDeclaration = descriptor.containingDeclaration
                if (extensionFunction) {
                    tailText += " for " + RENDERER.renderType(
                        descriptor.extensionReceiverParameter!!.type
                    )
                    tailText += " in " + DescriptorUtils.getFqName(containingDeclaration)
                }
            } else if (descriptor is VariableDescriptor) {
                val outType =
                    descriptor.type
                typeText = RENDERER.renderType(outType)
                if (
                    descriptor is ValueParameterDescriptor &&
                    callParameters.contains(descriptor)
                ) {
                    completionText = "$rawDescriptorName = "
                }
            } else if (descriptor is ClassDescriptor) {
                val declaredIn = descriptor.containingDeclaration
                tailText = " (" + DescriptorUtils.getFqName(declaredIn) + ")"
            } else {
                typeText = RENDERER.render(descriptor)
            }

            tailText = typeText.ifEmpty { tailText }

            if (completionText.isEmpty()) {
                completionText = presentableText
                var position = completionText.indexOf('(')
                if (position != -1) { //If this is a string with a package after
                    if (completionText[position - 1] == ' ') {
                        position -= 2
                    }
                    //if this is a method without args
                    if (completionText[position + 1] == ')') {
                        position++
                    }
                    completionText = completionText.substring(0, position + 1)
                }
                position = completionText.indexOf(":")
                if (position != -1) {
                    completionText = completionText.substring(0, position - 1)
                }
            }

            return DescriptorPresentation(
                rawDescriptorName,
                presentableText,
                tailText,
                completionText
            )
        }
    }
}
