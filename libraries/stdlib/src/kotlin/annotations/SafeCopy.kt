/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@ExperimentalStdlibApi
@SinceKotlin("2.0")
public annotation class SafeCopy

/**
 * Use-sites will still get a warning
 * Please prefer [SafeCopy]. [UnsafeCopy] will be deprecated in future versions of Kotlin
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@ExperimentalStdlibApi
@SinceKotlin("2.0")
public annotation class UnsafeCopy
