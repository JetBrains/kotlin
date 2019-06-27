/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing.processings

import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.implicitModality
import org.jetbrains.kotlin.idea.core.implicitVisibility
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.idea.inspections.RedundantExplicitTypeInspection
import org.jetbrains.kotlin.idea.inspections.RedundantSamConstructorInspection
import org.jetbrains.kotlin.idea.inspections.UseExpressionBodyInspection
import org.jetbrains.kotlin.idea.intentions.ConvertToStringTemplateIntention
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.intentions.UsePropertyAccessSyntaxIntention
import org.jetbrains.kotlin.idea.intentions.addUseSiteTarget
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.parentOfType
import org.jetbrains.kotlin.nj2k.postProcessing.ApplicabilityBasedInspectionLikeProcessing
import org.jetbrains.kotlin.nj2k.postProcessing.InspectionLikeProcessing
import org.jetbrains.kotlin.nj2k.postProcessing.generalInspectionBasedProcessing
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class RemoveExplicitPropertyTypeProcessing : ApplicabilityBasedInspectionLikeProcessing<KtProperty>(KtProperty::class) {
    override fun isApplicableTo(element: KtProperty, settings: ConverterSettings?): Boolean {
        val needFieldTypes = settings?.specifyFieldTypeByDefault == true
        val needLocalVariablesTypes = settings?.specifyLocalVariableTypeByDefault == true

        if (needLocalVariablesTypes && element.isLocal) return false
        if (needFieldTypes && element.isMember) return false
        val initializer = element.initializer ?: return false
        val withoutExpectedType =
            initializer.analyzeInContext(initializer.getResolutionScope()).getType(initializer) ?: return false
        val descriptor = element.resolveToDescriptorIfAny() as? CallableDescriptor ?: return false
        return if (element.isVar) withoutExpectedType == descriptor.returnType
        else withoutExpectedType.makeNotNullable() == descriptor.returnType?.makeNotNullable()
    }

    override fun apply(element: KtProperty) {
        element.typeReference = null
    }
}

class RemoveRedundantNullabilityProcessing : ApplicabilityBasedInspectionLikeProcessing<KtProperty>(KtProperty::class) {
    override fun isApplicableTo(element: KtProperty, settings: ConverterSettings?): Boolean {
        if (!element.isLocal) return false
        val typeReference = element.typeReference
        if (typeReference == null || typeReference.typeElement !is KtNullableType) return false
        val initializerType = element.initializer?.let {
            it.analyzeInContext(element.getResolutionScope()).getType(it)
        }
        if (initializerType?.isNullable() == true) return false

        return ReferencesSearch.search(element, element.useScope).findAll().mapNotNull { ref ->
            val parent = (ref.element.parent as? KtExpression)?.asAssignment()
            parent?.takeIf { it.left == ref.element }
        }.all {
            val right = it.right
            val withoutExpectedType = right?.analyzeInContext(element.getResolutionScope())
            withoutExpectedType?.getType(right)?.isNullable() == false
        }
    }

    override fun apply(element: KtProperty) {
        val typeElement = element.typeReference?.typeElement
        typeElement?.replace(typeElement.cast<KtNullableType>().innerType!!)
    }
}

class RemoveExplicitTypeArgumentsProcessing : ApplicabilityBasedInspectionLikeProcessing<KtTypeArgumentList>(KtTypeArgumentList::class) {
    override fun isApplicableTo(element: KtTypeArgumentList, settings: ConverterSettings?): Boolean =
        RemoveExplicitTypeArgumentsIntention.isApplicableTo(element, approximateFlexible = true)

    override fun apply(element: KtTypeArgumentList) {
        element.delete()
    }
}

class RemoveRedundantOverrideVisibilityProcessing : InspectionLikeProcessing {
    override val writeActionNeeded = true

    override fun createAction(element: PsiElement, settings: ConverterSettings?): (() -> Unit)? {
        if (element !is KtCallableDeclaration || !element.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return null
        val modifier = element.visibilityModifierType() ?: return null
        return { element.setVisibility(modifier) }
    }
}

class ConvertToStringTemplateProcessing : InspectionLikeProcessing {
    override val writeActionNeeded = true

    val intention = ConvertToStringTemplateIntention()

    override fun createAction(element: PsiElement, settings: ConverterSettings?): (() -> Unit)? {
        if (element is KtBinaryExpression && intention.isApplicableTo(element) && ConvertToStringTemplateIntention.shouldSuggestToConvert(
                element
            )
        ) {
            return { intention.applyTo(element, null) }
        } else {
            return null
        }
    }
}

class UsePropertyAccessSyntaxProcessing : InspectionLikeProcessing {
    override val writeActionNeeded = true

    val intention = UsePropertyAccessSyntaxIntention()

    override fun createAction(element: PsiElement, settings: ConverterSettings?): (() -> Unit)? {
        if (element !is KtCallExpression) return null
        val propertyName = intention.detectPropertyNameToUse(element) ?: return null
        return { intention.applyTo(element, propertyName, reformat = true) }
    }
}

class RemoveRedundantSamAdaptersProcessing : InspectionLikeProcessing {
    override val writeActionNeeded = true

