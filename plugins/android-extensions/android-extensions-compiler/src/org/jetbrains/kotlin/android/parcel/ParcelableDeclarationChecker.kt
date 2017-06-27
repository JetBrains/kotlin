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

import org.jetbrains.kotlin.android.synthetic.diagnostic.ErrorsAndroid
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.SimpleDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.TypeUtils

private val ANDROID_PARCELABLE_CLASS_FQNAME = FqName("android.os.Parcelable")

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
            is ClassDescriptor -> checkParcelableClass(descriptor, declaration, diagnosticHolder)
            is PropertyDescriptor -> {
                val containingClass = descriptor.containingDeclaration as? ClassDescriptor
                val ktProperty = declaration as? KtProperty
                if (containingClass != null && ktProperty != null) {
                    checkParcelableClassProperty(descriptor, containingClass, ktProperty, diagnosticHolder)
                }
            }
        }
    }

    private fun checkParcelableClassProperty(
            property: PropertyDescriptor,
            containingClass: ClassDescriptor,
            declaration: KtProperty,
            diagnosticHolder: DiagnosticSink
    ) {
        if (!containingClass.isMagicParcelable) return

        // Do not report on calculated properties
        if (declaration.getter?.hasBody() == true) return

        if (!property.annotations.hasAnnotation(TRANSIENT_FQNAME)) {
            diagnosticHolder.report(ErrorsAndroid.PROPERTY_WONT_BE_SERIALIZED.on(declaration.nameIdentifier ?: declaration))
        }
    }

    private fun checkParcelableClass(descriptor: ClassDescriptor, declaration: KtDeclaration, diagnosticHolder: DiagnosticSink) {
        if (!descriptor.isMagicParcelable) return

        if (declaration !is KtClass || (declaration.isAnnotation() || declaration.isInterface() || declaration.isEnum())) {
            val reportElement = (declaration as? KtClassOrObject)?.nameIdentifier ?: declaration
            diagnosticHolder.report(ErrorsAndroid.PARCELABLE_SHOULD_BE_CLASS.on(reportElement))
            return
        }

        val sealedOrAbstract = declaration.modifierList?.let { it.getModifier(KtTokens.ABSTRACT_KEYWORD) ?: it.getModifier(KtTokens.SEALED_KEYWORD) }
        if (sealedOrAbstract != null) {
            diagnosticHolder.report(ErrorsAndroid.PARCELABLE_SHOULD_BE_INSTANTIABLE.on(sealedOrAbstract))
        }

        if (declaration.isInner()) {
            val reportElement = declaration.modifierList?.getModifier(KtTokens.INNER_KEYWORD) ?: declaration.nameIdentifier ?: declaration
            diagnosticHolder.report(ErrorsAndroid.PARCELABLE_CANT_BE_INNER_CLASS.on(reportElement))
        }

        if (declaration.isLocal) {
            diagnosticHolder.report(ErrorsAndroid.PARCELABLE_CANT_BE_LOCAL_CLASS.on(declaration.nameIdentifier ?: declaration))
        }

        val superTypes = TypeUtils.getAllSupertypes(descriptor.defaultType)
        if (superTypes.none { it.constructor.declarationDescriptor?.fqNameSafe == ANDROID_PARCELABLE_CLASS_FQNAME }) {
            diagnosticHolder.report(ErrorsAndroid.NO_PARCELABLE_SUPERTYPE.on(declaration.nameIdentifier ?: declaration))
        }

        val primaryConstructor = declaration.primaryConstructor
        if (primaryConstructor == null && declaration.secondaryConstructors.isNotEmpty()) {
            diagnosticHolder.report(ErrorsAndroid.PARCELABLE_SHOULD_HAVE_PRIMARY_CONSTRUCTOR.on(declaration.nameIdentifier ?: declaration))
        }

        for (parameter in primaryConstructor?.valueParameters.orEmpty()) {
            if (!parameter.hasValOrVar()) {
                diagnosticHolder.report(ErrorsAndroid.PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR.on(
                        parameter.nameIdentifier ?: parameter))
            }
        }
    }
}