/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

import kotlin.wasm.internal.wasmTypeId
import kotlin.wasm.internal.findAssociatedObject

/**
 * If [T] is an @[AssociatedObjectKey]-annotated annotation class and [this] class is annotated with @[T] (`S::class`),
 * returns object `S`.
 *
 * Otherwise returns `null`.
 */
@ExperimentalAssociatedObjects
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public actual inline fun <reified T : Annotation> KClass<*>.findAssociatedObject(): Any? =
    findAssociatedObject(this, wasmTypeId<T>())