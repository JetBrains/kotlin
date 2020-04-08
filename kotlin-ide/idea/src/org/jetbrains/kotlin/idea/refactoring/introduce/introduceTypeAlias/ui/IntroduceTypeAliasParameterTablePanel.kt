/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.ui

import org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.TypeParameter
import org.jetbrains.kotlin.idea.refactoring.introduce.ui.AbstractParameterTablePanel
import java.util.*

open class IntroduceTypeAliasParameterTablePanel :
    AbstractParameterTablePanel<TypeParameter, IntroduceTypeAliasParameterTablePanel.TypeParameterInfo>() {
    class TypeParameterInfo(
        originalParameter: TypeParameter
    ) : AbstractParameterTablePanel.AbstractParameterInfo<TypeParameter>(originalParameter) {
        init {
            name = originalParameter.name
        }

        override fun toParameter() = originalParameter.copy(name)
    }

    fun init(parameters: List<TypeParameter>) {
        parameterInfos = parameters.mapTo(ArrayList(), ::TypeParameterInfo)
        super.init()
    }

    override fun isCheckMarkColumnEditable() = false

    val selectedTypeParameterInfos: List<TypeParameterInfo>
        get() = parameterInfos.filter { it.isEnabled }

    val selectedTypeParameters: List<TypeParameter>
        get() = selectedTypeParameterInfos.map { it.toParameter() }
}