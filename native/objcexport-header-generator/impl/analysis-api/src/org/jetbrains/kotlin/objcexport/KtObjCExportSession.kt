/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.utils.getOrPutNullable


sealed interface KtObjCExportSession {
    val configuration: KtObjCExportConfiguration
}

private val KtObjCExportSession.private: KtObjCExportSessionPrivate
    get() = when (this) {
        is KtObjCExportSessionPrivate -> this
    }

private interface KtObjCExportSessionPrivate : KtObjCExportSession {
    val cache: MutableMap<Any, Any?>
}

inline fun <T> KtObjCExportSession(
    configuration: KtObjCExportConfiguration,
    block: KtObjCExportSession.() -> T,
): T {
    return KtObjCExportSessionImpl(configuration, hashMapOf()).block()
}

@PublishedApi
internal class KtObjCExportSessionImpl(
    override val configuration: KtObjCExportConfiguration,
    override val cache: MutableMap<Any, Any?>,
) : KtObjCExportSessionPrivate


/**
 * Will cache the given [computation] in the current [KtObjCExportSession].
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
internal inline fun <reified T> cached(key: Any, noinline computation: () -> T): T {
    return cached(T::class.java, key, computation)
}

/**
 * @see cached
 */
context(KtObjCExportSession)
private fun <T> cached(typeOfT: Class<T>, key: Any, computation: () -> T): T {
    val value = private.cache.getOrPutNullable(key) {
        computation()
    }

    return typeOfT.cast(value)
}
