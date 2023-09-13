/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.viper.MangledName

interface NameMangler {
    fun mangleParameterName(parameter: FirValueParameterSymbol): MangledName
    fun mangleLocalPropertyName(property: FirPropertySymbol): MangledName
    val mangledReturnValueName: MangledName
    val mangledReturnLabelName: MangledName
}