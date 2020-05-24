/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject

internal class TypeKindHighlightingVisitor(holder: AnnotationHolder, bindingContext: BindingContext) :
    AfterAnalysisHighlightingVisitor(holder, bindingContext) {

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        val parent = expression.parent
        if (parent is KtSuperExpression || parent is KtThisExpression) {
            // Do nothing: 'super' and 'this' are highlighted as a keyword
            return
        }

        if (!NameHighlighter.namesHighlightingEnabled) return

        val referenceTarget = computeReferencedDescriptor(expression) ?: return

        val key = attributeKeyForObjectAccess(expression) ?: when (referenceTarget) {
            is TypeParameterDescriptor -> TYPE_PARAMETER
            is TypeAliasDescriptor -> TYPE_ALIAS
            !is ClassDescriptor -> return
            else -> when (referenceTarget.kind) {
                ClassKind.ANNOTATION_CLASS -> ANNOTATION
                else -> textAttributesKeyForClassDeclaration(referenceTarget)
            }
        }

        highlightName(computeHighlightingRangeForUsage(expression, referenceTarget), key)
    }

    private fun attributeKeyForObjectAccess(expression: KtSimpleNameExpression): TextAttributesKey? {
        val resolvedCall = expression.getResolvedCall(bindingContext)
        return if (resolvedCall?.resultingDescriptor is FakeCallableDescriptorForObject)
            attributeKeyForCallFromExtensions(expression, resolvedCall)
        else null
    }

    private fun computeReferencedDescriptor(expression: KtSimpleNameExpression): DeclarationDescriptor? {
        val referenceTarget = bindingContext.get(BindingContext.REFERENCE_TARGET, expression)

        if (referenceTarget !is ConstructorDescriptor) return referenceTarget

        val callElement = expression.getParentOfTypeAndBranch<KtCallExpression>(true) { calleeExpression }
            ?: expression.getParentOfTypeAndBranch<KtSuperTypeCallEntry>(true) { calleeExpression }

        if (callElement != null) {
            return referenceTarget
        }

        return referenceTarget.containingDeclaration
    }


    private fun computeHighlightingRangeForUsage(expression: KtSimpleNameExpression, referenceTarget: DeclarationDescriptor): TextRange {
        val expressionRange = expression.textRange

        if (referenceTarget !is ClassDescriptor || referenceTarget.kind != ClassKind.ANNOTATION_CLASS) return expressionRange

        // include '@' symbol if the reference is the first segment of KtAnnotationEntry
        // if "Deprecated" is highlighted then '@' should be highlighted too in "@Deprecated"
        val annotationEntry = PsiTreeUtil.getParentOfType(
            expression, KtAnnotationEntry::class.java, /* strict = */false, KtValueArgumentList::class.java
        )
        val atSymbol = annotationEntry?.atSymbol ?: return expressionRange
        return TextRange(atSymbol.textRange.startOffset, expression.textRange.endOffset)
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        val identifier = classOrObject.nameIdentifier
        val classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject)
        if (identifier != null && classDescriptor != null) {
            highlightName(
                identifier,
                attributeKeyForDeclarationFromExtensions(classOrObject, classDescriptor)
                    ?: textAttributesKeyForClassDeclaration(classDescriptor)
            )
        }
        super.visitClassOrObject(classOrObject)
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias) {
        val identifier = typeAlias.nameIdentifier
        val descriptor = bindingContext.get(BindingContext.TYPE_ALIAS, typeAlias)
        if (identifier != null && descriptor != null) {
            highlightName(
                identifier,
                attributeKeyForDeclarationFromExtensions(identifier, descriptor) ?: TYPE_ALIAS
            )
        }
        super.visitTypeAlias(typeAlias)
    }

    override fun visitDynamicType(type: KtDynamicType) {
        // Do nothing: 'dynamic' is highlighted as a keyword
    }

    private fun textAttributesKeyForClassDeclaration(descriptor: ClassDescriptor): TextAttributesKey = when (descriptor.kind) {
        ClassKind.INTERFACE -> TRAIT
        ClassKind.ANNOTATION_CLASS -> ANNOTATION
        ClassKind.OBJECT -> OBJECT
        ClassKind.ENUM_ENTRY -> ENUM_ENTRY
        else -> if (descriptor.modality === Modality.ABSTRACT) ABSTRACT_CLASS else CLASS
    }
}
