/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.embeddings.embedName

object NoopNameMangler : NameMangler {
    override fun mangleParameterName(parameter: FirValueParameterSymbol) = parameter.embedName()
    override fun mangleLocalPropertyName(property: FirPropertySymbol) = property.callableId.embedName()
    override val mangledReturnValueName = ReturnVariableName
    override val mangledReturnLabelName = ReturnLabelName
}