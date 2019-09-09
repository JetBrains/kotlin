/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AbstractTypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.name.Name
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
    visibility: Visibility,
    private val isActual: Boolean
) : AbstractTypeAliasDescriptor(containingDeclaration, annotations, name, SourceElement.NO_SOURCE, visibility) {

    override lateinit var underlyingType: SimpleType
    override lateinit var expandedType: SimpleType

    private lateinit var defaultType: SimpleType
    override fun getDefaultType() = defaultType

    override val classDescriptor get() = expandedType.constructor.declarationDescriptor as? ClassDescriptor

    private lateinit var typeConstructorParameters: List<TypeParameterDescriptor>
    override fun getTypeConstructorTypeParameters() = typeConstructorParameters

    override lateinit var constructors: Collection<TypeAliasConstructorDescriptor>

    override fun isActual() = isActual

    fun initialize(underlyingType: SimpleType, expandedType: SimpleType) {
        super.initialize(emptyList())
        this.underlyingType = underlyingType
        this.expandedType = expandedType
        typeConstructorParameters = computeConstructorTypeParameters()
        defaultType = computeDefaultType()
        constructors = getTypeAliasConstructors()
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
            substitutor.safeSubstitute(underlyingType, Variance.INVARIANT).asSimpleType(),
            substitutor.safeSubstitute(expandedType, Variance.INVARIANT).asSimpleType()
        )
        return substituted
    }
}
