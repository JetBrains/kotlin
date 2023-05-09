/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

import kotlin.wasm.internal.wasmTypeId
import kotlin.wasm.internal.findAssociatedObject

/**
 * The experimental marker for associated objects API.
 *
 * Any usage of a declaration annotated with `@ExperimentalAssociatedObjects` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalAssociatedObjects::class)`,
 * or by using the compiler argument `-opt-in=kotlin.reflect.ExperimentalAssociatedObjects`.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(value = AnnotationRetention.BINARY)
public annotation class ExperimentalAssociatedObjects

/**
 * Makes the annotated annotation class an associated object key.
 *
 * An associated object key annotation should have single [KClass] parameter.
 * When applied to a class with reference to an object declaration as an argument, it binds
 * the object to the class, making this binding discoverable at runtime using [findAssociatedObject].
 */
@ExperimentalAssociatedObjects
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS)
public annotation class AssociatedObjectKey

/**
 * If [T] is an @[AssociatedObjectKey]-annotated annotation class and [this] class is annotated with @[T] (`S::class`),
 * returns object `S`.
 *
 * Otherwise returns `null`.
 */
@ExperimentalAssociatedObjects
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun <reified T : Annotation> KClass<*>.findAssociatedObject(): Any? =
    findAssociatedObject(this, wasmTypeId<T>())