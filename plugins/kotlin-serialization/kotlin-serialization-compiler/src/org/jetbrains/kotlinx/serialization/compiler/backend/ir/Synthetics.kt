package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FieldDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType


/**
 * Simple property descriptor with backing field without getters/setters for ir generation purposes
 */
class SimpleSyntheticPropertyDescriptor(
    owner: ClassDescriptor,
    name: String,
    type: KotlinType,
    isVar: Boolean = false,
    visibility: DescriptorVisibility = DescriptorVisibilities.PRIVATE
) : PropertyDescriptorImpl(
    owner,
    null,
    Annotations.EMPTY,
    Modality.FINAL,
    visibility,
    isVar,
    Name.identifier(name),
    CallableMemberDescriptor.Kind.SYNTHESIZED,
    owner.source,
    false, false, false, false, false, false
    ) {

    private val _backingField = FieldDescriptorImpl(Annotations.EMPTY, this)

    init {
        super.setType(type, emptyList(), owner.thisAsReceiverParameter, null, emptyList())
        super.initialize(null, null, _backingField, null)
    }
}
