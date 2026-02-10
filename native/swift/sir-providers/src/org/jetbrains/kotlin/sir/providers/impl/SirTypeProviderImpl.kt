/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.*
import org.jetbrains.kotlin.sir.providers.SirTypeProvider.ErrorTypeStrategy
import org.jetbrains.kotlin.sir.providers.source.KotlinRuntimeElement
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.expandedType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public class SirTypeProviderImpl(
    private val sirSession: SirSession,
    override val errorTypeStrategy: ErrorTypeStrategy,
    override val unsupportedTypeStrategy: ErrorTypeStrategy,
) : SirTypeProvider {

    @ConsistentCopyVisibility
    public data class TypeTranslationCtx internal constructor(
        val currentPosition: SirTypeVariance,
        val reportErrorType: (String) -> Nothing,
        val reportUnsupportedType: () -> Nothing,
        val processTypeImports: (List<SirImport>) -> Unit,
        val requiresHashableAsAny: Boolean,
    ) {
        public fun anyRepresentativeType(): SirType =
            if (requiresHashableAsAny) SirType.anyHashable else KotlinRuntimeSupportModule.kotlinBridgeableType
    }

    override fun KaType.translateType(
        ktAnalysisSession: KaSession,
        position: SirTypeVariance,
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
        processTypeImports: (List<SirImport>) -> Unit,
        requiresHashableAsAny: Boolean,
    ): SirType = translateType(
        TypeTranslationCtx(
            currentPosition = position,
            reportErrorType = reportErrorType,
            reportUnsupportedType = reportUnsupportedType,
            processTypeImports = processTypeImports,
            requiresHashableAsAny = requiresHashableAsAny,
        )
    )

    private fun KaType.translateType(
        ctx: TypeTranslationCtx,
    ): SirType =
        buildSirType(this@translateType, ctx)
            .handleErrors(ctx.reportErrorType, ctx.reportUnsupportedType)
            .handleImports(ctx.processTypeImports)

    @OptIn(KaNonPublicApi::class)
    private fun buildSirType(ktType: KaType, ctx: TypeTranslationCtx): SirType {
        fun buildRegularType(kaType: KaType): SirType = sirSession.withSessions {
            when (kaType) {
                is KaUsualClassType -> {
                    when {
                        kaType.isNothingType -> SirNominalType(SirSwiftModule.never)
                        kaType.isAnyType -> ctx.anyRepresentativeType()

                        else -> {
                            if (sirSession.isClassIdSupported(kaType.classId)) {
                                val customBridge = kaType.toSirTypeBridge(ctx)?.bridge?.swiftType
                                if (customBridge != null) return@withSessions customBridge.optionalIfNeeded(kaType)
                            }

                            val classSymbol = kaType.symbol
                            when (classSymbol.sirAvailability()) {
                                is SirAvailability.Available, is SirAvailability.Hidden ->
                                    if (classSymbol is KaClassSymbol && classSymbol.classKind == KaClassKind.INTERFACE) {
                                        SirExistentialType(classSymbol.toSir().allDeclarations.firstIsInstance<SirProtocol>())
                                    } else {
                                        nominalTypeFromClassSymbol(classSymbol)
                                    }
                                is SirAvailability.Unavailable -> null
                            }
                        }
                    }
                        ?.optionalIfNeeded(kaType)
                        ?: SirUnsupportedType
                }
                is KaFunctionType -> {
                    SirFunctionalType(
                        isAsync = kaType.isSuspendFunctionType,
                        parameterTypes = listOfNotNull(
                            kaType.receiverType?.translateType(ctx.copy(currentPosition = ctx.currentPosition.flip()))
                                ?.withEscapingIfNeeded()
                        ) + kaType.parameterTypes
                            .map { it.translateType(ctx.copy(currentPosition = ctx.currentPosition.flip())).withEscapingIfNeeded() },
                        returnType = kaType.returnType.translateType(ctx.copy(currentPosition = ctx.currentPosition)),
                    )
                        .withEscapingIfNeeded()
                        .optionalIfNeeded(kaType)
                }
                is KaTypeParameterType -> ctx.translateTypeParameterType(kaType)
                is KaErrorType
                    -> SirErrorType(kaType.errorMessage)
                else
                    -> SirErrorType("Unexpected type $kaType")
            }
        }

        return ktType.abbreviation?.let { buildRegularType(it) }
            ?: buildRegularType(ktType)
    }

    private fun TypeTranslationCtx.translateTypeParameterType(type: KaTypeParameterType): SirType = sirSession.withSessions {
        val symbol = type.symbol
        val fallbackType = SirUnsupportedType
        if (symbol.isReified) return@withSessions fallbackType
        return@withSessions when (symbol.upperBounds.size) {
            0 -> anyRepresentativeType().optional()
            1 -> {
                val upperBound = symbol.upperBounds.single().translateType(this@translateTypeParameterType)
                if (type.isMarkedNullable) {
                    upperBound.optional()
                } else {
                    upperBound
                }
            }
            else -> fallbackType
        }
    }

    private fun SirType.handleErrors(
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
    ): SirType {
        if (this is SirErrorType && sirSession.errorTypeStrategy == ErrorTypeStrategy.Fail) {
            reportErrorType(reason)
        }
        if (this is SirUnsupportedType && sirSession.unsupportedTypeStrategy == ErrorTypeStrategy.Fail) {
            reportUnsupportedType()
        }
        return this
    }

    private fun SirType.handleImports(
        processTypeImports: (List<SirImport>) -> Unit,
    ): SirType {
        fun SirDeclaration.extractImport() {
            when (val origin = this.origin) {
                is KotlinSource -> {
                    val ktModule = sirSession.withSessions {
                        origin.symbol.containingModule
                    }
                    val sirModule = with(sirSession) {
                        ktModule.sirModule()
                    }
                    processTypeImports(listOf(SirImport(sirModule.name)))
                }
                is KotlinRuntimeElement -> {
                    processTypeImports(listOf(SirImport(KotlinRuntimeModule.name)))
                }
                else -> {}
            }
        }

        when (this) {
            is SirNominalType -> {
                generateSequence(this) { this.parent }.forEach { _ ->
                    typeArguments.forEach { it.handleImports(processTypeImports) }
                    typeDeclaration.extractImport()
                }
            }
            is SirExistentialType -> this.protocols.forEach { it.extractImport() }
            is SirFunctionalType -> {
                parameterTypes.forEach { it.handleImports(processTypeImports) }
                returnType.handleImports(processTypeImports)
            }
            is SirTupleType -> {
                types.forEach { (_, type) -> type.handleImports(processTypeImports) }
            }
            is SirErrorType -> {}
            SirUnsupportedType -> {}
            is SirArrayType, is SirDictionaryType, is SirOptionalType ->
                TODO("already covered by NominalType, exhaustive check is faulty here")
        }
        return this
    }

    private fun nominalTypeFromClassSymbol(
        symbol: KaClassLikeSymbol,
    ): SirNominalType? = sirSession.withSessions {
        symbol.toSir().allDeclarations.firstIsInstanceOrNull<SirScopeDefiningDeclaration>()?.let(::SirNominalType)
    }

    private fun SirType.optionalIfNeeded(originalKtType: KaType): SirType = sirSession.withSessions {
        if (originalKtType.isMarkedNullable && !originalKtType.isTypealiasToNullableType) {
            optional()
        } else {
            this@optionalIfNeeded
        }
    }

    context(ka: KaSession)
    private val KaType.isTypealiasToNullableType: Boolean
        get() = (symbol as? KaTypeAliasSymbol)?.expandedType?.isMarkedNullable ?: false

    private fun SirType.withEscapingIfNeeded(): SirType = when (this) {
        is SirFunctionalType -> copyAppendingAttributes(SirAttribute.Escaping)
        is SirNominalType -> if (isTypealiasOntoFunctionalType) {
            copyAppendingAttributes(SirAttribute.Escaping)
        } else {
            this
        }
        else -> this
    }

    private val SirNominalType.isTypealiasOntoFunctionalType: Boolean
        get() = (typeDeclaration as? SirTypealias)?.let { it.expandedType is SirFunctionalType } == true
}
