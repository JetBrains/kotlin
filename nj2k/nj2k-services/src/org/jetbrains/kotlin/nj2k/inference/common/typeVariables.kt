/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.types.KotlinType

sealed class TypeVariable {
    abstract val classReference: ClassReference
    abstract val typeParameters: List<TypeParameter>
    abstract var state: State
}

sealed class TypeElementData {
    abstract val typeElement: KtTypeElement
}

data class TypeElementDataImpl(override val typeElement: KtTypeElement) : TypeElementData()
data class TypeParameterElementData(
    override val typeElement: KtTypeElement,
    val typeParameterDescriptor: TypeParameterDescriptor
) : TypeElementData()

data class TypeElementBasedTypeVariable(
    override val classReference: ClassReference,
    override val typeParameters: List<TypeParameter>,
    val typeElement: TypeElementData,
    override var state: State
) : TypeVariable() {

    //ignore state as it is mutable
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypeElementBasedTypeVariable

        if (classReference != other.classReference) return false
        if (typeParameters != other.typeParameters) return false
        if (typeElement != other.typeElement) return false

        return true
    }

    override fun hashCode(): Int {
        var result = classReference.hashCode()
        result = 31 * result + typeParameters.hashCode()
        result = 31 * result + typeElement.hashCode()
        return result
    }
}

data class TypeBasedTypeVariable(
    override val classReference: ClassReference,
    override val typeParameters: List<TypeParameter>,
    val type: KotlinType,
    override var state: State
) : TypeVariable() {

    //ignore state as it is mutable
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypeBasedTypeVariable

        if (classReference != other.classReference) return false
        if (typeParameters != other.typeParameters) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = classReference.hashCode()
        result = 31 * result + typeParameters.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

val TypeVariable.isFixed: Boolean
    get() = state != State.UNKNOWN

fun TypeVariable.setStateIfNotFixed(newState: State) {
    if (!isFixed) {
        state = newState
    }
}

