/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.template.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.core.unquote
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.getResolvableApproximations
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.ExplicitApiDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.ifEmpty

class SpecifyTypeExplicitlyIntention : SelfTargetingRangeIntention<KtCallableDeclaration>(
    KtCallableDeclaration::class.java,
    KotlinBundle.lazyMessage("specify.type.explicitly")
), HighPriorityAction {
    override fun applicabilityRange(element: KtCallableDeclaration): TextRange? {
        if (!ExplicitApiDeclarationChecker.returnTypeCheckIsApplicable(element)) return null
        // If ApiMode is on, then this intention duplicates corresponding quickfix for compiler error
        // and we disable it here.
        if (ExplicitApiDeclarationChecker.publicReturnTypeShouldBePresentInApiMode(
                element,
                element.languageVersionSettings,
                element.resolveToDescriptorIfAny()
            )
        ) return null
        setTextGetter(
            if (element is KtFunction)
                KotlinBundle.lazyMessage("specify.return.type.explicitly")
            else
                defaultTextGetter
        )

        val initializer = (element as? KtDeclarationWithInitializer)?.initializer
        return if (initializer != null) {
            TextRange(element.startOffset, initializer.startOffset - 1)
        } else {
            TextRange(element.startOffset, element.endOffset)
        }
    }

    override fun applyTo(element: KtCallableDeclaration, editor: Editor?) {
        val type = getTypeForDeclaration(element)
        if (type.isError) {
            if (editor != null) {
                HintManager.getInstance().showErrorHint(editor, KotlinBundle.message("cannot.infer.type.for.this.declaration"))
            }
            return
        }

        addTypeAnnotation(editor, element, type)
    }

    companion object {
        private val PropertyDescriptor.setterType: KotlinType?
            get() = setter?.valueParameters?.firstOrNull()?.type?.let { if (it.isError) null else it }

        fun dangerousFlexibleTypeOrNull(
            declaration: KtCallableDeclaration, publicAPIOnly: Boolean, reportPlatformArguments: Boolean
        ): KotlinType? {
            when (declaration) {
                is KtFunction -> if (declaration.isLocal || declaration.hasDeclaredReturnType()) return null
                is KtProperty -> if (declaration.isLocal || declaration.typeReference != null) return null
                else -> return null
            }

            if (declaration.containingClassOrObject?.isLocal == true) return null

            val callable = declaration.resolveToDescriptorIfAny() as? CallableDescriptor ?: return null
            if (publicAPIOnly && !callable.visibility.isPublicAPI) return null
            val type = callable.returnType ?: return null
            if (type.isDynamic()) return null
            if (reportPlatformArguments) {
                if (!type.isFlexibleRecursive()) return null
            } else {
                if (!type.isFlexible()) return null
            }

            return type
        }

        fun getTypeForDeclaration(declaration: KtCallableDeclaration): KotlinType {
            val descriptor = declaration.resolveToDescriptorIfAny()
            val type = (descriptor as? CallableDescriptor)?.let {
                if (it.overriddenDescriptors.firstOrNull()?.returnType?.isMarkedNullable == false)
                    it.returnType?.makeNotNullable()
                else
                    it.returnType
            }

            if (type != null && type.isError && descriptor is PropertyDescriptor) {
                return descriptor.setterType ?: ErrorUtils.createErrorType("null type")
            }

            return type ?: ErrorUtils.createErrorType("null type")
        }

        fun createTypeExpressionForTemplate(
            exprType: KotlinType,
            contextElement: KtDeclaration,
            useTypesFromOverridden: Boolean = false
        ): Expression? {
            val resolutionFacade = contextElement.getResolutionFacade()
            val bindingContext = resolutionFacade.analyze(contextElement, BodyResolveMode.PARTIAL)
            val scope = contextElement.getResolutionScope(bindingContext, resolutionFacade)

            fun KotlinType.toResolvableApproximations(): List<KotlinType> =
                with(getResolvableApproximations(scope, checkTypeParameters = false).toList()) {
                    when {
                        exprType.isNullabilityFlexible() -> flatMap {
                            listOf(TypeUtils.makeNotNullable(it), TypeUtils.makeNullable(it))
                        }
                        else -> this
                    }
                }

            val overriddenTypes: List<KotlinType> = if (!useTypesFromOverridden) {
                null
            } else {
                val declarationDescriptor = contextElement.resolveToDescriptorIfAny() as? CallableDescriptor
                declarationDescriptor?.overriddenDescriptors?.mapNotNull { it.returnType }
            } ?: emptyList()

            val types = (listOf(exprType) + overriddenTypes).distinct().flatMap {
                it.toResolvableApproximations()
            }.ifEmpty { return null }

            if (ApplicationManager.getApplication().isUnitTestMode) {
                // This helps to be sure no nullable types are suggested
                if (contextElement.containingKtFile.findDescendantOfType<PsiComment>()?.takeIf {
                        it.text == "// CHOOSE_NULLABLE_TYPE_IF_EXISTS"
                    } != null) {
                    val targetType = types.firstOrNull { it.isMarkedNullable } ?: types.first()
                    return TypeChooseValueExpression(listOf(targetType), targetType)
                }
                // This helps to be sure something except Nothing is suggested
                if (contextElement.containingKtFile.findDescendantOfType<PsiComment>()?.takeIf {
                        it.text == "// DO_NOT_CHOOSE_NOTHING"
                    } != null) {
                    val targetType = types.firstOrNull { !KotlinBuiltIns.isNothingOrNullableNothing(it) } ?: types.first()
                    return TypeChooseValueExpression(listOf(targetType), targetType)
                }
            }

            return TypeChooseValueExpression(types, types.first())
        }

        // Explicit class is used because of KT-20460
        private class TypeChooseValueExpression(
            items: List<KotlinType>, defaultItem: KotlinType
        ) : ChooseValueExpression<KotlinType>(items, defaultItem) {
            override fun getLookupString(element: KotlinType) =
                IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(element)

            override fun getResult(element: KotlinType): String {
                val renderType = IdeDescriptorRenderers.FQ_NAMES_IN_TYPES_WITH_NORMALIZER.renderType(element)
                val descriptor = element.constructor.declarationDescriptor
                if (descriptor?.fqNameOrNull()?.asString() == renderType) {
                    val className = (DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? KtClass)?.nameIdentifier?.text
                    if (className != null && className != className.unquote()) {
                        return className
                    }
                }

                return renderType
            }
        }

        fun addTypeAnnotation(editor: Editor?, declaration: KtCallableDeclaration, exprType: KotlinType) = if (editor != null) {
            addTypeAnnotationWithTemplate(editor, declaration, exprType)
        } else {
            declaration.setType(exprType)
        }

        @JvmOverloads
        fun createTypeReferencePostprocessor(
            declaration: KtCallableDeclaration,
            iterator: Iterator<KtCallableDeclaration>? = null,
            editor: Editor? = null
        ): TemplateEditingAdapter {
            return object : TemplateEditingAdapter() {
                override fun templateFinished(template: Template, brokenOff: Boolean) {
                    val typeRef = declaration.typeReference
                    if (typeRef != null && typeRef.isValid) {
                        runWriteAction {
                            ShortenReferences.DEFAULT.process(typeRef)
                            if (iterator != null && editor != null) addTypeAnnotationWithTemplate(editor, iterator)
                        }
                    }
                }
            }
        }

        fun addTypeAnnotationWithTemplate(editor: Editor, iterator: Iterator<KtCallableDeclaration>?) {
            if (iterator == null || !iterator.hasNext()) return
            val declaration = iterator.next()
            val exprType = getTypeForDeclaration(declaration)
            addTypeAnnotationWithTemplate(editor, declaration, exprType, iterator)
        }

        private fun addTypeAnnotationWithTemplate(
            editor: Editor, declaration: KtCallableDeclaration, exprType: KotlinType,
            iterator: Iterator<KtCallableDeclaration>? = null
        ) {
            assert(!exprType.isError) { "Unexpected error type, should have been checked before: " + declaration.getElementTextWithContext() + ", type = " + exprType }

            val project = declaration.project
            val expression = createTypeExpressionForTemplate(exprType, declaration, useTypesFromOverridden = true) ?: return

            declaration.setType(StandardNames.FqNames.any.asString())

            PsiDocumentManager.getInstance(project).commitAllDocuments()
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

            val newTypeRef = declaration.typeReference!!
            val builder = TemplateBuilderImpl(newTypeRef)
            builder.replaceElement(newTypeRef, expression)

            editor.caretModel.moveToOffset(newTypeRef.node.startOffset)

            TemplateManager.getInstance(project).startTemplate(
                editor,
                builder.buildInlineTemplate(),
                createTypeReferencePostprocessor(declaration, iterator, editor)
            )
        }
    }
}

