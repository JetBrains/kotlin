package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlinx.dataframe.plugin.extensions.impl.SchemaProperty
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

sealed interface CallShapeData {
    class Schema(val columns: List<SchemaProperty>) : CallShapeData

    class Scope(val columns: List<SchemaProperty>, val source: KtSourceElement?) : CallShapeData

    class RefinedType(val scopes: List<FirRegularClassSymbol>) : CallShapeData
}


object CallShapeAttribute : FirDeclarationDataKey()

var FirClass.callShapeData: CallShapeData? by FirDeclarationDataRegistry.data(CallShapeAttribute)
