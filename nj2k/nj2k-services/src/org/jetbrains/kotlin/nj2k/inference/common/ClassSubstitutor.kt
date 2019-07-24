/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.resolve.BindingContext

class ClassSubstitutor(private val superTypes: Map<ClassifierDescriptor, Map<TypeParameterDescriptor, KtTypeElement>>) {
    operator fun get(klass: ClassDescriptor, typeParameter: TypeParameterDescriptor): KtTypeElement? =
        superTypes[klass]?.get(typeParameter)

    companion object {
        fun createFromKtClass(klass: KtClassOrObject, resolutionFacade: ResolutionFacade): ClassSubstitutor? {
            val superTypes =
                klass.superTypeListEntries.map { superType ->
                    val typeReference = superType.typeReference ?: return null
                    val type = typeReference.analyze(resolutionFacade)[BindingContext.TYPE, typeReference] ?: return null
                    val declarationDescriptor = type.constructor.declarationDescriptor ?: return null
                    declarationDescriptor to type.constructor.parameters.zip(
                        typeReference.typeElement?.typeArgumentsAsTypes ?: return null
                    ) { parameter, argument ->
                        parameter to (argument.typeElement ?: return null)
                    }.toMap()
                }.toMap()
            return ClassSubstitutor(superTypes)
        }
    }
}

