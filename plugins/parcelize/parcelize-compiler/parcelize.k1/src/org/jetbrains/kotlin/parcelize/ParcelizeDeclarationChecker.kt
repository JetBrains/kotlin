/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.builtins.isBuiltinFunctionalTypeOrSubtype
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parcelize.ParcelizeNames.OLD_PARCELER_FQN
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELABLE_FQN
import org.jetbrains.kotlin.parcelize.diagnostic.ErrorsParcelize
import org.jetbrains.kotlin.parcelize.serializers.isParcelable
import org.jetbrains.kotlin.parcelize.serializers.matchesFqNameWithSupertypes
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmFieldAnnotation
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound
import org.jetbrains.kotlin.types.typeUtil.supertypes

open class ParcelizeDeclarationChecker(val parcelizeAnnotations: List<FqName>) : DeclarationChecker {
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
        if (!containingClass.isParcelize(parcelizeAnnotations)) {
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

        if (containingClass.isParcelize(parcelizeAnnotations)
            && (declaration.hasDelegate() || bindingContext[BindingContext.BACKING_FIELD_REQUIRED, property] == true)
            && !hasIgnoredOnParcel()
            && !containingClass.hasCustomParceler()
        ) {
            val reportElement = declaration.nameIdentifier ?: declaration
            diagnosticHolder.report(ErrorsParcelize.PROPERTY_WONT_BE_SERIALIZED.on(reportElement))
        }

        // @JvmName is not applicable to property so we can check just the descriptor name
        if (property.name.asString() == "CREATOR" && property.findJvmFieldAnnotation() != null && containingClass.isCompanionObject) {
            val outerClass = containingClass.containingDeclaration as? ClassDescriptor
            if (outerClass != null && outerClass.isParcelize(parcelizeAnnotations)) {
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
            if (type.constructor.declarationDescriptor?.fqNameSafe == OLD_PARCELER_FQN) {
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
        if (!descriptor.isParcelize(parcelizeAnnotations)) {
            return
        }

        if (declaration !is KtClassOrObject) {
            diagnosticHolder.report(ErrorsParcelize.PARCELABLE_SHOULD_BE_CLASS.on(declaration))
            return
        }

        if (declaration is KtClass && (declaration.isAnnotation() || declaration.isInterface() && !declaration.isSealed())) {
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

        val abstractModifier = declaration.modifierList?.getModifier(KtTokens.ABSTRACT_KEYWORD)
        if (abstractModifier != null) {
            diagnosticHolder.report(ErrorsParcelize.PARCELABLE_SHOULD_BE_INSTANTIABLE.on(abstractModifier))
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
        if (superTypes.none { it.constructor.declarationDescriptor?.fqNameSafe == PARCELABLE_FQN }) {
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

        // The constructor checks are irrelevant for custom parcelers
        if (descriptor.hasCustomParceler()) {
            return
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
            checkParcelableClassProperty(parameter, descriptor, diagnosticHolder, typeMapper, languageVersionSettings)
        }
    }

    private fun checkParcelableClassProperty(
        parameter: KtParameter,
        containerClass: ClassDescriptor,
        diagnosticHolder: DiagnosticSink,
        typeMapper: KotlinTypeMapper,
        languageVersionSettings: LanguageVersionSettings
    ) {
        if (!parameter.hasValOrVar()) {
            val reportElement = parameter.nameIdentifier ?: parameter
            diagnosticHolder.report(ErrorsParcelize.PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR.on(reportElement))
        }

        val descriptor = typeMapper.bindingContext[BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter] ?: return

        // Don't check parameters which won't be serialized
        if (descriptor.annotations.any { it.fqName in IGNORED_ON_PARCEL_FQ_NAMES }) {
            return
        }

        val type = descriptor.type
        if (!type.isError) {
            val customParcelerTypes =
                (getTypeParcelers(descriptor.annotations) + getTypeParcelers(containerClass.annotations)).map { (mappedType, _) ->
                    mappedType
                }.toSet()

            val unsupported = checkParcelableType(type, customParcelerTypes, containerClass, languageVersionSettings)
            val reportElement = parameter.typeReference ?: parameter.nameIdentifier ?: parameter
            if (type in unsupported) {
                diagnosticHolder.report(ErrorsParcelize.PARCELABLE_TYPE_NOT_SUPPORTED.on(reportElement))
            } else {
                unsupported.forEach {
                    diagnosticHolder.report(ErrorsParcelize.PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED.on(reportElement, it))
                }
            }
        }
    }

    // Returns the set of types that are *not* supported. This set can include types other than `type`
    // if it is a generally supported container type that contains unsupported elements in this instantiation.
    private fun checkParcelableType(
        type: KotlinType,
        customParcelerTypes: Set<KotlinType>,
        containerClass: ClassDescriptor,
        languageVersionSettings: LanguageVersionSettings,
        inDataClass: Boolean = false,
    ): Set<KotlinType> {
        if (type.hasAnyAnnotation(ParcelizeNames.RAW_VALUE_ANNOTATION_FQ_NAMES)
            || type.hasAnyAnnotation(ParcelizeNames.WRITE_WITH_FQ_NAMES)
            || type in customParcelerTypes
            || type.isBuiltinFunctionalTypeOrSubtype
        ) return emptySet()

        val upperBound = type.getErasedUpperBound()
        val descriptor = upperBound.constructor.declarationDescriptor as? ClassDescriptor
            ?: return setOf(type)

        if (descriptor.kind.isSingleton || descriptor.kind.isEnumClass) {
            return emptySet()
        }

        val fqName = descriptor.fqNameSafe.asString()
        if (fqName in BuiltinParcelableTypes.PARCELABLE_BASE_TYPE_FQNAMES) {
            return emptySet()
        }

        if (fqName in BuiltinParcelableTypes.PARCELABLE_CONTAINER_FQNAMES) {
            return upperBound.arguments.fold(emptySet()) { acc, arg ->
                acc union checkParcelableType(arg.type, customParcelerTypes, containerClass, languageVersionSettings)
            }
        }

        if (BuiltinParcelableTypes.PARCELABLE_SUPERTYPE_FQNAMES.any { type.matchesFqNameWithSupertypes(it) }) {
            return emptySet()
        }

        if (descriptor.isData && (inDataClass || type.annotations.hasAnnotation(ParcelizeNames.DATA_CLASS_ANNOTATION_FQ_NAME))) {
            val scope = descriptor.getMemberScope(type.arguments)
            val primaryConstructor = descriptor.constructors.find { it.isPrimary } ?: return setOf(type)
            val properties = primaryConstructor.valueParameters.map {
                scope.getContributedVariables(it.name, NoLookupLocation.FOR_ALREADY_TRACKED).first()
            }
            // Serialization uses the property getters, deserialization uses the constructor.
            if (!DescriptorVisibilityUtils.isVisible(null, primaryConstructor, containerClass, languageVersionSettings) ||
                properties.any { !DescriptorVisibilityUtils.isVisible(null, it, containerClass, languageVersionSettings) }
            ) return setOf(type)

            return properties.fold(emptySet()) { acc, property ->
                acc union checkParcelableType(
                    property.type, customParcelerTypes, containerClass, languageVersionSettings, inDataClass = true
                )
            }
        }

        if (BuiltinParcelableTypes.EXTERNAL_SERIALIZABLE_FQNAMES.any { type.matchesFqNameWithSupertypes(it) }) {
            return emptySet()
        }

        return setOf(type)
    }

    private fun KotlinType.getErasedUpperBound(): KotlinType =
        (constructor.declarationDescriptor as? TypeParameterDescriptor)?.representativeUpperBound?.getErasedUpperBound()
            ?: this

    private fun ClassDescriptor.hasCustomParceler(): Boolean {
        val companionObjectSuperTypes = companionObjectDescriptor?.let { TypeUtils.getAllSupertypes(it.defaultType) } ?: return false
        return companionObjectSuperTypes.any { it.isParceler }
    }
}
