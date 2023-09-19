/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:kotlin.jvm.JvmName("AutoCloseableKt")
@file:kotlin.jvm.JvmPackageName("kotlin.jdk7")

package kotlin

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// TODO: Introduce this typealias when Common AutoCloseable is stabilized.
//   This typealias shadows existing java.lang.AutoCloseable, and the experimental status would require
//   opt-in in exiting usages (that are w/o package prefix). Hence it would break previously compilable code.
//@Suppress("ACTUAL_WITHOUT_EXPECT")
//@SinceKotlin("1.8")
//public actual typealias AutoCloseable = java.lang.AutoCloseable

@Suppress("ACTUAL_WITHOUT_EXPECT")
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
// TODO: remove java.lang package prefix when the kotlin.AutoCloseable typealias is introduced and KT-55392 is fixed.
//   The prefix is currently needed for the current dokka to generate correct signature.
public actual inline fun <T : java.lang.AutoCloseable?, R> T.use(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        this.closeFinally(exception)
    }
}

@SinceKotlin("1.2")
@PublishedApi
internal fun java.lang.AutoCloseable?.closeFinally(cause: Throwable?): Unit = when {
    this == null -> {}
    cause == null -> close()
    else ->
        try {
            close()
        } catch (closeException: Throwable) {
            cause.addSuppressed(closeException)
        }
}