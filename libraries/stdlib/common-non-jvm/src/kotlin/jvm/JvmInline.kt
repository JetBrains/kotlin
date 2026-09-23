/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

/**
 * Specifies that given value class is an inline class.
 *
 * Adding or removing the annotation is a binary-incompatible change, since methods of inline classes
 * and functions with inline classes in their signatures are mangled.
 *
 * On non-JVM platforms, it's highly recommended to use [PlatformInline] instead.
 */
@Suppress("ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION")
public actual typealias JvmInline = kotlin.PlatformInline
