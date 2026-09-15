/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Specifies that given value class is inline class.
 *
 * Adding or removing the annotation is a binary-incompatible change, since methods of inline classes
 * and functions with inline classes in their signatures are mangled.
 *
 * In JVM, this annotation is a synonym for [kotlin.jvm.JvmInline].
 * It's the method of such a specification for non-JVM platforms.
 */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
@SinceKotlin("2.5")
public expect annotation class PlatformInline
