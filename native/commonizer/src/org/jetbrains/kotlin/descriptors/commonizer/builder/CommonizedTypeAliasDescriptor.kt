/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AbstractTypeAliasDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.asSimpleType

class CommonizedTypeAliasDescriptor(
    override val storageManager: StorageManager,
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    visibility: DescriptorVisibility,
    private val isActual: Boolean
) : AbstractTypeAliasDescriptor(containingDeclaration, annotations, name, SourceElement.NO_SOURCE, visibility) {

    private lateinit var underlyingTypeImpl: NotNullLazyValue<SimpleType>
    override val underlyingType get() = underlyingTypeImpl()

    private lateinit var expandedTypeImpl: NotNullLazyValue<SimpleType>
    override val expandedType: SimpleType get() = expandedTypeImpl()

    private val defaultTypeImpl = storageManager.createLazyValue { computeDefaultType() }
    override fun getDefaultType() = defaultTypeImpl()

    override val classDescriptor get() = expandedType.constructor.declarationDescriptor as? ClassDescriptor

    private val typeConstructorParametersImpl = storageManager.createLazyValue { computeConstructorTypeParameters() }
    override fun getTypeConstructorTypeParameters() = typeConstructorParametersImpl()

    private val constructorsImpl = storageManager.createLazyValue { getTypeAliasConstructors() }
    override val constructors get() = constructorsImpl()

    override fun isActual() = isActual

    fun initialize(
        declaredTypeParameters: List<TypeParameterDescriptor>,
        underlyingType: NotNullLazyValue<SimpleType>,
        expandedType: NotNullLazyValue<SimpleType>
    ) {
        super.initialize(declaredTypeParameters)
        underlyingTypeImpl = underlyingType
        expandedTypeImpl = expandedType
    }

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters {
        if (substitutor.isEmpty) return this
        val substituted = CommonizedTypeAliasDescriptor(
            storageManager = storageManager,
            containingDeclaration = containingDeclaration,
            annotations = annotations,
            name = name,
            visibility = visibility,
            isActual = isActual
        )
        substituted.initialize(
            declaredTypeParameters,
            storageManager.createLazyValue { substitutor.safeSubstitute(underlyingType, Variance.INVARIANT).asSimpleType() },
            storageManager.createLazyValue { substitutor.safeSubstitute(expandedType, Variance.INVARIANT).asSimpleType() }
        )
        return substituted
    }
}
