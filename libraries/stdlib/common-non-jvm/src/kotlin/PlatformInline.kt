/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Specifies that given value class is an inline class.
 *
 * Adding or removing the annotation is a binary-incompatible change, since methods of inline classes
 * and functions with inline classes in their signatures are mangled.
 *
 * In JVM, this annotation is a synonym for [kotlin.jvm.JvmInline].
 * In other platforms, one has to use [PlatformInline] for the same purpose.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@SinceKotlin("2.5")
public actual annotation class PlatformInline

