/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.serializers

import org.jetbrains.kotlin.parcelize.ParcelizeNames
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

data class TypeParcelerMapping(
    val mappedType: KotlinType,
    val parcelerType: KotlinType,
)

fun KotlinType.isParcelable() = matchesFqNameWithSupertypes(ParcelizeNames.PARCELABLE_FQN.asString())

fun KotlinType.matchesFqNameWithSupertypes(fqName: String): Boolean {
    if (this.matchesFqName(fqName)) {
        return true
    }

    return TypeUtils.getAllSupertypes(this).any { it.matchesFqName(fqName) }
}

fun KotlinType.matchesFqName(fqName: String): Boolean {
    return this.constructor.declarationDescriptor?.fqNameSafe?.asString() == fqName
}
