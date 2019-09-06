/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CommonProperty
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ExtensionReceiver.Companion.toReceiverNoAnnotations
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.Property

class PropertyCommonizer : CallableMemberCommonizer<PropertyDescriptor, Property>() {
    private val setter = PropertySetterCommonizer.default()
    private var isExternal = true

    override val result: Property
        get() = when (state) {
            State.EMPTY, State.ERROR -> error("Can't commonize property")
            State.IN_PROGRESS -> CommonProperty(
                name = name!!,
                modality = modality.result,
                visibility = visibility.result,
                isExternal = isExternal,
                extensionReceiver = extensionReceiver.result?.toReceiverNoAnnotations(),
                returnType = returnType.result,
                setter = setter.result
            )
        }

    override fun canBeCommonized(next: PropertyDescriptor) = when {
        next.isConst -> false // expect property can't be const because expect can't have initializer
        next.isLateInit -> false // expect property can't be lateinit
        else -> true
    }

    override fun commonizeSpecifics(next: PropertyDescriptor): Boolean {

        // TODO: type parameters (for properties???)

        isExternal = isExternal && next.isExternal

        return setter.commonizeWith(next.setter)
    }
}
