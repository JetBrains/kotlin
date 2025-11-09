/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("kotlin.jvm.jdk8")

package kotlin.jvm

import java.lang.annotation.Inherited

/**
 * Makes the annotation class "inherited" in Java and Kotlin. An annotation declared on a class will be visible on sub-classes
 * unless overridden by another instance of the same annotation class.
 */
@SinceKotlin("2.3")
public typealias JvmInherited = Inherited
