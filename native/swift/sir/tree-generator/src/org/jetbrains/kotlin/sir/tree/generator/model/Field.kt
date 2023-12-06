/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.ListField

abstract class Field(
    override val name: String,
    override var isMutable: Boolean
) : AbstractField<Field>(), AbstractFieldWithDefaultValue<Field> {

    override val origin: Field
        get() = this

    override var withGetter: Boolean = false

    override var customSetter: String? = null

    override var defaultValueInImplementation: String? = null

    override var defaultValueInBuilder: String? = null

    override val isVolatile: Boolean
        get() = false

    override var isFinal: Boolean = false

    override val isParameter: Boolean
        get() = false

    abstract fun internalCopy(): Field

    override fun copy() = internalCopy().also(::updateFieldsInCopy)

    override fun updateFieldsInCopy(copy: Field) {
        super.updateFieldsInCopy(copy)
        copy.withGetter = withGetter
        copy.customSetter = customSetter
        copy.defaultValueInImplementation = defaultValueInImplementation
        copy.isFinal = isFinal
    }
}

class SimpleField(
    name: String,
    override val typeRef: TypeRefWithNullability,
    isMutable: Boolean,
) : Field(name, isMutable) {

    override fun internalCopy() = SimpleField(name, typeRef, isMutable)

    override fun replaceType(newType: TypeRefWithNullability) = SimpleField(name, newType, isMutable).also(::updateFieldsInCopy)
}

class ListField(
    name: String,
    override val baseType: TypeRef,
    isMutable: Boolean,
) : Field(name, isMutable), ListField {

    override val typeRef: ClassRef<PositionTypeParameterRef>
        get() = super.typeRef

    override val listType: ClassRef<PositionTypeParameterRef>
        get() = StandardTypes.list

    override fun internalCopy() = ListField(name, baseType, isMutable)

    override fun replaceType(newType: TypeRefWithNullability) = internalCopy().also(::updateFieldsInCopy)
}
