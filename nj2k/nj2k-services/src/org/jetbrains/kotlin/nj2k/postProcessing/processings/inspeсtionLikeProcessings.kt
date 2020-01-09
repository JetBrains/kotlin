/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing.processings

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.debugger.sequence.psi.callName
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeIntention
import org.jetbrains.kotlin.idea.intentions.addUseSiteTarget
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedFoldingUtils
import org.jetbrains.kotlin.idea.quickfix.AddConstModifierFix
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.postProcessing.InspectionLikeProcessingForElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class RemoveExplicitPropertyTypeProcessing : InspectionLikeProcessingForElement<KtProperty>(KtProperty::class.java) {
    override fun isApplicableTo(element: KtProperty, settings: ConverterSettings?): Boolean {
        val needFieldTypes = settings?.specifyFieldTypeByDefault == true
        val needLocalVariablesTypes = settings?.specifyLocalVariableTypeByDefault == true

        if (needLocalVariablesTypes && element.isLocal) return false
        if (needFieldTypes && element.isMember) return false
        val initializer = element.initializer ?: return false
        val withoutExpectedType =
            initializer.analyzeInContext(initializer.getResolutionScope()).getType(initializer) ?: return false
        val typeBeDescriptor = element.resolveToDescriptorIfAny().safeAs<CallableDescriptor>()?.returnType ?: return false
        return KotlinTypeChecker.DEFAULT.equalTypes(withoutExpectedType, typeBeDescriptor)
    }

    override fun apply(element: KtProperty) {
        element.typeReference = null
    }
}

class RemoveRedundantNullabilityProcessing : InspectionLikeProcessingForElement<KtProperty>(KtProperty::class.java) {
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
        typeElement?.replace(typeElement.safeAs<KtNullableType>()?.innerType ?: return)
    }
}

class RemoveExplicitTypeArgumentsProcessing : InspectionLikeProcessingForElement<KtTypeArgumentList>(KtTypeArgumentList::class.java) {
    override fun isApplicableTo(element: KtTypeArgumentList, settings: ConverterSettings?): Boolean =
        RemoveExplicitTypeArgumentsIntention.isApplicableTo(element, approximateFlexible = true)

    override fun apply(element: KtTypeArgumentList) {
        element.delete()
    }
}

// the types arguments for Stream.collect calls cannot be explicitly specified in Kotlin
// but we need them in nullability inference, so we remove it here
class RemoveJavaStreamsCollectCallTypeArgumentsProcessing :
    InspectionLikeProcessingForElement<KtCallExpression>(KtCallExpression::class.java) {
    override fun isApplicableTo(element: KtCallExpression, settings: ConverterSettings?): Boolean {
        if (element.typeArgumentList == null) return false
        if (element.callName() != COLLECT_FQ_NAME.shortName().identifier) return false
        return element.isCalling(COLLECT_FQ_NAME)
    }

    override fun apply(element: KtCallExpression) {
        element.typeArgumentList?.delete()
    }

    companion object {
        private val COLLECT_FQ_NAME = FqName("java.util.stream.Stream.collect")
    }
}


