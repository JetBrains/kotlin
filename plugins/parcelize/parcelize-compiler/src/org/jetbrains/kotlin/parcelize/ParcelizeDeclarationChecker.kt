/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.FrameMap
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parcelize.diagnostic.ErrorsParcelize
import org.jetbrains.kotlin.parcelize.serializers.ParcelSerializer
import org.jetbrains.kotlin.parcelize.serializers.isParcelable
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmFieldAnnotation
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.supertypes

val ANDROID_PARCELABLE_CLASS_FQNAME = FqName("android.os.Parcelable")
val ANDROID_PARCELABLE_CREATOR_CLASS_FQNAME = FqName("android.os.Parcelable.Creator")
val ANDROID_PARCEL_CLASS_FQNAME = FqName("android.os.Parcel")

open class ParcelizeDeclarationChecker : DeclarationChecker {
    private companion object {
        private val IGNORED_ON_PARCEL_FQ_NAMES = listOf(
            FqName("kotlinx.parcelize.IgnoredOnParcel"),
            FqName("kotlinx.android.parcel.IgnoredOnParcel")
        )
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val trace = context.trace

        when (descriptor) {
            is ClassDescriptor -> {
                checkParcelableClass(descriptor, declaration, trace, trace.bindingContext, context.languageVersionSettings)
                checkParcelerClass(descriptor, declaration, trace)
            }
            is SimpleFunctionDescriptor -> {
                val containingClass = descriptor.containingDeclaration as? ClassDescriptor
                val ktFunction = declaration as? KtFunction
                if (containingClass != null && ktFunction != null) {
                    checkParcelableClassMethod(descriptor, containingClass, ktFunction, trace)
                }
            }
            is PropertyDescriptor -> {
                val containingClass = descriptor.containingDeclaration as? ClassDescriptor
                val ktProperty = declaration as? KtProperty
                if (containingClass != null && ktProperty != null) {
                    checkParcelableClassProperty(descriptor, containingClass, ktProperty, trace, trace.bindingContext)
                }
            }
        }
    }

    private fun checkParcelableClassMethod(
        method: SimpleFunctionDescriptor,
        containingClass: ClassDescriptor,
        declaration: KtFunction,
        diagnosticHolder: DiagnosticSink
    ) {
        if (!containingClass.isParcelize) {
            return
        }

        if (method.isWriteToParcel() && declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
            val reportElement = declaration.modifierList?.getModifier(KtTokens.OVERRIDE_KEYWORD)
                ?: declaration.nameIdentifier
                ?: declaration

            diagnosticHolder.report(ErrorsParcelize.OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED.on(reportElement))
        }
    }

    private fun checkParcelableClassProperty(
        property: PropertyDescriptor,
        containingClass: ClassDescriptor,
        declaration: KtProperty,
        diagnosticHolder: DiagnosticSink,
        bindingContext: BindingContext
    ) {
        fun hasIgnoredOnParcel(): Boolean {
            fun Annotations.hasIgnoredOnParcel() = any { it.fqName in IGNORED_ON_PARCEL_FQ_NAMES }

            return property.annotations.hasIgnoredOnParcel() || (property.getter?.annotations?.hasIgnoredOnParcel() ?: false)
        }

        if (containingClass.isParcelize
            && (declaration.hasDelegate() || bindingContext[BindingContext.BACKING_FIELD_REQUIRED, property] == true)
            && !hasIgnoredOnParcel()
        ) {
            val reportElement = declaration.nameIdentifier ?: declaration
            diagnosticHolder.report(ErrorsParcelize.PROPERTY_WONT_BE_SERIALIZED.on(reportElement))
        }

        // @JvmName is not applicable to property so we can check just the descriptor name
        if (property.name.asString() == "CREATOR" && property.findJvmFieldAnnotation() != null && containingClass.isCompanionObject) {
            val outerClass = containingClass.containingDeclaration as? ClassDescriptor
            if (outerClass != null && outerClass.isParcelize) {
                val reportElement = declaration.nameIdentifier ?: declaration
                diagnosticHolder.report(ErrorsParcelize.CREATOR_DEFINITION_IS_NOT_ALLOWED.on(reportElement))
            }
        }
    }

    private fun checkParcelerClass(
        descriptor: ClassDescriptor,
        declaration: KtDeclaration,
        diagnosticHolder: DiagnosticSink,
    ) {
        if (!descriptor.isCompanionObject || declaration !is KtObjectDeclaration) {
            return
        }

        for (type in descriptor.defaultType.supertypes()) {
            if (type.constructor.declarationDescriptor?.fqNameSafe == OLD_PARCELER_FQNAME) {
                val reportElement = declaration.nameIdentifier ?: declaration.getObjectKeyword() ?: declaration
                diagnosticHolder.report(ErrorsParcelize.DEPRECATED_PARCELER.on(reportElement))
                break
            }
        }
    }

