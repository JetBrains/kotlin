/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.render
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.api.types.allSupertypes
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.sirModule
import org.jetbrains.kotlin.sir.providers.source.KotlinParameterOrigin
import org.jetbrains.kotlin.sir.providers.source.KotlinTypeParameterOrigin
import org.jetbrains.kotlin.sir.providers.translateType
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.providers.utils.objCNameAnnotation
import org.jetbrains.kotlin.sir.providers.utils.updateImports
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.nodes.SirFunctionFromKtPropertySymbol
import kotlin.collections.emptyList

@OptIn(KaExperimentalApi::class)
internal inline fun <reified T : KaCallableSymbol> SirFromKtSymbol<T>.translateReturnType(
    resolveToUpperBounds: Boolean = true,
): SirType = withSessions {
    this@translateReturnType.ktSymbol.returnType.translateType(
        SirTypeVariance.COVARIANT,
        reportErrorType = { error("Can't translate return type in ${ktSymbol.render()}: ${it}") },
        reportUnsupportedType = { error("Can't translate return type in ${ktSymbol.render()}: type is not supported") },
        processTypeImports = this@translateReturnType.ktSymbol.containingModule.sirModule()::updateImports,
        resolveToUpperBounds = resolveToUpperBounds,
    )
}

@OptIn(KaExperimentalApi::class)
internal inline fun <reified T : KaCallableSymbol> SirFromKtSymbol<T>.translateInvariantType(
    resolveToUpperBounds: Boolean = true,
): SirType = withSessions {
    this@translateInvariantType.ktSymbol.returnType.translateType(
        SirTypeVariance.INVARIANT,
        reportErrorType = { error("Can't translate invariant type in ${ktSymbol.render()}: ${it}") },
        reportUnsupportedType = { error("Can't translate invariant type in ${ktSymbol.render()}: type is not supported") },
        processTypeImports = this@translateInvariantType.ktSymbol.containingModule.sirModule()::updateImports,
        resolveToUpperBounds = resolveToUpperBounds,
    )
}

internal inline fun <reified T : KaFunctionSymbol> SirFromKtSymbol<T>.hasUnsupportedTypeParameters(): Boolean = withSessions {
    if (!sirSession.collectionsV2) return@withSessions true
    val typeParameters = ktSymbol.typeParameters
    // Swift doesn't support type parameter types as constraints
    if (typeParameters.any { it.upperBounds.singleOrNull()?.collectTypeParameters()?.isNotEmpty() == true }) return@withSessions true
    val usedTypeParameters = buildList {
        addAll(ktSymbol.contextParameters.map { it.returnType })
        addIfNotNull(ktSymbol.receiverParameter?.returnType)
        addAll(ktSymbol.valueParameters.map { it.returnType })
        add(ktSymbol.returnType)
    }.flatMap { type ->
        when (type) {
            is KaTypeParameterType -> listOf(type.symbol)
            is KaClassType -> {
                val usedTypeParameters = type.collectTypeParameters()
                if (usedTypeParameters.isEmpty()) return@flatMap emptyList()
                val supportedTypeParameters = type.collectSupportedTypeParameters()
                if (usedTypeParameters.any { it !in supportedTypeParameters }) return@withSessions true
                usedTypeParameters
            }
            else -> emptyList()
        }
    }.toSet()
    if (usedTypeParameters.size != typeParameters.size) return@withSessions true
    !typeParameters.containsAll(usedTypeParameters)
}

private fun KaType.collectTypeParameters(): List<KaTypeParameterSymbol> {
    if (this is KaTypeParameterType) return listOf(this.symbol)
    if (this !is KaClassType) return emptyList()
    return typeArguments.flatMap { it.type?.collectTypeParameters() ?: emptyList() }
}

private val classIdsSupportingGenerics = listOf(StandardClassIds.List, StandardClassIds.Set, StandardClassIds.Map)

private val collectionV2ClassIds = listOf(StandardClassIds.List, StandardClassIds.MutableList)

context(sirSession: SirSession, kaSession: KaSession)
private fun KaClassType.collectSupportedTypeParameters(): List<KaTypeParameterSymbol> {
    return when {
        classId in classIdsSupportingGenerics -> typeArguments
        sirSession.collectionsV2 -> sequence {
            yield(this@collectSupportedTypeParameters)
            yieldAll(allSupertypes)
        }.filterIsInstance<KaUsualClassType>().filter { it.classId in collectionV2ClassIds }.flatMap { it.typeArguments }.toList()
        else -> emptyList()
    }.flatMap { typeArgument ->
        when (val type = typeArgument.type) {
            is KaTypeParameterType -> listOf(type.symbol)
            is KaClassType -> type.collectSupportedTypeParameters()
            else -> emptyList()
        }
    }
}

