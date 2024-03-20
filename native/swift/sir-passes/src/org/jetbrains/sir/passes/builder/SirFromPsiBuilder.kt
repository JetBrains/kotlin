/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.passes.builder

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.*
import org.jetbrains.kotlin.sir.util.SirSwiftModule


context(KtAnalysisSession)
public fun KtFileSymbol.buildSirDeclarationList(): List<SirDeclaration> = getFileScope()
    .buildSirDeclarationList()
    .toList()

context(KtAnalysisSession)
private fun KtScope.buildSirDeclarationList() = getAllSymbols()
    .filter { it.isConsumableBySir() }
    .mapNotNull { it.toSirDeclaration() }

context(KtAnalysisSession)
private fun KtSymbol.isConsumableBySir(): Boolean {
    return isPublic() && when (this) {
        is KtNamedClassOrObjectSymbol -> {
            isConsumableBySirBuilder()
        }
        is KtConstructorSymbol -> {
            true
        }
        is KtFunctionSymbol -> {
            SUPPORTED_SYMBOL_ORIGINS.contains(origin)
                    && !isSuspend
                    && !isInline
                    && !isExtension
                    && !isOperator
        }
        is KtVariableSymbol -> {
            true
        }
        else -> false
    }
}

private val SUPPORTED_SYMBOL_ORIGINS = setOf(KtSymbolOrigin.SOURCE, KtSymbolOrigin.LIBRARY)

context(KtAnalysisSession)
private fun KtSymbol.toSirDeclaration(): SirDeclaration? {
    return when (this) {
        is KtNamedClassOrObjectSymbol -> {
            sirClass()
        }
        is KtConstructorSymbol -> {
            sirInit()
        }
        is KtFunctionLikeSymbol -> {
            sirFunction()
        }
        is KtVariableSymbol -> {
            sirVariable()
        }
        else -> null
    }
}

context(KtAnalysisSession)
internal fun KtNamedClassOrObjectSymbol.sirClass(): SirNamedDeclaration {
    return buildClass {
        val symbol = this@sirClass
        name = symbol.name.asString()
        origin = KotlinSource(symbol)

        declarations += symbol.getCombinedDeclaredMemberScope().buildSirDeclarationList()

        documentation = symbol.documentation()

    }.also { resultedClass ->
        resultedClass.declarations.forEach { decl -> decl.parent = resultedClass }
    }
}

context(KtAnalysisSession)
internal fun KtFunctionLikeSymbol.sirFunction(): SirFunction = buildFunction {
    val symbol = this@sirFunction
    val callableId = symbol.callableIdIfNonLocal
    origin = KotlinSource(symbol)

    kind = symbol.sirCallableKind

    name = callableId?.callableName?.asString() ?: "UNKNOWN_FUNCTION_NAME"

    symbol.valueParameters.mapTo(parameters) {
        SirParameter(
            argumentName = it.name.asString(),
            type = buildSirNominalType(it.returnType)
        )
    }
    returnType = buildSirNominalType(symbol.returnType)
    documentation = symbol.documentation()
}

context(KtAnalysisSession)
internal fun KtConstructorSymbol.sirInit(): SirInit = buildInit {
    val symbol = this@sirInit
    origin = KotlinSource(symbol)

    kind = symbol.sirCallableKind
    isFailable = false
    initKind = SirInitializerKind.ORDINARY

    symbol.valueParameters.mapTo(parameters) {
        SirParameter(
            argumentName = it.name.asString(),
            type = buildSirNominalType(it.returnType)
        )
    }

    documentation = symbol.documentation()
}

context(KtAnalysisSession)
internal fun KtVariableSymbol.sirVariable(): SirVariable = buildVariable {
    val symbol = this@sirVariable
    val callableId = symbol.callableIdIfNonLocal
    origin = KotlinSource(symbol)

    val accessorKind = symbol.sirCallableKind

    name = callableId?.callableName?.asString() ?: "UNKNOWN_VARIABLE_NAME"

    type = buildSirNominalType(symbol.returnType)

    getter = buildGetter {
        kind = accessorKind
    }
    setter = if (!symbol.isVal) buildSetter {
        kind = accessorKind
    } else null

    documentation = symbol.documentation()
}.also {
    it.getter.parent = it
    it.setter?.parent = it
}

public data class KotlinSource(
    val symbol: KtSymbol,
) : SirOrigin.Foreign.SourceCode


context(KtAnalysisSession)
private fun buildSirNominalType(it: KtType): SirNominalType = SirNominalType(
    when {
        it.isUnit -> SirSwiftModule.void

        it.isByte -> SirSwiftModule.int8
        it.isShort -> SirSwiftModule.int16
        it.isInt -> SirSwiftModule.int32
        it.isLong -> SirSwiftModule.int64

        it.isUByte -> SirSwiftModule.uint8
        it.isUShort -> SirSwiftModule.uint16
        it.isUInt -> SirSwiftModule.uint32
        it.isULong -> SirSwiftModule.uint64

        it.isBoolean -> SirSwiftModule.bool

        it.isDouble -> SirSwiftModule.double
        it.isFloat -> SirSwiftModule.float
        else ->
            throw IllegalArgumentException("Swift Export does not support argument type: ${it.asStringForDebugging()}")
    }
)

context(KtAnalysisSession)
private fun KtNamedClassOrObjectSymbol.isConsumableBySirBuilder(): Boolean =
    classKind == KtClassKind.CLASS
            && (superTypes.count() == 1 && superTypes.first().isAny) // Every class has Any as a superclass
            && !isData
            && !isInline
            && modality == Modality.FINAL

private val KtCallableSymbol.sirCallableKind: SirCallableKind
    get() = when (symbolKind) {
        KtSymbolKind.TOP_LEVEL -> {
            val isRootPackage = callableIdIfNonLocal?.packageName?.isRoot
            if (isRootPackage == true) {
                SirCallableKind.FUNCTION
            } else {
                SirCallableKind.STATIC_METHOD
            }
        }
        KtSymbolKind.CLASS_MEMBER, KtSymbolKind.ACCESSOR
        -> SirCallableKind.INSTANCE_METHOD
        KtSymbolKind.LOCAL,
        KtSymbolKind.SAM_CONSTRUCTOR
        -> TODO("encountered callable kind($symbolKind) that is not translatable currently. Fix this crash during KT-65980.")
    }

private fun KtSymbol.isPublic(): Boolean = (this as? KtSymbolWithVisibility)?.visibility?.isPublicAPI == true

private fun KtSymbol.documentation(): String? = this.psiSafe<KtDeclaration>()?.docComment?.text
