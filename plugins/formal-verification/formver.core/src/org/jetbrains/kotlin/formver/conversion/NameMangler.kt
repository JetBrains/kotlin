/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.embeddings.embedName
import org.jetbrains.kotlin.formver.embeddings.embedPropertyName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.Name

class NameMangler(
    val mangledReturnValueName: MangledName = ReturnVariableName,
    private val substitutionParams: Map<Name, SubstitutionItem> = mapOf(),
    returnLabelIndex: Int? = null
) {
    fun mangleParameterName(parameter: FirValueParameterSymbol) = substitutionParams[parameter.name]?.name ?: parameter.embedName()
    fun mangleLocalPropertyName(property: FirPropertySymbol, scopeDepth: Int) = property.callableId.embedPropertyName(scopeDepth)

    val mangledReturnLabelName: MangledName = ReturnLabelName(returnLabelIndex)
}