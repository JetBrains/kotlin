/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.internal.JvmBuiltin

package kotlin

import kotlin.reflect.KClass

/**
 * This annotation can only be applied to a value parameter of an `equals` operator.
 * Annotating any other parameters with it results in a compilation error.
 *
 * Expresses that `equals` can only return `true` if its left-hand side is subtype of [bound]'s argument.
 * In particular,
 *  - Body of such `equals` is implicitly modified with preliminary guard checking this subtype relation;
 *  - On use-sites, compilation error is reported if left-hand side is definitely incompatible with [bound]'s argument.
 *
 * Please note that this annotation is experimental and can only be used when the corresponding language feature is enabled.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("2.5")
@Suppress("NEWER_VERSION_IN_SINCE_KOTLIN")
public annotation class EqualityBound(val bound: KClass<*>)
