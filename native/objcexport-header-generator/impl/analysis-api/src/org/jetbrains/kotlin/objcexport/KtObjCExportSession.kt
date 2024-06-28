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
 * context(KtObjCExportSession)
 * val mutableSetObjCName get() = cached("mutableSetOfObjCName") {
 *     "MutableSet".getObjCKotlinStdlibClassOrProtocolName().objCName
 *     //                       ^
 *     //           Requires KtObjCExportSession
 * }
 * ```
 */
context(KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal inline fun <reified T> cached(key: Any, noinline computation: () -> T): T {
    return cached(T::class.java, key, computation)
}

/**
 * @see cached
 */
context(KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun <T> cached(typeOfT: Class<T>, key: Any, computation: () -> T): T {
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
context(KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun <T> KaFunctionSymbol.withOverriddenSignature(
    name: String,
    returnType: KaType?,
    valueParametersAssociated: List<Pair<MethodBridgeValueParameter, KtObjCParameterData?>>?,
    block: context(KtObjCExportSession, KaFunctionSymbol) () -> T,
): T = runWithOverride(KtObjCExportSymbolOverride(name, returnType, valueParametersAssociated), block)

/**
 * Temporarily overrides the symbol name and executes the lambda with the applied change.
 * Within that lambda, the translation API will take into account the overridden name.
 * The [name] property, however, will still return the original name
 */
context(KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun <T> KaNamedSymbol.withOverriddenName(name: String, block: context(KtObjCExportSession, KaNamedSymbol) () -> T): T =
    runWithOverride(KtObjCExportSymbolOverride(name, null, null), block)

context(KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun <T, S : KaSymbol> S.runWithOverride(override: KtObjCExportSymbolOverride, block: context(KtObjCExportSession) S.() -> T): T {
    val session = (private as KtObjCExportSessionImpl).let { it.copy(overrides = it.overrides + (this to override)) }
    return block(session, this)
}

context(KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KaCallableSymbol.exportSessionReturnType(): KaType =
    private.overrides[this]?.returnType ?: returnType

context(KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KaFunctionSymbol.exportSessionValueParameters(): List<Pair<MethodBridgeValueParameter, KtObjCParameterData?>>? =
    private.overrides[this]?.valueParametersAssociated

context(KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KaNamedSymbol.exportSessionSymbolName(): String =
    private.overrides[this]?.name ?: name.asString()

context(KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KaClassifierSymbol.exportSessionSymbolNameOrAnonymous(): String =
    private.overrides[this]?.name ?: nameOrAnonymous.asString()
