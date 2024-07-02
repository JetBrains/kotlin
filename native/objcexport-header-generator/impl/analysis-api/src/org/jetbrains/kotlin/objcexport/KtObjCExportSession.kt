/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.objcexport.MethodBridgeValueParameter
import org.jetbrains.kotlin.utils.getOrPutNullable


sealed interface KtObjCExportSession {
    val configuration: KtObjCExportConfiguration

    val useSiteExportSession: KtObjCExportSession
        get() = this
}

/**
 * Internal representation of [withKtObjCExportSession].
 * All *internal* accessible services shall be added here.
 */
@PublishedApi
internal sealed interface KtObjCExportSessionInternal : KtObjCExportSession {
    val moduleNaming: KtObjCExportModuleNaming
    val moduleClassifier: KtObjCExportModuleClassifier
}

@PublishedApi
internal val KtObjCExportSession.internal: KtObjCExportSessionInternal
    get() = when (this) {
        is KtObjCExportSessionInternal -> this
    }

internal class KtObjCExportSymbolOverride(
    val name: String,
    val returnType: KaType?,
    val valueParametersAssociated: List<Pair<MethodBridgeValueParameter, KtObjCParameterData?>>?,
)

/**
 * Private representation of [withKtObjCExportSession].
 * All *private* accessible data shall only be added here and potentially
 * exposed as functions within this source file
 */
private interface KtObjCExportSessionPrivate : KtObjCExportSessionInternal {
    val cache: MutableMap<Any, Any?>
    val overrides: Map<KaSymbol, KtObjCExportSymbolOverride>
}

private val KtObjCExportSession.private: KtObjCExportSessionPrivate
    get() = when (this) {
        is KtObjCExportSessionPrivate -> this
    }

inline fun <T> withKtObjCExportSession(
    configuration: KtObjCExportConfiguration,
    moduleNaming: KtObjCExportModuleNaming = KtObjCExportModuleNaming.default,
    moduleClassifier: KtObjCExportModuleClassifier = KtObjCExportModuleClassifier.default,
    block: KtObjCExportSession.() -> T,
): T {
    return KtObjCExportSessionImpl(
        configuration = configuration,
        moduleNaming = moduleNaming,
        moduleClassifier = moduleClassifier,
        cache = hashMapOf(),
        overrides = hashMapOf(),
    ).block()
}

@PublishedApi
internal data class KtObjCExportSessionImpl(
    override val configuration: KtObjCExportConfiguration,
    override val moduleNaming: KtObjCExportModuleNaming,
    override val moduleClassifier: KtObjCExportModuleClassifier,
    override val cache: MutableMap<Any, Any?>,
    override val overrides: Map<KaSymbol, KtObjCExportSymbolOverride>,
) : KtObjCExportSessionPrivate


/**
 * Will cache the given [computation] in the current [withKtObjCExportSession].
 * Example Usage: Caching the ObjC name of 'MutableSet'
 *
 * ```kotlin
 * val mutableSetObjCName get() = cached("mutableSetOfObjCName") {
 *     "MutableSet".getObjCKotlinStdlibClassOrProtocolName().objCName
 *     //                       ^
 *     //           Requires KtObjCExportSession
 * }
 * ```
 */
internal inline fun <reified T> KtObjCExportSession.cached(key: Any, noinline computation: () -> T): T {
    return cached(T::class.java, key, computation)
}

/**
 * @see cached
 */
private fun <T> KtObjCExportSession.cached(typeOfT: Class<T>, key: Any, computation: () -> T): T {
    val value = private.cache.getOrPutNullable(key) {
        computation()
    }

    return typeOfT.cast(value)
}

/**
 * Temporarily overrides the function signature and executes the lambda with the applied change.
 * Within that lambda, the translation API will take into account the overrides.
 * The [name], [returnType] and [valueParametersAssociated] properties, however, will still return the original values
 */
fun <T> ObjCExportContext.withOverriddenSignature(
    symbol: KaFunctionSymbol,
    name: String,
    returnType: KaType?,
    valueParametersAssociated: List<Pair<MethodBridgeValueParameter, KtObjCParameterData?>>?,
    block: ObjCExportContext.(KaSymbol) -> T,
): T = runWithOverride(symbol, KtObjCExportSymbolOverride(name, returnType, valueParametersAssociated), block)

/**
 * Temporarily overrides the symbol name and executes the lambda with the applied change.
 * Within that lambda, the translation API will take into account the overridden name.
 * The [name] property, however, will still return the original name
 */
fun <T> ObjCExportContext.withOverriddenName(
    symbol: KaNamedSymbol,
    name: String,
    block: ObjCExportContext.(KaSymbol) -> T,
): T =
    runWithOverride(symbol, KtObjCExportSymbolOverride(name, null, null), block)

private fun <T, S : KaSymbol> ObjCExportContext.runWithOverride(
    symbol: S,
    override: KtObjCExportSymbolOverride,
    block: ObjCExportContext.(KaSymbol) -> T,
): T {
    val session = (exportSession.private as KtObjCExportSessionImpl).let {
        it.copy(overrides = it.overrides + (symbol to override))
    }
    return ObjCExportContext(
        kaSession = kaSession,
        exportSession = session
    ).block(symbol)
    //return session.block(symbol)
}

internal fun KtObjCExportSession.exportSessionReturnType(symbol: KaCallableSymbol): KaType =
    private.overrides[symbol]?.returnType ?: symbol.returnType

internal fun KtObjCExportSession.exportSessionValueParameters(symbol: KaFunctionSymbol): List<Pair<MethodBridgeValueParameter, KtObjCParameterData?>>? =
    private.overrides[symbol]?.valueParametersAssociated

internal fun KtObjCExportSession.exportSessionSymbolName(symbol: KaNamedSymbol): String =
    private.overrides[symbol]?.name ?: symbol.name.asString()

internal fun KtObjCExportSession.exportSessionSymbolNameOrAnonymous(symbol: KaClassifierSymbol): String =
    private.overrides[symbol]?.name ?: symbol.nameOrAnonymous.asString()
