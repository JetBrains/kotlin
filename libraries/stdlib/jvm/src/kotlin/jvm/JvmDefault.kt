/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind

/**
 *
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
 *
 * #
 * This annotation is **deprecated** in favor of new compiler arguments `-Xjvm-default=all-compatibility` and `-Xjvm-default=all`.
 * The new arguments allow all interface methods with bodies to be generated as JVM default methods on JVM target 1.8+.
 * Please refer to the [official documentation](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#default-methods-in-interfaces)
 * for more information.
 *
 */
@SinceKotlin("1.2")
@RequireKotlin("1.2.40", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Deprecated("Switch to new -Xjvm-default options: `all` or `all-compatibility`")
annotation class JvmDefault
