/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

class Column(val name: Name, val type: FirTypeRef)

sealed interface CallShapeData {
    class Schema(val columns: List<Column>) : CallShapeData

    class Scope(val token: FirClassSymbol<FirClass>, val columns: List<Column>) : CallShapeData

    class RefinedType(val scopes: List<FirRegularClassSymbol>) : CallShapeData
}


object CallShapeAttribute : FirDeclarationDataKey()

var FirClass.callShapeData: CallShapeData? by FirDeclarationDataRegistry.data(CallShapeAttribute)
