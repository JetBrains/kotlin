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

import org.jetbrains.kotlin.android.parcel.serializers.ParcelSerializer
import org.jetbrains.kotlin.android.parcel.serializers.isParcelable
import org.jetbrains.kotlin.android.synthetic.diagnostic.DefaultErrorMessagesAndroid
import org.jetbrains.kotlin.android.synthetic.diagnostic.ErrorsAndroid
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.state.IncompatibleClassTracker
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.SimpleDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmFieldAnnotation
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isError

private val ANDROID_PARCELABLE_CLASS_FQNAME = FqName("android.os.Parcelable")
internal val ANDROID_PARCEL_CLASS_FQNAME = FqName("android.os.Parcel")

class ParcelableDeclarationChecker : SimpleDeclarationChecker {
    private companion object {
        private val TRANSIENT_FQNAME = FqName(Transient::class.java.canonicalName)
    }

    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        when (descriptor) {
            is ClassDescriptor -> checkParcelableClass(descriptor, declaration, diagnosticHolder, bindingContext)
            is SimpleFunctionDescriptor -> {
                val containingClass = descriptor.containingDeclaration as? ClassDescriptor
                val ktFunction = declaration as? KtFunction
                if (containingClass != null && ktFunction != null) {
                    checkParcelableClassMethod(descriptor, containingClass, ktFunction, diagnosticHolder)
                }
            }
            is PropertyDescriptor -> {
                val containingClass = descriptor.containingDeclaration as? ClassDescriptor
                val ktProperty = declaration as? KtProperty
                if (containingClass != null && ktProperty != null) {
                    checkParcelableClassProperty(descriptor, containingClass, ktProperty, diagnosticHolder, bindingContext)
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
        if (!containingClass.isParcelize) return

        if (method.isWriteToParcel() && declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
            val reportElement = declaration.modifierList?.getModifier(KtTokens.OVERRIDE_KEYWORD) ?: declaration.nameIdentifier ?: declaration
            diagnosticHolder.reportFromPlugin(ErrorsAndroid.OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED.on(reportElement), DefaultErrorMessagesAndroid)
        }
    }

    private fun checkParcelableClassProperty(
            property: PropertyDescriptor,
            containingClass: ClassDescriptor,
            declaration: KtProperty,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (containingClass.isParcelize
                && (declaration.hasDelegate() || bindingContext[BindingContext.BACKING_FIELD_REQUIRED, property] == true)
                && !property.annotations.hasAnnotation(TRANSIENT_FQNAME)
        ) {
            val reportElement = declaration.nameIdentifier ?: declaration
            diagnosticHolder.reportFromPlugin(ErrorsAndroid.PROPERTY_WONT_BE_SERIALIZED.on(reportElement), DefaultErrorMessagesAndroid)
        }

        // @JvmName is not applicable to property so we can check just the descriptor name
        if (property.name.asString() == "CREATOR" && property.findJvmFieldAnnotation() != null && containingClass.isCompanionObject) {
            val outerClass = containingClass.containingDeclaration as? ClassDescriptor
            if (outerClass != null && outerClass.isParcelize) {
                val reportElement = declaration.nameIdentifier ?: declaration
                diagnosticHolder.reportFromPlugin(ErrorsAndroid.CREATOR_DEFINITION_IS_NOT_ALLOWED.on(reportElement), DefaultErrorMessagesAndroid)
            }
        }
    }

    private fun checkParcelableClass(
            descriptor: ClassDescriptor,
            declaration: KtDeclaration,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (!descriptor.isParcelize) return

        if (declaration !is KtClass || (declaration.isAnnotation() || declaration.isInterface())) {
            val reportElement = (declaration as? KtClassOrObject)?.nameIdentifier ?: declaration
            diagnosticHolder.reportFromPlugin(ErrorsAndroid.PARCELABLE_SHOULD_BE_CLASS.on(reportElement), DefaultErrorMessagesAndroid)
            return
        }

        if (declaration.isEnum()) {
            val reportElement = (declaration as? KtClass)?.nameIdentifier ?: declaration
            diagnosticHolder.reportFromPlugin(ErrorsAndroid.PARCELABLE_SHOULD_BE_CLASS.on(reportElement), DefaultErrorMessagesAndroid)
            return
        }

        val sealedOrAbstract = declaration.modifierList?.let { it.getModifier(KtTokens.ABSTRACT_KEYWORD) ?: it.getModifier(KtTokens.SEALED_KEYWORD) }
        if (sealedOrAbstract != null) {
            diagnosticHolder.reportFromPlugin(ErrorsAndroid.PARCELABLE_SHOULD_BE_INSTANTIABLE.on(sealedOrAbstract), DefaultErrorMessagesAndroid)
        }

        if (declaration.isInner()) {
            val reportElement = declaration.modifierList?.getModifier(KtTokens.INNER_KEYWORD) ?: declaration.nameIdentifier ?: declaration
            diagnosticHolder.reportFromPlugin(ErrorsAndroid.PARCELABLE_CANT_BE_INNER_CLASS.on(reportElement), DefaultErrorMessagesAndroid)
        }

        if (declaration.isLocal) {
            val reportElement = declaration.nameIdentifier ?: declaration
            diagnosticHolder.reportFromPlugin(ErrorsAndroid.PARCELABLE_CANT_BE_LOCAL_CLASS.on(reportElement), DefaultErrorMessagesAndroid)
        }

        val superTypes = TypeUtils.getAllSupertypes(descriptor.defaultType)
        if (superTypes.none { it.constructor.declarationDescriptor?.fqNameSafe == ANDROID_PARCELABLE_CLASS_FQNAME }) {
            val reportElement = declaration.nameIdentifier ?: declaration
            diagnosticHolder.reportFromPlugin(ErrorsAndroid.NO_PARCELABLE_SUPERTYPE.on(reportElement), DefaultErrorMessagesAndroid)
        }

        for (supertypeEntry in declaration.superTypeListEntries) {
            supertypeEntry as? KtDelegatedSuperTypeEntry ?: continue
            val delegateExpression = supertypeEntry.delegateExpression ?: continue
            val type = bindingContext[BindingContext.TYPE, supertypeEntry.typeReference] ?: continue
            if (type.isParcelable()) {
                val reportElement = supertypeEntry.byKeywordNode?.psi ?: delegateExpression
                diagnosticHolder.reportFromPlugin(ErrorsAndroid.PARCELABLE_DELEGATE_IS_NOT_ALLOWED.on(reportElement), DefaultErrorMessagesAndroid)
            }
        }

        val primaryConstructor = declaration.primaryConstructor
        if (primaryConstructor == null && declaration.secondaryConstructors.isNotEmpty()) {
            val reportElement = declaration.nameIdentifier ?: declaration
            diagnosticHolder.reportFromPlugin(ErrorsAndroid.PARCELABLE_SHOULD_HAVE_PRIMARY_CONSTRUCTOR.on(reportElement), DefaultErrorMessagesAndroid)
        } else if (primaryConstructor != null && primaryConstructor.valueParameters.isEmpty()) {
            val reportElement = declaration.nameIdentifier ?: declaration
            diagnosticHolder.reportFromPlugin(ErrorsAndroid.PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY.on(reportElement), DefaultErrorMessagesAndroid)
        }

        val typeMapper = KotlinTypeMapper(
                bindingContext,
                ClassBuilderMode.full(false),
                NoResolveFileClassesProvider,
                IncompatibleClassTracker.DoNothing,
                descriptor.module.name.asString(),
                /* isJvm8Target */ false,
                /* isJvm8TargetWithDefaults */ false)

        for (parameter in primaryConstructor?.valueParameters.orEmpty()) {
            checkParcelableClassProperty(parameter, diagnosticHolder, typeMapper)
        }
    }

    private fun checkParcelableClassProperty(parameter: KtParameter, diagnosticHolder: DiagnosticSink, typeMapper: KotlinTypeMapper) {
        if (!parameter.hasValOrVar()) {
            val reportElement = parameter.nameIdentifier ?: parameter
            diagnosticHolder.reportFromPlugin(
                    ErrorsAndroid.PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR.on(reportElement), DefaultErrorMessagesAndroid)
        }

        val descriptor = typeMapper.bindingContext[BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter] ?: return
        val type = descriptor.type

        if (!type.isError) {
            val asmType = typeMapper.mapType(type)

            try {
                ParcelSerializer.get(type, asmType, typeMapper, strict = true)
            }
            catch (e: IllegalArgumentException) {
                // get() throws IllegalArgumentException on unknown types
                val reportElement = parameter.typeReference ?: parameter.nameIdentifier ?: parameter
                diagnosticHolder.reportFromPlugin(ErrorsAndroid.PARCELABLE_TYPE_NOT_SUPPORTED.on(reportElement), DefaultErrorMessagesAndroid)
            }
        }
    }
}