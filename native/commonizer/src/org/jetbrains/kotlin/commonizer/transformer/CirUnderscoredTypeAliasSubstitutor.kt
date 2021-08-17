/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.transformer

import org.jetbrains.kotlin.commonizer.TargetDependent
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirType
import org.jetbrains.kotlin.commonizer.cir.CirTypeAliasType
import org.jetbrains.kotlin.commonizer.core.expandedType
import org.jetbrains.kotlin.commonizer.mergedtree.CirClassifierIndex
import org.jetbrains.kotlin.commonizer.mergedtree.CirTypeSubstitutor
import org.jetbrains.kotlin.commonizer.mergedtree.findTypeAlias

/**
 * Explicitly substitute [CirTypeAliasType]s whose classifier name is prefixed with a double underscore: "__"
 * with a type using a classifier without the prefix if
 * 1) Such a TypeAlias exists on the platform
 * 2) Such a TypeAlias expands to the same type
 *
 * Example from platform.posix.mkdir:
 * This function uses the following parameter:
 * linux:  `platform/posix/__mode_t -> kotlin/UInt`
 * apple:  `platform/posix/mode_t -> platform/posix/__darwin_mode_t -> platform/posix/__uint16_t -> kotlin/UShort`
 *
 * However, on linux a corresponding type alias
 * `platform/posix/mode_t -> platform/posix/__mode_t -> kotlin/UInt`
 * exists which is preferable
 */
internal class CirUnderscoredTypeAliasSubstitutor(
    private val classifierIndices: TargetDependent<CirClassifierIndex>
) : CirTypeSubstitutor {

    override fun substitute(targetIndex: Int, type: CirType): CirType {
        if (type !is CirTypeAliasType) return type

        /* TypeAliases cannot be nested and therefore are expected to have only a single name segment */
        val name = type.classifierId.relativeNameSegments.singleOrNull() ?: return type
        if (!name.name.startsWith("__")) return type

        /* Argument substitution not implemented. No real world cases known that would benefit */
        if (type.arguments.isNotEmpty()) return type

        val preferredClassifierId = CirEntityId.create(
            type.classifierId.packageName,
            CirName.create(name.name.removePrefix("__"))
        )

        val preferredTypeAlias = classifierIndices[targetIndex].findTypeAlias(preferredClassifierId) ?: return type
        val expandedType = type.expandedType()
        if (preferredTypeAlias.expandedType != expandedType) return type

        return CirTypeAliasType.createInterned(
            typeAliasId = preferredClassifierId,
            underlyingType = preferredTypeAlias.underlyingType,
            arguments = emptyList(),
            isMarkedNullable = expandedType.isMarkedNullable
        )
    }
}
