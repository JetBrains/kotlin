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

/**
 * Specifies that given class will become a value class in the future.
 *
 * This annotation has a number of certain requirements for a class it's used on;
 * shortly, these requirements are similar to those used for real value classes.
 * Compiler will report warnings or errors on use-sites when the class is assuming to have identity.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@SinceKotlin("2.5")
public annotation class WillBecomeValue