    private fun checkParcelableClass(
        descriptor: ClassDescriptor,
        declaration: KtDeclaration,
        diagnosticHolder: DiagnosticSink,
        bindingContext: BindingContext,
        languageVersionSettings: LanguageVersionSettings
    ) {
        if (!descriptor.isParcelize) {
            return
        }

        if (declaration !is KtClassOrObject) {
            diagnosticHolder.report(ErrorsParcelize.PARCELABLE_SHOULD_BE_CLASS.on(declaration))
            return
        }

        if (declaration is KtClass && (declaration.isAnnotation() || declaration.isInterface())) {
            val reportElement = declaration.nameIdentifier ?: declaration
            diagnosticHolder.report(ErrorsParcelize.PARCELABLE_SHOULD_BE_CLASS.on(reportElement))
            return
        }

        for (companion in declaration.companionObjects) {
            if (companion.name == "CREATOR") {
                val reportElement = companion.nameIdentifier ?: companion
                diagnosticHolder.report(ErrorsParcelize.CREATOR_DEFINITION_IS_NOT_ALLOWED.on(reportElement))
            }
        }

        val sealedOrAbstract =
            declaration.modifierList?.let { it.getModifier(KtTokens.ABSTRACT_KEYWORD) ?: it.getModifier(KtTokens.SEALED_KEYWORD) }
        if (sealedOrAbstract != null) {
            diagnosticHolder.report(ErrorsParcelize.PARCELABLE_SHOULD_BE_INSTANTIABLE.on(sealedOrAbstract))
        }

        if (declaration is KtClass && declaration.isInner()) {
            val reportElement = declaration.modifierList?.getModifier(KtTokens.INNER_KEYWORD) ?: declaration.nameIdentifier ?: declaration
            diagnosticHolder.report(ErrorsParcelize.PARCELABLE_CANT_BE_INNER_CLASS.on(reportElement))
        }

        if (declaration.isLocal) {
            val reportElement = declaration.nameIdentifier ?: declaration
            diagnosticHolder.report(ErrorsParcelize.PARCELABLE_CANT_BE_LOCAL_CLASS.on(reportElement))
        }

        val superTypes = TypeUtils.getAllSupertypes(descriptor.defaultType)
        if (superTypes.none { it.constructor.declarationDescriptor?.fqNameSafe == ANDROID_PARCELABLE_CLASS_FQNAME }) {
            val reportElement = declaration.nameIdentifier ?: declaration
            diagnosticHolder.report(ErrorsParcelize.NO_PARCELABLE_SUPERTYPE.on(reportElement))
        }

        for (supertypeEntry in declaration.superTypeListEntries) {
            supertypeEntry as? KtDelegatedSuperTypeEntry ?: continue
            val delegateExpression = supertypeEntry.delegateExpression ?: continue
            val type = bindingContext[BindingContext.TYPE, supertypeEntry.typeReference] ?: continue
            if (type.isParcelable()) {
                val reportElement = supertypeEntry.byKeywordNode?.psi ?: delegateExpression
                diagnosticHolder.report(ErrorsParcelize.PARCELABLE_DELEGATE_IS_NOT_ALLOWED.on(reportElement))
            }
        }

        val primaryConstructor = declaration.primaryConstructor
        if (primaryConstructor == null && declaration.secondaryConstructors.isNotEmpty()) {
            val reportElement = declaration.nameIdentifier ?: declaration
            diagnosticHolder.report(ErrorsParcelize.PARCELABLE_SHOULD_HAVE_PRIMARY_CONSTRUCTOR.on(reportElement))
        } else if (primaryConstructor != null && primaryConstructor.valueParameters.isEmpty()) {
            val reportElement = declaration.nameIdentifier ?: declaration
            diagnosticHolder.report(ErrorsParcelize.PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY.on(reportElement))
        }

        val typeMapper = KotlinTypeMapper(
            bindingContext,
            ClassBuilderMode.FULL,
            descriptor.module.name.asString(),
            languageVersionSettings,
            useOldInlineClassesManglingScheme = false
        )

        for (parameter in primaryConstructor?.valueParameters.orEmpty<KtParameter>()) {
            checkParcelableClassProperty(parameter, descriptor, diagnosticHolder, typeMapper)
        }
    }

    private fun checkParcelableClassProperty(
        parameter: KtParameter,
        containerClass: ClassDescriptor,
        diagnosticHolder: DiagnosticSink,
        typeMapper: KotlinTypeMapper
    ) {
        if (!parameter.hasValOrVar()) {
            val reportElement = parameter.nameIdentifier ?: parameter
            diagnosticHolder.report(ErrorsParcelize.PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR.on(reportElement))
        }

        val descriptor = typeMapper.bindingContext[BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter] ?: return
        val type = descriptor.type

        if (!type.isError && !containerClass.hasCustomParceler()) {
            val asmType = typeMapper.mapType(type)

            try {
                val parcelers = getTypeParcelers(descriptor.annotations) + getTypeParcelers(containerClass.annotations)
                val context = ParcelSerializer.ParcelSerializerContext(
                    typeMapper,
                    typeMapper.mapType(containerClass.defaultType),
                    parcelers,
                    FrameMap()
                )

                ParcelSerializer.get(type, asmType, context, strict = true)
            } catch (e: IllegalArgumentException) {
                // get() throws IllegalArgumentException on unknown types
                val reportElement = parameter.typeReference ?: parameter.nameIdentifier ?: parameter
                diagnosticHolder.report(ErrorsParcelize.PARCELABLE_TYPE_NOT_SUPPORTED.on(reportElement))
            }
        }
    }

    private fun ClassDescriptor.hasCustomParceler(): Boolean {
        val companionObjectSuperTypes = companionObjectDescriptor?.let { TypeUtils.getAllSupertypes(it.defaultType) } ?: return false
        return companionObjectSuperTypes.any { it.isParceler }
    }
}
