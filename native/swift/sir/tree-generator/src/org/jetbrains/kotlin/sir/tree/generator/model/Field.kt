/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.ListField

abstract class Field(
    override val name: String,
    override var isMutable: Boolean,
) : AbstractField<Field>() {
    override var customSetter: String? = null

    override var defaultValueInBuilder: String? = null

    override var isFinal: Boolean = false

    override fun updateFieldsInCopy(copy: Field) {
        super.updateFieldsInCopy(copy)
        copy.customSetter = customSetter
        copy.isFinal = isFinal
    }
}

class SimpleField(
    name: String,
    override var typeRef: TypeRefWithNullability,
    isMutable: Boolean,
    override val isChild: Boolean,
) : Field(name, isMutable) {

    override fun internalCopy() = SimpleField(name, typeRef, isMutable, isChild)

    override fun substituteType(map: TypeParameterSubstitutionMap) {
        typeRef = typeRef.substitute(map) as TypeRefWithNullability
    }
}

class ListField(
    name: String,
    override var baseType: TypeRef,
    private val isMutableList: Boolean,
    isMutable: Boolean,
    override val isChild: Boolean,
) : Field(name, isMutable), ListField {

    override val typeRef: ClassRef<PositionTypeParameterRef>
        get() = super.typeRef

    override val listType: ClassRef<PositionTypeParameterRef>
        get() = if (isMutableList) StandardTypes.mutableList else StandardTypes.list

    override fun internalCopy() = ListField(name, baseType, isMutableList, isMutable, isChild)

    override fun substituteType(map: TypeParameterSubstitutionMap) {
        baseType = baseType.substitute(map)
    }
}
