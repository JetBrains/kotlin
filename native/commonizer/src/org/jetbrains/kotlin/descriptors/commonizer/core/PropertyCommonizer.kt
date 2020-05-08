/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirCommonProperty
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirProperty

class PropertyCommonizer(cache: CirClassifiersCache) : AbstractFunctionOrPropertyCommonizer<CirProperty>(cache) {
    private val setter = PropertySetterCommonizer()
    private var isExternal = true

    override fun commonizationResult() = CirCommonProperty(
        name = name,
        modality = modality.result,
        visibility = visibility.result,
        isExternal = isExternal,
        extensionReceiver = extensionReceiver.result,
        returnType = returnType.result,
        kind = kind,
        setter = setter.result,
        typeParameters = typeParameters.result
    )

    override fun doCommonizeWith(next: CirProperty): Boolean {
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
