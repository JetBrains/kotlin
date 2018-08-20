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
 * Usages of this annotation require an explicit compilation argument to be specified:
 * either `-Xjvm-default=enable` or `-Xjvm-default=compatibility`.
 *
 * * with `-Xjvm-default=enable`, only default method in interface is generated for each @[JvmDefault] method.
 *   In this mode, annotating an existing method with @[JvmDefault] can break binary compatibility, because it will effectively
 *   remove the method from the `DefaultImpls` class.
 * * with `-Xjvm-default=compatibility`, in addition to the default interface method, a compatibility accessor is generated
 *   in the `DefaultImpls` class, that calls the default interface method via a synthetic accessor.
 *   In this mode, annotating an existing method with @[JvmDefault] is binary compatible, but results in more methods in bytecode.
 *
 * Removing this annotation from an interface member is a binary incompatible change in both modes.
 *
 * Generation of default methods is only possible with JVM target bytecode version 1.8 (`-jvm-target 1.8`) or higher.
 *
 * @[JvmDefault] methods are excluded from interface delegation.
 */
@SinceKotlin("1.2")
@RequireKotlin("1.2.40", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class JvmDefault
