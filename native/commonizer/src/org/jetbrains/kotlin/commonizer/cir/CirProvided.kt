/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.cir.ArtificialSupertypes.artificialSupertypes
import org.jetbrains.kotlin.commonizer.utils.CNAMES_STRUCTS_PACKAGE
import org.jetbrains.kotlin.commonizer.utils.OBJCNAMES_CLASSES_PACKAGE
import org.jetbrains.kotlin.commonizer.utils.OBJCNAMES_PROTOCOLS_PACKAGE
import org.jetbrains.kotlin.commonizer.utils.isUnderKotlinNativeSyntheticPackages
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.types.Variance

object CirProvided {
    /* Classifiers */
    sealed interface Classifier: AnyClassifier {
        val typeParameters: List<TypeParameter>
    }

    sealed interface Class : Classifier, AnyClass {
        override val visibility: Visibility
        val supertypes: List<Type>
    }

    data class RegularClass(
        override val typeParameters: List<TypeParameter>,
        override val supertypes: List<Type>,
        override val visibility: Visibility,
        val kind: ClassKind
    ) : Class

    data class ExportedForwardDeclarationClass(val syntheticClassId: CirEntityId) : Class {
        init {
            check(syntheticClassId.packageName.isUnderKotlinNativeSyntheticPackages)
        }

        override val typeParameters: List<TypeParameter> get() = emptyList()
        override val visibility: Visibility get() = Visibilities.Public
        override val supertypes: List<Type> = syntheticClassId.artificialSupertypes()
    }

    data class TypeAlias(
        override val typeParameters: List<TypeParameter>,
        override val underlyingType: ClassOrTypeAliasType
    ) : Classifier, AnyTypeAlias

    /* Type parameter */
    data class TypeParameter(val index: Int, val variance: Variance)

    /* Types */
    sealed interface Type: AnyType {
        override val isMarkedNullable: Boolean
    }

    sealed interface ClassOrTypeAliasType : Type, AnyClassOrTypeAliasType {
        override val classifierId: CirEntityId
        val arguments: List<TypeProjection>
    }

    data class TypeParameterType(
        val index: Int,
        override val isMarkedNullable: Boolean
    ) : Type

    data class ClassType(
        override val classifierId: CirEntityId,
        override val arguments: List<TypeProjection>,
        override val isMarkedNullable: Boolean,
        val outerType: ClassType?
    ) : ClassOrTypeAliasType

    data class TypeAliasType(
        override val classifierId: CirEntityId,
        override val arguments: List<TypeProjection>,
        override val isMarkedNullable: Boolean
    ) : ClassOrTypeAliasType

    /* Type projections */
    sealed interface TypeProjection
    object StarTypeProjection : TypeProjection
    data class RegularTypeProjection(val variance: Variance, val type: Type) : TypeProjection
}

/**
 * Analog to "KlibResolvedModuleDescriptorsFactoryImpl.createForwardDeclarationsModule" which also
 * automatically assumes relevant supertypes for forward declarations based upon the package they are in.
 */
private object ArtificialSupertypes {
    private fun createType(classId: String): CirProvided.ClassType {
        return CirProvided.ClassType(
            classifierId = CirEntityId.create(classId),
            outerType = null, arguments = emptyList(), isMarkedNullable = false
        )
    }

    private val cOpaqueType = listOf(createType("kotlinx/cinterop/COpaque"))
    private val objcObjectBase = listOf(createType("kotlinx/cinterop/ObjCObjectBase"))
    private val objcCObject = listOf(createType("kotlinx/cinterop/ObjCObject"))

    fun CirEntityId.artificialSupertypes(): List<CirProvided.Type> {
        return when (packageName) {
            CNAMES_STRUCTS_PACKAGE -> cOpaqueType
            OBJCNAMES_CLASSES_PACKAGE -> objcObjectBase
            OBJCNAMES_PROTOCOLS_PACKAGE -> objcCObject
            else -> emptyList()
        }
    }
}