    override fun createAction(element: PsiElement, settings: ConverterSettings?): (() -> Unit)? {
        if (element !is KtCallExpression) return null

        val expressions = RedundantSamConstructorInspection.samConstructorCallsToBeConverted(element)
        if (expressions.isEmpty()) return null

        return {
            RedundantSamConstructorInspection.samConstructorCallsToBeConverted(element)
                .forEach { RedundantSamConstructorInspection.replaceSamConstructorCall(it) }
        }
    }
}

class UseExpressionBodyProcessing : InspectionLikeProcessing {
    override val writeActionNeeded = true

    override fun createAction(element: PsiElement, settings: ConverterSettings?): (() -> Unit)? {
        if (element !is KtPropertyAccessor) return null

        val inspection = UseExpressionBodyInspection(convertEmptyToUnit = false)
        if (!inspection.isActiveFor(element)) return null

        return {
            if (inspection.isActiveFor(element)) {
                inspection.simplify(element, false)
            }
        }
    }
}

class RemoveRedundantCastToNullableProcessing : InspectionLikeProcessing {
    override val writeActionNeeded = true

    override fun createAction(element: PsiElement, settings: ConverterSettings?): (() -> Unit)? {
        if (element !is KtBinaryExpressionWithTypeRHS) return null

        val context = element.analyze()
        val leftType = context.getType(element.left) ?: return null
        val rightType = context.get(BindingContext.TYPE, element.right) ?: return null

        if (!leftType.isMarkedNullable && rightType.isMarkedNullable) {
            return {
                val type = element.right?.typeElement as? KtNullableType
                type?.replace(type.innerType!!)
            }
        }

        return null
    }
}

class UninitializedVariableReferenceFromInitializerToThisReferenceProcessing :
    InspectionLikeProcessing {
    override val writeActionNeeded = true

    override fun createAction(element: PsiElement, settings: ConverterSettings?): (() -> Unit)? {
        if (element !is KtSimpleNameExpression) return null
        val anonymousObject = element.getParentOfType<KtClassOrObject>(true)?.takeIf { it.name == null } ?: return null

        val resolved = element.mainReference.resolve() ?: return null
        if (resolved.isAncestor(element, strict = true)) {
            if (resolved is KtVariableDeclaration && resolved.hasInitializer()) {
                if (resolved.initializer!!.getChildOfType<KtClassOrObject>() == anonymousObject) {
                    return { element.replaced(KtPsiFactory(element).createThisExpression()) }
                }
            }
        }

        return null
    }
}

class UnresolvedVariableReferenceFromInitializerToThisReferenceProcessing :
    InspectionLikeProcessing {
    override val writeActionNeeded = true

    override fun createAction(element: PsiElement, settings: ConverterSettings?): (() -> Unit)? {
        if (element !is KtSimpleNameExpression || element.mainReference.resolve() != null) return null

        val anonymousObject = element.getParentOfType<KtClassOrObject>(true) ?: return null

        val variable = anonymousObject.getParentOfType<KtVariableDeclaration>(true) ?: return null

        if (variable.nameAsName == element.getReferencedNameAsName() &&
            variable.initializer?.getChildOfType<KtClassOrObject>() == anonymousObject
        ) {
            return { element.replaced(KtPsiFactory(element).createThisExpression()) }
        }

        return null
    }
}

class VarToValProcessing : ApplicabilityBasedInspectionLikeProcessing<KtProperty>(KtProperty::class) {
    private fun KtProperty.hasWriteUsages(): Boolean =
        ReferencesSearch.search(this, useScope).any { usage ->
            (usage as? KtSimpleNameReference)?.element?.let { nameReference ->
                val receiver = nameReference.parent?.safeAs<KtDotQualifiedExpression>()?.receiverExpression
                if (nameReference.parentOfType<KtAnonymousInitializer>() != null
                    && (receiver == null || receiver is KtThisExpression)
                ) return@let false
                nameReference.readWriteAccess(useResolveForReadWrite = true).isWrite
            } == true
        }

    override fun isApplicableTo(element: KtProperty, settings: ConverterSettings?): Boolean {
        if (!element.isVar) return false
        if (!element.isPrivate()) return false
        val descriptor = element.resolveToDescriptorIfAny() ?: return false
        if (descriptor.overriddenDescriptors.any { it.safeAs<VariableDescriptor>()?.isVar == true }) return false
        return !element.hasWriteUsages()
    }

    override fun apply(element: KtProperty) {
        val factory = KtPsiFactory(element)
        element.valOrVarKeyword.replace(factory.createValKeyword())
    }
}

class JavaObjectEqualsToEqOperatorProcessing : ApplicabilityBasedInspectionLikeProcessing<KtCallExpression>(KtCallExpression::class) {
    companion object {
        val CALL_FQ_NAME = FqName("java.util.Objects.equals")
    }

