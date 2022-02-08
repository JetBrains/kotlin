/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:kotlin.jvm.JvmPackageName("kotlin.jvm.jdk8")

package kotlin.jvm

/**
 * Makes the annotation class repeatable in Java and Kotlin. A repeatable annotation can be applied more than once
 * on the same element.
 *
 * @property value the container annotation class, used to hold repeated entries of the annotation in the JVM bytecode.
 */
@SinceKotlin("1.6")
public typealias JvmRepeatable = java.lang.annotation.Repeatable