class RemoveRedundantOverrideVisibilityProcessing :
    InspectionLikeProcessingForElement<KtCallableDeclaration>(KtCallableDeclaration::class.java) {

    override fun isApplicableTo(element: KtCallableDeclaration, settings: ConverterSettings?): Boolean {
        if (!element.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return false
        return element.visibilityModifier() != null
    }

    override fun apply(element: KtCallableDeclaration) {
        val modifier = element.visibilityModifierType() ?: return
        element.setVisibility(modifier)
    }
}

class ReplaceGetterBodyWithSingleReturnStatementWithExpressionBody :
    InspectionLikeProcessingForElement<KtPropertyAccessor>(KtPropertyAccessor::class.java) {

    private fun KtPropertyAccessor.singleBodyStatementExpression() =
        bodyBlockExpression?.statements
            ?.singleOrNull()
            ?.safeAs<KtReturnExpression>()
            ?.takeIf { it.labeledExpression == null }
            ?.returnedExpression

    override fun isApplicableTo(element: KtPropertyAccessor, settings: ConverterSettings?): Boolean {
        if (!element.isGetter) return false
        return element.singleBodyStatementExpression() != null
    }

    override fun apply(element: KtPropertyAccessor) {
        val body = element.bodyExpression ?: return
        val returnedExpression = element.singleBodyStatementExpression() ?: return

        val commentSaver = CommentSaver(body)
        element.addBefore(KtPsiFactory(element).createEQ(), body)
        val newBody = body.replaced(returnedExpression)
        commentSaver.restore(newBody)
    }
}

class RemoveRedundantCastToNullableProcessing :
    InspectionLikeProcessingForElement<KtBinaryExpressionWithTypeRHS>(KtBinaryExpressionWithTypeRHS::class.java) {

    override fun isApplicableTo(element: KtBinaryExpressionWithTypeRHS, settings: ConverterSettings?): Boolean {
        if (element.right?.typeElement !is KtNullableType) return false
        val context = element.analyze()
        val leftType = context.getType(element.left) ?: return false
        val rightType = context.get(BindingContext.TYPE, element.right) ?: return false
        return !leftType.isMarkedNullable && rightType.isMarkedNullable
    }

    override fun apply(element: KtBinaryExpressionWithTypeRHS) {
        val type = element.right?.typeElement as? KtNullableType ?: return
        type.replace(type.innerType ?: return)
    }
}

class RemoveRedundantSamAdaptersProcessing :
    InspectionLikeProcessingForElement<KtCallExpression>(KtCallExpression::class.java) {
    override val writeActionNeeded = false

    override fun isApplicableTo(element: KtCallExpression, settings: ConverterSettings?): Boolean =
        RedundantSamConstructorInspection.samConstructorCallsToBeConverted(element).isNotEmpty()

    override fun apply(element: KtCallExpression) {
        val callsToBeConverted = RedundantSamConstructorInspection.samConstructorCallsToBeConverted(element)
        runWriteAction {
            for (call in callsToBeConverted) {
                RedundantSamConstructorInspection.replaceSamConstructorCall(call)
            }
        }
    }
}

class UninitializedVariableReferenceFromInitializerToThisReferenceProcessing :
    InspectionLikeProcessingForElement<KtSimpleNameExpression>(KtSimpleNameExpression::class.java) {

    override fun isApplicableTo(element: KtSimpleNameExpression, settings: ConverterSettings?): Boolean {
        val anonymousObject = element.getStrictParentOfType<KtClassOrObject>()?.takeIf { it.name == null } ?: return false
        val resolved = element.mainReference.resolve() ?: return false
        if (resolved.isAncestor(element, strict = true)) {
            if (resolved is KtVariableDeclaration && resolved.hasInitializer()) {
                if (resolved.initializer?.getChildOfType<KtClassOrObject>() == anonymousObject) {
                    return true
                }
            }
        }
        return false
    }

    override fun apply(element: KtSimpleNameExpression) {
        element.replaced(KtPsiFactory(element).createThisExpression())
    }
}

class UnresolvedVariableReferenceFromInitializerToThisReferenceProcessing :
    InspectionLikeProcessingForElement<KtSimpleNameExpression>(KtSimpleNameExpression::class.java) {

    override fun isApplicableTo(element: KtSimpleNameExpression, settings: ConverterSettings?): Boolean {
        val anonymousObject = element.getStrictParentOfType<KtClassOrObject>() ?: return false
        val variable = anonymousObject.getStrictParentOfType<KtVariableDeclaration>() ?: return false
        if (variable.nameAsName != element.getReferencedNameAsName()) return false
        if (variable.initializer?.getChildOfType<KtClassOrObject>() != anonymousObject) return false
        return element.mainReference.resolve() == null
    }

    override fun apply(element: KtSimpleNameExpression) {
        element.replaced(KtPsiFactory(element).createThisExpression())
    }
}

class VarToValProcessing : InspectionLikeProcessingForElement<KtProperty>(KtProperty::class.java) {
    private fun KtProperty.hasWriteUsages(): Boolean =
        ReferencesSearch.search(this, useScope).any { usage ->
            (usage as? KtSimpleNameReference)?.element?.let { nameReference ->
                val receiver = nameReference.parent?.safeAs<KtDotQualifiedExpression>()?.receiverExpression
                if (nameReference.getStrictParentOfType<KtAnonymousInitializer>() != null
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

class JavaObjectEqualsToEqOperatorProcessing : InspectionLikeProcessingForElement<KtCallExpression>(KtCallExpression::class.java) {
    companion object {
        val CALL_FQ_NAME = FqName("java.util.Objects.equals")
    }

    override fun isApplicableTo(element: KtCallExpression, settings: ConverterSettings?): Boolean {
        if (element.callName() != CALL_FQ_NAME.shortName().identifier) return false
        if (element.valueArguments.size != 2) return false
        if (element.valueArguments.any { it.getArgumentExpression() == null }) return false
        return element.isCalling(CALL_FQ_NAME)
    }

    override fun apply(element: KtCallExpression) {
        val factory = KtPsiFactory(element)
        element.getQualifiedExpressionForSelectorOrThis().replace(
            factory.createExpressionByPattern(
                "($0 == $1)",
                element.valueArguments[0].getArgumentExpression() ?: return,
                element.valueArguments[1].getArgumentExpression() ?: return
            )
        )
    }
}


class RemoveForExpressionLoopParameterTypeProcessing :
    InspectionLikeProcessingForElement<KtForExpression>(KtForExpression::class.java) {
    override fun isApplicableTo(element: KtForExpression, settings: ConverterSettings?): Boolean =
        element.loopParameter?.typeReference?.typeElement != null
                && settings?.specifyLocalVariableTypeByDefault != true

    override fun apply(element: KtForExpression) {
        element.loopParameter?.typeReference = null
    }
}

class RemoveRedundantConstructorKeywordProcessing :
    InspectionLikeProcessingForElement<KtPrimaryConstructor>(KtPrimaryConstructor::class.java) {
    override fun isApplicableTo(element: KtPrimaryConstructor, settings: ConverterSettings?): Boolean =
        element.containingClassOrObject is KtClass
                && element.getConstructorKeyword() != null
                && element.annotationEntries.isEmpty()
                && element.visibilityModifier() == null


    override fun apply(element: KtPrimaryConstructor) {
        element.getConstructorKeyword()?.delete()
        element.prevSibling
            ?.safeAs<PsiWhiteSpace>()
            ?.takeUnless { it.textContains('\n') }
            ?.delete()
    }
}

class RemoveRedundantModalityModifierProcessing : InspectionLikeProcessingForElement<KtDeclaration>(KtDeclaration::class.java) {
    override fun isApplicableTo(element: KtDeclaration, settings: ConverterSettings?): Boolean {
        if (element.hasModifier(KtTokens.FINAL_KEYWORD)) {
            return !element.hasModifier(KtTokens.OVERRIDE_KEYWORD)
        }
        val modalityModifierType = element.modalityModifierType() ?: return false
        return modalityModifierType == element.implicitModality()
    }

    override fun apply(element: KtDeclaration) {
        element.removeModifier(element.modalityModifierType() ?: return)
    }
}


class RemoveRedundantVisibilityModifierProcessing : InspectionLikeProcessingForElement<KtDeclaration>(KtDeclaration::class.java) {
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
        element.removeModifier(element.visibilityModifierType() ?: return)
    }
}

class RemoveExplicitOpenInInterfaceProcessing : InspectionLikeProcessingForElement<KtClass>(KtClass::class.java) {
    override fun isApplicableTo(element: KtClass, settings: ConverterSettings?): Boolean =
        element.isValid
                && element.isInterface()
                && element.hasModifier(KtTokens.OPEN_KEYWORD)

    override fun apply(element: KtClass) {
        element.removeModifier(KtTokens.OPEN_KEYWORD)
    }
}

class MoveGetterAndSetterAnnotationsToPropertyProcessing : InspectionLikeProcessingForElement<KtProperty>(KtProperty::class.java) {
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

class RedundantExplicitTypeInspectionBasedProcessing : InspectionLikeProcessingForElement<KtProperty>(KtProperty::class.java) {
    override fun isApplicableTo(element: KtProperty, settings: ConverterSettings?): Boolean =
        RedundantExplicitTypeInspection.hasRedundantType(element)

    override fun apply(element: KtProperty) {
        element.typeReference = null
        RemoveExplicitTypeIntention.removeExplicitType(element)
    }
}

class CanBeValInspectionBasedProcessing : InspectionLikeProcessingForElement<KtDeclaration>(KtDeclaration::class.java) {
    override fun isApplicableTo(element: KtDeclaration, settings: ConverterSettings?): Boolean =
        CanBeValInspection.canBeVal(element, ignoreNotUsedVals = false)

    override fun apply(element: KtDeclaration) {
        if (element !is KtValVarKeywordOwner) return
        element.valOrVarKeyword?.replace(KtPsiFactory(element).createValKeyword())
    }
}


class MayBeConstantInspectionBasedProcessing : InspectionLikeProcessingForElement<KtProperty>(KtProperty::class.java) {
    override fun isApplicableTo(element: KtProperty, settings: ConverterSettings?): Boolean =
        with(MayBeConstantInspection) {
            val status = element.getStatus()
            status == MayBeConstantInspection.Status.MIGHT_BE_CONST
                    || status == MayBeConstantInspection.Status.JVM_FIELD_MIGHT_BE_CONST
        }

    override fun apply(element: KtProperty) {
        AddConstModifierFix.addConstModifier(element)
    }
}

class RemoveExplicitUnitTypeProcessing : InspectionLikeProcessingForElement<KtNamedFunction>(KtNamedFunction::class.java) {
    override fun isApplicableTo(element: KtNamedFunction, settings: ConverterSettings?): Boolean {
        val typeReference = element.typeReference?.typeElement ?: return false
        if (!typeReference.textMatches("Unit")) return false
        return RedundantUnitReturnTypeInspection.hasRedundantUnitReturnType(element)
    }

    override fun apply(element: KtNamedFunction) {
        element.typeReference = null
    }
}


class RemoveExplicitGetterInspectionBasedProcessing :
    InspectionLikeProcessingForElement<KtPropertyAccessor>(KtPropertyAccessor::class.java) {
    override fun isApplicableTo(element: KtPropertyAccessor, settings: ConverterSettings?): Boolean =
        element.isRedundantGetter()

    override fun apply(element: KtPropertyAccessor) {
        RemoveRedundantGetterFix.removeRedundantGetter(element)
    }
}

class RemoveExplicitSetterInspectionBasedProcessing :
    InspectionLikeProcessingForElement<KtPropertyAccessor>(KtPropertyAccessor::class.java) {
    override fun isApplicableTo(element: KtPropertyAccessor, settings: ConverterSettings?): Boolean =
        element.isRedundantSetter()

    override fun apply(element: KtPropertyAccessor) {
        RemoveRedundantSetterFix.removeRedundantSetter(element)
    }
}


class RedundantSemicolonInspectionBasedProcessing :
    InspectionLikeProcessingForElement<PsiElement>(PsiElement::class.java) {
    override fun isApplicableTo(element: PsiElement, settings: ConverterSettings?): Boolean =
        element.node.elementType == KtTokens.SEMICOLON
                && RedundantSemicolonInspection.isRedundantSemicolon(element)

    override fun apply(element: PsiElement) {
        element.delete()
    }
}

class ExplicitThisInspectionBasedProcessing :
    InspectionLikeProcessingForElement<KtExpression>(KtExpression::class.java) {
    override fun isApplicableTo(element: KtExpression, settings: ConverterSettings?): Boolean =
        ExplicitThisInspection.hasExplicitThis(element)

    override fun apply(element: KtExpression) {
        ExplicitThisExpressionFix.removeExplicitThisExpression(
            with(ExplicitThisInspection) {
                element.thisAsReceiverOrNull() ?: return
            }
        )
    }
}

class LiftReturnInspectionBasedProcessing :
    InspectionLikeProcessingForElement<KtExpression>(KtExpression::class.java) {
    override fun isApplicableTo(element: KtExpression, settings: ConverterSettings?): Boolean =
        LiftReturnOrAssignmentInspection.getState(element, false)?.any {
            it.liftType == LiftReturnOrAssignmentInspection.Companion.LiftType.LIFT_RETURN_OUT
        } ?: false

    override fun apply(element: KtExpression) {
        BranchedFoldingUtils.foldToReturn(element)
    }
}

class LiftAssignmentInspectionBasedProcessing :
    InspectionLikeProcessingForElement<KtExpression>(KtExpression::class.java) {
    override fun isApplicableTo(element: KtExpression, settings: ConverterSettings?): Boolean =
        LiftReturnOrAssignmentInspection.getState(element, false)?.any {
            it.liftType == LiftReturnOrAssignmentInspection.Companion.LiftType.LIFT_ASSIGNMENT_OUT
        } ?: false

    override fun apply(element: KtExpression) {
        BranchedFoldingUtils.foldToAssignment(element)
    }
}

class MoveLambdaOutsideParenthesesProcessing :
    InspectionLikeProcessingForElement<KtCallExpression>(KtCallExpression::class.java) {
    override fun isApplicableTo(element: KtCallExpression, settings: ConverterSettings?): Boolean =
        element.canMoveLambdaOutsideParentheses()

    override fun apply(element: KtCallExpression) {
        element.moveFunctionLiteralOutsideParentheses()
    }
}