    override fun isApplicableTo(element: KtCallExpression, settings: ConverterSettings?): Boolean {
        if (element.valueArguments.size != 2) return false
        if (element.valueArguments.any { it.getArgumentExpression() == null }) return false
        val target = element.calleeExpression
            .safeAs<KtReferenceExpression>()
            ?.resolve()
            ?: return false
        return target.getKotlinFqName() == CALL_FQ_NAME
    }

    override fun apply(element: KtCallExpression) {
        val factory = KtPsiFactory(element)
        element.getQualifiedExpressionForSelectorOrThis().replace(
            factory.createExpressionByPattern(
                "($0 == $1)",
                element.valueArguments[0].getArgumentExpression()!!,
                element.valueArguments[1].getArgumentExpression()!!
            )
        )
    }
}


class RemoveForExpressionLoopParameterTypeProcessing :
    ApplicabilityBasedInspectionLikeProcessing<KtForExpression>(KtForExpression::class) {
    override fun isApplicableTo(element: KtForExpression, settings: ConverterSettings?): Boolean =
        element.loopParameter?.typeReference?.typeElement != null
                && settings?.specifyLocalVariableTypeByDefault != true

    override fun apply(element: KtForExpression) {
        element.loopParameter?.typeReference = null
    }
}

class RemoveRedundantConstructorKeywordProcessing :
    ApplicabilityBasedInspectionLikeProcessing<KtPrimaryConstructor>(KtPrimaryConstructor::class) {
    override fun isApplicableTo(element: KtPrimaryConstructor, settings: ConverterSettings?): Boolean =
        element.containingClassOrObject is KtClass
                && element.getConstructorKeyword() != null
                && element.annotationEntries.isEmpty()
                && element.visibilityModifier() == null


    override fun apply(element: KtPrimaryConstructor) {
        element.removeRedundantConstructorKeywordAndSpace()
    }
}

class RemoveRedundantModalityModifierProcessing : ApplicabilityBasedInspectionLikeProcessing<KtDeclaration>(KtDeclaration::class) {
    override fun isApplicableTo(element: KtDeclaration, settings: ConverterSettings?): Boolean {
        if (element.hasModifier(KtTokens.FINAL_KEYWORD)) {
            return !element.hasModifier(KtTokens.OVERRIDE_KEYWORD)
        }
        val modalityModifierType = element.modalityModifierType() ?: return false
        return modalityModifierType == element.implicitModality()
    }

    override fun apply(element: KtDeclaration) {
        element.removeModifier(element.modalityModifierType()!!)
    }
}


class RemoveRedundantVisibilityModifierProcessing : ApplicabilityBasedInspectionLikeProcessing<KtDeclaration>(KtDeclaration::class) {
    override fun isApplicableTo(element: KtDeclaration, settings: ConverterSettings?) = when {
        element.hasModifier(KtTokens.PUBLIC_KEYWORD) && element.hasModifier(KtTokens.OVERRIDE_KEYWORD) ->
            false
        element.hasModifier(KtTokens.INTERNAL_KEYWORD) && element.containingClassOrObject?.isLocal == true ->
            true
        element.visibilityModifierType() == element.implicitVisibility() ->
            true
        else -> false
    }

    override fun apply(element: KtDeclaration) {
        element.removeModifier(element.visibilityModifierType()!!)
    }
}

class RemoveExplicitPropertyTypeWithInspectionProcessing :
    InspectionLikeProcessing {
    override val writeActionNeeded: Boolean = true
    private val processing =
        generalInspectionBasedProcessing(RedundantExplicitTypeInspection())

    override fun createAction(element: PsiElement, settings: ConverterSettings?): (() -> Unit)? {
        if (settings?.specifyLocalVariableTypeByDefault == true) return null

        return processing.createAction(element, settings)
    }
}

class RemoveExplicitOpenInInterfaceProcessing : ApplicabilityBasedInspectionLikeProcessing<KtClass>(KtClass::class) {
    override fun isApplicableTo(element: KtClass, settings: ConverterSettings?): Boolean =
        element.isValid
                && element.isInterface()
                && element.hasModifier(KtTokens.OPEN_KEYWORD)

    override fun apply(element: KtClass) {
        element.removeModifier(KtTokens.OPEN_KEYWORD)
    }
}

class MoveGetterAndSetterAnnotationsToPropertyProcessing : ApplicabilityBasedInspectionLikeProcessing<KtProperty>(KtProperty::class) {
    override fun isApplicableTo(element: KtProperty, settings: ConverterSettings?): Boolean =
        element.accessors.isNotEmpty()

    override fun apply(element: KtProperty) {
        for (accessor in element.accessors.sortedBy { it.isGetter }) {
            for (entry in accessor.annotationEntries) {
                element.addAnnotationEntry(entry).also {
                    it.addUseSiteTarget(
                        if (accessor.isGetter) AnnotationUseSiteTarget.PROPERTY_GETTER
                        else AnnotationUseSiteTarget.PROPERTY_SETTER,
                        element.project
                    )
                }
            }
            accessor.annotationEntries.forEach { it.delete() }
        }
    }
}