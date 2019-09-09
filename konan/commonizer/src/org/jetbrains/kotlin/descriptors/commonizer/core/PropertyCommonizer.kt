/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CommonProperty
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ExtensionReceiver.Companion.toReceiverNoAnnotations
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.Property

class PropertyCommonizer(cache: ClassifiersCache) : AbstractFunctionOrPropertyCommonizer<Property>(cache) {
    private val setter = PropertySetterCommonizer.default()
    private var isExternal = true

    override fun commonizationResult() = CommonProperty(
        name = name,
        modality = modality.result,
        visibility = visibility.result,
        isExternal = isExternal,
        extensionReceiver = extensionReceiver.result?.toReceiverNoAnnotations(),
        returnType = returnType.result,
        setter = setter.result,
        typeParameters = typeParameters.result
    )

    override fun doCommonizeWith(next: Property): Boolean {
        when {
            next.isConst -> return false // expect property can't be const because expect can't have initializer
            next.isLateInit -> return false // expect property can't be lateinit
        }

        val result = super.doCommonizeWith(next)
                && setter.commonizeWith(next.setter)

        if (result) {
            isExternal = isExternal && next.isExternal
        }

        return result
    }
}