internal inline fun <reified T : KaFunctionSymbol> SirFromKtSymbol<T>.translateTypeParameters(): List<SirTypeParameter> {
    return withSessions {
        this@translateTypeParameters.ktSymbol.typeParameters.map { typeParameter ->
            val name = typeParameter.name.asString()
            val constraint = typeParameter.upperBounds.let { upperBounds ->
                when (upperBounds.size) {
                    0 -> KotlinRuntimeSupportModule.kotlinBridgeableType.optional()
                    1 -> upperBounds.single().translateType(
                        position = SirTypeVariance.INVARIANT,
                        reportErrorType = { error("Can't translate type constraint in ${ktSymbol.render()}: $it") },
                        reportUnsupportedType = { error("Can't translate type constraint in ${ktSymbol.render()}: type is not supported") },
                        processTypeImports = this@translateTypeParameters.ktSymbol.containingModule.sirModule()::updateImports,
                        resolveToUpperBounds = false,
                    )
                    else -> SirUnsupportedType
                }
            }.nonOptional().let {
                when (it) {
                    is SirNominalType -> it
                    is SirExistentialType if it.protocols.size == 1 -> SirNominalType(it.protocols[0].first)
                    else -> error("Unsupported type constraint $it")
                }
            }
            val origin = KotlinTypeParameterOrigin(typeParameter)
            SirTypeParameter(name, constraint, origin)
        }
    }
}

internal inline fun <reified T : KaFunctionSymbol> SirFromKtSymbol<T>.translateParameters(
    resolveToUpperBounds: Boolean = true,
): List<SirParameter> {
    return withSessions {
        this@translateParameters.ktSymbol.valueParameters.map { parameter ->
            val sirType = createParameterType(ktSymbol, parameter, resolveToUpperBounds).escaping
            val objCNameAnnotation = parameter.objCNameAnnotation
            val argumentName: String? = when (val raw = objCNameAnnotation?.argumentName) {
                null -> parameter.name.asString()
                "" -> null
                else -> raw
            }
            val parameterName = objCNameAnnotation?.name ?: parameter.name.asString()
            SirParameter(
                argumentName = argumentName,
                parameterName = parameterName.takeIf { it != argumentName },
                type = sirType,
                origin = KotlinParameterOrigin.ValueParameter(parameter),
                isVariadic = parameter.isVararg,
                kotlinName = parameter.name.asString(),
            )
        }
    }
}

internal inline fun <reified T : KaCallableSymbol> SirFromKtSymbol<T>.translateExtensionParameter(
    resolveToUpperBounds: Boolean = true,
): SirParameter? {
    return withSessions {
        this@translateExtensionParameter.ktSymbol.receiverParameter?.let { receiver ->
            val sirType = createParameterType(ktSymbol, receiver, resolveToUpperBounds)
            SirParameter(
                parameterName = receiver.name.asStringStripSpecialMarkers(),
                type = sirType,
                origin = KotlinParameterOrigin.ReceiverParameter(receiver)
            )
        }
    }
}

internal inline fun <reified T : KaCallableSymbol> SirFromKtSymbol<T>.translateContextParameters(
    resolveToUpperBounds: Boolean = true,
): Pair<SirParameter, List<SirParameter>>? {
    val parameters = withSessions {
        val symbol = when (this@translateContextParameters) {
            is SirFunctionFromKtPropertySymbol -> ktPropertySymbol
            else -> ktSymbol
        }
        symbol.contextParameters.map { parameter ->
            val sirType = createParameterType(ktSymbol, parameter, resolveToUpperBounds)
            SirParameter(
                parameterName = parameter.name.identifierOrNullIfSpecial,
                type = sirType,
                kotlinName = parameter.name.asString(),
            )
        }
    }
    if (parameters.isEmpty()) return null
    val type = when (parameters.size) {
        1 -> parameters.first().type
        else -> SirTupleType(parameters.map { it.parameterName to it.type })
    }
    return SirParameter(
        parameterName = "context",
        type = type,
    ) to parameters
}

@OptIn(KaExperimentalApi::class)
context(ka: KaSession, sir: SirSession)
private fun <P : KaParameterSymbol> createParameterType(
    ktSymbol: KaDeclarationSymbol,
    parameter: P,
    resolveToUpperBounds: Boolean = true,
): SirType {
    return parameter.returnType.translateType(
        position = SirTypeVariance.CONTRAVARIANT,
        reportErrorType = { error("Can't translate parameter ${parameter.render()} type in ${ktSymbol.render()}: $it") },
        reportUnsupportedType = { error("Can't translate parameter ${parameter.render()} type in ${ktSymbol.render()}: type is not supported") },
        processTypeImports = ktSymbol.containingModule.sirModule()::updateImports,
        resolveToUpperBounds = resolveToUpperBounds,
    )
}
