/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.ir.CommonProperty
import org.jetbrains.kotlin.descriptors.commonizer.ir.Property
import org.jetbrains.kotlin.name.Name

class PropertyCommonizer : Commonizer<PropertyDescriptor, Property> {
    private enum class State {
        EMPTY,
        ERROR,
        IN_PROGRESS
    }

    private var name: Name? = null
    // TODO: visibility - what if virtual declaration?
    private val visibility = VisibilityCommonizer.lowering()
    private val modality = ModalityCommonizer.default()
    private val returnType = TypeCommonizer.default()
    private val setter = PropertySetterCommonizer.default()
    private val extensionReceiver = ExtensionReceiverCommonizer.default()

    private var state = State.EMPTY

    override val result: Property
        get() = when (state) {
            State.EMPTY, State.ERROR -> error("Can't commonize property")
            State.IN_PROGRESS -> CommonProperty(
                name = name!!,
                visibility = visibility.result,
                modality = modality.result,
                type = returnType.result,
                setter = setter.result,
                extensionReceiverType = extensionReceiver.result
            )
        }

    override fun commonizeWith(next: PropertyDescriptor): Boolean {
        if (state == State.ERROR)
            return false

        if (name == null)
            name = next.name

        val result = canBeCommonized(next)
                && visibility.commonizeWith(next.visibility)
                && modality.commonizeWith(next.modality)
                && returnType.commonizeWith(next.type)
                && setter.commonizeWith(next.setter)
                && extensionReceiver.commonizeWith(next.extensionReceiverParameter)

        // TODO: type parameters (for properties???)

        state = if (!result) State.ERROR else State.IN_PROGRESS

        return result
    }

    private fun canBeCommonized(property: PropertyDescriptor) = when {
        property.isConst -> false // expect property can't be const because expect can't have initializer
        property.isLateInit -> false // expect property can't be lateinit
        else -> true
    }
}
