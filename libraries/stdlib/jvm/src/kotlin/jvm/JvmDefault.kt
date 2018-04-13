/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind

/**
 * Specifies that a JVM default method should be generated for non-abstract Kotlin interface member.
 *
 * This annotation requires explicit compilation flag to be enabled: `-Xenable-jvm-default`.
 * Also this requires jvmTarget 1.8 or higher.
 * Adding or removing this annotation to an interface member is a binary incompatible change.
 * @JvmDefault methods are excluded from interface delegation.
 */
@SinceKotlin("1.2")
@RequireKotlin("1.2.40", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class JvmDefault
