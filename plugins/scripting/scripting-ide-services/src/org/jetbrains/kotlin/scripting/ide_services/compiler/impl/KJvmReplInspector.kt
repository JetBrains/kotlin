/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.compiler.impl

import org.jetbrains.kotlin.backend.common.onlyIf
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.scripting.ide_services.compiler.api.DetailsLevel
import org.jetbrains.kotlin.scripting.ide_services.compiler.api.SourceCodeInspectionVariant
import java.io.File
import java.util.*
import kotlin.script.experimental.api.SourceCodeCompletionVariant

fun getKJvmInspections(
    codeText: String,
    ktScript: KtFile,
    bindingContext: BindingContext,
    resolutionFacade: KotlinResolutionFacadeForRepl,
    moduleDescriptor: ModuleDescriptor,
    cursor: Int,
    detailsLevel: DetailsLevel
) = KJvmReplInspector(codeText, ktScript, bindingContext, resolutionFacade, moduleDescriptor, cursor, detailsLevel).getInspections()

private class KJvmReplInspector(
    private val codeText: String,
    private val ktScript: KtFile,
    private val bindingContext: BindingContext,
    private val resolutionFacade: KotlinResolutionFacadeForRepl,
    private val moduleDescriptor: ModuleDescriptor,
    private val cursor: Int,
    private val detailsLevel: DetailsLevel
) {

//    val cursor = {
//        if (cursor >= codeText.length)
//            codeText.length - 1
//        else if (cursor == 0)
//            0
//        else {
//            val beforeCur = codeText[cursor - 1]
//            val afterCur = codeText[cursor]
//
//        }
//    }()

    fun getInspections() = sequence<SourceCodeInspectionVariant> gen@{
        val element = ktScript.getElementAt(cursor) ?: ktScript.getElementAt(cursor - 1) ?: return@gen
        val elementText = element.text
        val nameFilter = { name: Name -> !name.isSpecial && name.identifier == elementText }

        var descriptors: Collection<DeclarationDescriptor>? = null
        var isSortNeeded = true

        val simpleExpression = when {
            element is KtSimpleNameExpression -> element
            element.parent is KtSimpleNameExpression -> element.parent as KtSimpleNameExpression
            else -> null
        }

        if (simpleExpression != null) {
            val inDescriptor = simpleExpression.getResolutionScope(bindingContext, resolutionFacade).ownerDescriptor

            isSortNeeded = false
            descriptors = ReferenceVariantsHelper(
                bindingContext,
                resolutionFacade,
                moduleDescriptor,
                VisibilityFilter(inDescriptor)
            ).getReferenceVariants(
                simpleExpression,
                DescriptorKindFilter.ALL,
                nameFilter,
                filterOutJavaGettersAndSetters = true,
                filterOutShadowed = false, // setting to true makes it slower up to 4 times
                excludeNonInitializedVariable = true,
                useReceiverType = null
            )

        } else {
            val resolutionScope: LexicalScope?
            val parent = element.parent

            val qualifiedExpression = when {
                element is KtQualifiedExpression -> {
                    element
                }
                parent is KtQualifiedExpression -> parent
                else -> null
            }

            if (qualifiedExpression != null) {
                val receiverExpression = qualifiedExpression.receiverExpression
                val expressionType = bindingContext.get(
                    BindingContext.EXPRESSION_TYPE_INFO,
                    receiverExpression
                )?.type
                if (expressionType != null) {
                    isSortNeeded = false
                    descriptors = ReferenceVariantsHelper(
                        bindingContext,
                        resolutionFacade,
                        moduleDescriptor,
                        { true }
                    ).getReferenceVariants(
                        receiverExpression,
                        CallTypeAndReceiver.DOT(receiverExpression),
                        DescriptorKindFilter.ALL,
                        nameFilter
                    )
                }
            } else {
                resolutionScope = bindingContext.get(
                    BindingContext.LEXICAL_SCOPE,
                    element as KtExpression?
                )
                descriptors = (resolutionScope?.getContributedDescriptors(
                    DescriptorKindFilter.ALL,
                    nameFilter
                )
                    ?: return@gen)
            }
        }

        if (descriptors == null) {
            return@gen
        }

        if (descriptors !is ArrayList<*>) {
            descriptors = ArrayList(descriptors)
        }

        //val kDocs = descriptors.map { it.findKDoc() }

        (descriptors as ArrayList<DeclarationDescriptor>)
            .map {
                val presentation =
                    getPresentation(
                        it
                    )
                Triple(it, presentation, (presentation.presentableText + presentation.tailText).toLowerCase())
            }
            .onlyIf({ isSortNeeded }) { it.sortedBy { descTriple -> descTriple.third } }
            .forEach {
                val descriptor = it.first
                val (rawName, presentableText, tailText, _, typeText) = it.second
                if (rawName == elementText) {
                    val fullName: String = formatName(rawName)
                    yield(
                        SourceCodeInspectionVariant(
                            presentableText,
                            fullName,
                            typeText,
                            getIconFromDescriptor(
                                descriptor
                            ),
                            ""
                        )
                    )
                }
            }
    }

    companion object {
        private fun getPresentation(descriptor: DeclarationDescriptor): DescriptorPresentation {
            val rawDescriptorName = descriptor.name.asString()
            val descriptorName = rawDescriptorName.quoteIfNeeded()
            var presentableText = descriptorName
            var typeText = ""
            var tailText = ""
            when (descriptor) {
                is FunctionDescriptor -> {
                    val renderedReturnType = descriptor.returnType?.let { RENDERER.renderType(it) }
                    val renderedParams = descriptor.valueParameters.map { it.name.asString() to RENDERER.renderType(it.type) }

                    val returnTypeString = renderedReturnType?.let { ": $it" } ?: ""
                    val parametersString =
                        renderedParams.joinToString(", ") { it.first + ": " + it.second }
                    val typeArgsString = descriptor.typeParameters.joinToString(", ") { RENDERER.render(it) }
                    presentableText = "fun $typeArgsString $descriptorName($parametersString)$returnTypeString"

                    val returnTypeForFunType = renderedReturnType?.let { " -> $it" } ?: " -> Unit"
                    val parametersTypesString =
                        renderedParams.joinToString(", ") { it.second }
                    typeText = "($parametersTypesString)$returnTypeForFunType"
                }
                is VariableDescriptor -> {
                    val outType = descriptor.type
                    typeText = RENDERER.renderType(outType)
                    presentableText = "val $descriptorName: $typeText"
                }
                is ClassDescriptor -> {
                    val declaredIn = descriptor.containingDeclaration
                    typeText = "class"
                    presentableText = "class $descriptorName"
                    tailText = " (" + DescriptorUtils.getFqName(declaredIn) + ")"
                }
                else -> {
                    typeText = RENDERER.render(descriptor)
                }
            }

            tailText = if (typeText.isEmpty()) tailText else typeText

            return DescriptorPresentation(
                rawDescriptorName,
                presentableText,
                tailText,
                "",
                typeText
            )
        }
    }
}
