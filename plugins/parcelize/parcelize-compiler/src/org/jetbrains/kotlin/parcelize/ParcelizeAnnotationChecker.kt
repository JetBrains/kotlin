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

package org.jetbrains.kotlin.parcelize

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.parcelize.ParcelizeNames.DEPRECATED_RUNTIME_PACKAGE
import org.jetbrains.kotlin.parcelize.ParcelizeNames.IGNORED_ON_PARCEL_FQ_NAMES
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELER_FQN
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELIZE_CLASS_FQ_NAMES
import org.jetbrains.kotlin.parcelize.ParcelizeNames.RAW_VALUE_ANNOTATION_FQ_NAMES
import org.jetbrains.kotlin.parcelize.ParcelizeNames.TYPE_PARCELER_FQ_NAMES
import org.jetbrains.kotlin.parcelize.ParcelizeNames.WRITE_WITH_FQ_NAMES
import org.jetbrains.kotlin.parcelize.diagnostic.ErrorsParcelize
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import org.jetbrains.kotlin.types.typeUtil.supertypes

open class ParcelizeAnnotationChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val constructorDescriptor = resolvedCall.resultingDescriptor as? ClassConstructorDescriptor ?: return
        val annotationClass = constructorDescriptor.constructedClass.takeIf { it.kind == ClassKind.ANNOTATION_CLASS } ?: return

        val annotationEntry = resolvedCall.call.callElement.getNonStrictParentOfType<KtAnnotationEntry>() ?: return
        val annotationOwner = annotationEntry.getStrictParentOfType<KtModifierListOwner>() ?: return

        if (annotationClass.fqNameSafe in TYPE_PARCELER_FQ_NAMES) {
            checkTypeParcelerUsage(resolvedCall, annotationEntry, context, annotationOwner)
            checkDeprecatedAnnotations(resolvedCall, annotationEntry, context, isForbidden = true)
        }

        if (annotationClass.fqNameSafe in WRITE_WITH_FQ_NAMES) {
            checkWriteWithUsage(resolvedCall, annotationEntry, context, annotationOwner)
            checkDeprecatedAnnotations(resolvedCall, annotationEntry, context, isForbidden = true)
        }

        if (annotationClass.fqNameSafe in IGNORED_ON_PARCEL_FQ_NAMES) {
            checkIgnoredOnParcelUsage(annotationEntry, context, annotationOwner)
            checkDeprecatedAnnotations(resolvedCall, annotationEntry, context, isForbidden = false)
        }

        if (annotationClass.fqNameSafe in PARCELIZE_CLASS_FQ_NAMES || annotationClass.fqNameSafe in RAW_VALUE_ANNOTATION_FQ_NAMES) {
            checkDeprecatedAnnotations(resolvedCall, annotationEntry, context, isForbidden = false)
        }
    }

    private fun checkDeprecatedAnnotations(
        resolvedCall: ResolvedCall<*>,
        annotationEntry: KtAnnotationEntry,
        context: CallCheckerContext,
        isForbidden: Boolean
    ) {
        val descriptorPackage = resolvedCall.resultingDescriptor.containingPackage()
        if (descriptorPackage == DEPRECATED_RUNTIME_PACKAGE) {
            val factory = if (isForbidden) ErrorsParcelize.FORBIDDEN_DEPRECATED_ANNOTATION else ErrorsParcelize.DEPRECATED_ANNOTATION
            context.trace.report(factory.on(annotationEntry))
        }
    }

    private fun checkIgnoredOnParcelUsage(annotationEntry: KtAnnotationEntry, context: CallCheckerContext, element: KtModifierListOwner) {
        if (element is KtParameter && PsiTreeUtil.getParentOfType(element, KtDeclaration::class.java) is KtPrimaryConstructor) {
            context.trace.report(ErrorsParcelize.INAPPLICABLE_IGNORED_ON_PARCEL_CONSTRUCTOR_PROPERTY.on(annotationEntry))
        } else if (element !is KtProperty || PsiTreeUtil.getParentOfType(element, KtDeclaration::class.java) !is KtClassOrObject) {
            context.trace.report(ErrorsParcelize.INAPPLICABLE_IGNORED_ON_PARCEL.on(annotationEntry))
        }
    }

    private fun checkTypeParcelerUsage(
        resolvedCall: ResolvedCall<*>,
        annotationEntry: KtAnnotationEntry,
        context: CallCheckerContext,
        element: KtModifierListOwner
    ) {
        val descriptor = context.trace[BindingContext.DECLARATION_TO_DESCRIPTOR, element] ?: return
        val thisMappedType = resolvedCall.typeArguments.values.takeIf { it.size == 2 }?.first() ?: return

        val duplicatingAnnotationCount = descriptor.annotations
            .filter { it.fqName in TYPE_PARCELER_FQ_NAMES }
            .mapNotNull { it.type.arguments.takeIf { args -> args.size == 2 }?.first()?.type }
            .count { it == thisMappedType }

        if (duplicatingAnnotationCount > 1) {
            val reportElement = annotationEntry.typeArguments.firstOrNull() ?: annotationEntry
            context.trace.report(ErrorsParcelize.DUPLICATING_TYPE_PARCELERS.on(reportElement))
            return
        }

        val containingClass = when (element) {
            is KtClassOrObject -> element
            is KtParameter -> element.containingClassOrObject
            else -> null
        }

        checkIfTheContainingClassIsParcelize(containingClass, annotationEntry, context)

        if (element is KtParameter && element.getStrictParentOfType<KtDeclaration>() is KtPrimaryConstructor) {
            val containingClassDescriptor = context.trace[BindingContext.CLASS, containingClass]
            val thisAnnotationDescriptor = context.trace[BindingContext.ANNOTATION, annotationEntry]

            if (containingClass != null && containingClassDescriptor != null && thisAnnotationDescriptor != null) {
                // We can ignore value arguments here cause @TypeParceler is a zero-parameter annotation
                if (containingClassDescriptor.annotations.any { it.type == thisAnnotationDescriptor.type }) {
                    val reportElement = (annotationEntry.typeReference?.typeElement as? KtUserType)?.referenceExpression ?: annotationEntry
                    context.trace.report(ErrorsParcelize.REDUNDANT_TYPE_PARCELER.on(reportElement, containingClass))
                }
            }
        }
    }

    private fun checkWriteWithUsage(
        resolvedCall: ResolvedCall<*>,
        annotationEntry: KtAnnotationEntry,
        context: CallCheckerContext,
        element: KtModifierListOwner
    ) {
        if (element !is KtTypeReference) {
            return
        }

        val actualType = context.trace[BindingContext.TYPE, element]?.replaceAnnotations(Annotations.EMPTY) ?: return

        val parcelerType = resolvedCall.typeArguments.values.singleOrNull() ?: return
        val parcelerClass = parcelerType.constructor.declarationDescriptor as? ClassDescriptor ?: return

        val containingClass = element.getStrictParentOfType<KtClassOrObject>()
        checkIfTheContainingClassIsParcelize(containingClass, annotationEntry, context)

        fun reportElement() = annotationEntry.typeArguments.singleOrNull() ?: annotationEntry

        if (parcelerClass.kind != ClassKind.OBJECT) {
            context.trace.report(ErrorsParcelize.PARCELER_SHOULD_BE_OBJECT.on(reportElement()))
            return
        }

        fun KotlinType.fqName() = constructor.declarationDescriptor?.fqNameSafe
        val parcelerSuperType = parcelerClass.defaultType.supertypes().firstOrNull { it.fqName() == PARCELER_FQN } ?: return
        val expectedType = parcelerSuperType.arguments.singleOrNull()?.type ?: return

        if (!actualType.isSubtypeOf(expectedType)) {
            context.trace.report(ErrorsParcelize.PARCELER_TYPE_INCOMPATIBLE.on(reportElement(), expectedType, actualType))
        }
    }

    private fun checkIfTheContainingClassIsParcelize(
        containingClass: KtClassOrObject?,
        annotationEntry: KtAnnotationEntry,
        context: CallCheckerContext
    ) {
        if (containingClass != null) {
            val containingClassDescriptor = context.trace[BindingContext.CLASS, containingClass]
            if (containingClassDescriptor != null && !containingClassDescriptor.isParcelize) {
                val reportElement = (annotationEntry.typeReference?.typeElement as? KtUserType)?.referenceExpression ?: annotationEntry
                context.trace.report(ErrorsParcelize.CLASS_SHOULD_BE_PARCELIZE.on(reportElement, containingClass))
            }
        }
    }
}

internal inline fun <reified T : PsiElement> PsiElement.getStrictParentOfType(): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, true)
}

internal inline fun <reified T : PsiElement> PsiElement.getNonStrictParentOfType(): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, false)
}
