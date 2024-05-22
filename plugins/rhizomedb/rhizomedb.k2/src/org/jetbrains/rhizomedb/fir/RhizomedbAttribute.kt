/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.Name

enum class RhizomedbAttributeKind {
    REQUIRED, OPTIONAL, MANY
}

data class RhizomedbAttribute(
    val propertyName: Name,
    val entityKType: ConeKotlinType,
    val valueKType: ConeKotlinType,
    val kind: RhizomedbAttributeKind,
)

val RhizomedbAttribute.attributeName: Name get() = propertyName.toAttributeName()
val RhizomedbAttribute.attributeType: ConeKotlinType
    get() {
        val attributeType = when (kind) {
            RhizomedbAttributeKind.REQUIRED -> RhizomedbSymbolNames.requiredClassId
            RhizomedbAttributeKind.OPTIONAL -> RhizomedbSymbolNames.optionalClassId
            RhizomedbAttributeKind.MANY -> RhizomedbSymbolNames.manyClassId
        }
        return attributeType.constructClassLikeType(arrayOf(valueKType, entityKType))
    }

fun Name.toAttributeName(): Name = Name.identifier(identifier + "Attr")
fun Name.toPropertyName(): Name? {
    return if (identifier.endsWith("Attr")) Name.identifier(identifier.removeSuffix("Attr"))
    else null
}