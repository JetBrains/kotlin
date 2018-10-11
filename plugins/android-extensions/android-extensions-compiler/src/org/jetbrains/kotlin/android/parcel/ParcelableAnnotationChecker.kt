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

package org.jetbrains.kotlin.android.parcel

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.TypeParceler
import kotlinx.android.parcel.WriteWith
import org.jetbrains.kotlin.android.synthetic.diagnostic.DefaultErrorMessagesAndroid
import org.jetbrains.kotlin.android.synthetic.diagnostic.ErrorsAndroid
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.name.FqName
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

class ParcelableAnnotationChecker : CallChecker {
    companion object {
        val TYPE_PARCELER_FQNAME = FqName(TypeParceler::class.java.name)
        val WRITE_WITH_FQNAME = FqName(WriteWith::class.java.name)
        val IGNORED_ON_PARCEL_FQNAME = FqName(IgnoredOnParcel::class.java.name)
    }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val constructorDescriptor = resolvedCall.resultingDescriptor as? ClassConstructorDescriptor ?: return
        val annotationClass = constructorDescriptor.constructedClass.takeIf { it.kind == ClassKind.ANNOTATION_CLASS } ?: return

        val annotationEntry = resolvedCall.call.callElement.getNonStrictParentOfType<KtAnnotationEntry>() ?: return
        val annotationOwner = annotationEntry.getStrictParentOfType<KtModifierListOwner>() ?: return

        if (annotationClass.fqNameSafe == TYPE_PARCELER_FQNAME) {
            checkTypeParcelerUsage(resolvedCall, annotationEntry, context, annotationOwner)
        }

        if (annotationClass.fqNameSafe == WRITE_WITH_FQNAME) {
            checkWriteWithUsage(resolvedCall, annotationEntry, context, annotationOwner)
        }

        if (annotationClass.fqNameSafe == IGNORED_ON_PARCEL_FQNAME) {
            checkIgnoredOnParcelUsage(annotationEntry, context, annotationOwner)
        }
    }

    private fun checkIgnoredOnParcelUsage(annotationEntry: KtAnnotationEntry, context: CallCheckerContext, element: KtModifierListOwner) {
        if (element is KtParameter && PsiTreeUtil.getParentOfType(element, KtDeclaration::class.java) is KtPrimaryConstructor) {
            context.trace.reportFromPlugin(
                ErrorsAndroid.INAPPLICABLE_IGNORED_ON_PARCEL_CONSTRUCTOR_PROPERTY.on(annotationEntry),
                DefaultErrorMessagesAndroid
            )
        } else if (element !is KtProperty || PsiTreeUtil.getParentOfType(element, KtDeclaration::class.java) !is KtClass) {
            context.trace.reportFromPlugin(ErrorsAndroid.INAPPLICABLE_IGNORED_ON_PARCEL.on(annotationEntry), DefaultErrorMessagesAndroid)
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
            .filter { it.fqName == TYPE_PARCELER_FQNAME }
            .mapNotNull { it.type.arguments.takeIf { args -> args.size == 2 }?.first()?.type }
            .count { it == thisMappedType }

        if (duplicatingAnnotationCount > 1) {
            val reportElement = annotationEntry.typeArguments.firstOrNull() ?: annotationEntry
            context.trace.reportFromPlugin(ErrorsAndroid.DUPLICATING_TYPE_PARCELERS.on(reportElement), DefaultErrorMessagesAndroid)
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
                    context.trace.reportFromPlugin(
                            ErrorsAndroid.REDUNDANT_TYPE_PARCELER.on(reportElement, containingClass), DefaultErrorMessagesAndroid)
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
        element as? KtTypeReference ?: return

        val actualType = context.trace[BindingContext.TYPE, element]?.replaceAnnotations(Annotations.EMPTY) ?: return

        val parcelerType = resolvedCall.typeArguments.values.singleOrNull() ?: return
        val parcelerClass = parcelerType.constructor.declarationDescriptor as? ClassDescriptor ?: return

        val containingClass = element.getStrictParentOfType<KtClassOrObject>()
        checkIfTheContainingClassIsParcelize(containingClass, annotationEntry, context)

        fun reportElement() = annotationEntry.typeArguments.singleOrNull() ?: annotationEntry

        if (parcelerClass.kind != ClassKind.OBJECT) {
            context.trace.reportFromPlugin(ErrorsAndroid.PARCELER_SHOULD_BE_OBJECT.on(reportElement()), DefaultErrorMessagesAndroid)
            return
        }

        fun KotlinType.fqName() = constructor.declarationDescriptor?.fqNameSafe
        val parcelerSuperType = parcelerClass.defaultType.supertypes().firstOrNull { it.fqName() == PARCELER_FQNAME } ?: return
        val expectedType = parcelerSuperType.arguments.singleOrNull()?.type ?: return

        if (!actualType.isSubtypeOf(expectedType)) {
            context.trace.reportFromPlugin(ErrorsAndroid.PARCELER_TYPE_INCOMPATIBLE.on(reportElement(), expectedType, actualType),
                                           DefaultErrorMessagesAndroid)
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
                context.trace.reportFromPlugin(ErrorsAndroid.CLASS_SHOULD_BE_PARCELIZE.on(reportElement, containingClass),
                                               DefaultErrorMessagesAndroid)
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