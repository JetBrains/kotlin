/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Contains workarounds to suppress deprecated import without suppressing deprecation in the whole file.
 */
package org.jetbrains.kotlin.gradle.dsl

@Suppress("DEPRECATION")
internal typealias KotlinCompileDeprecated<T> = KotlinCompile<T>

@Suppress("DEPRECATION")
internal typealias KotlinJvmOptionsDeprecated = KotlinJvmOptions

@Suppress("DEPRECATION")
internal typealias KotlinCommonOptionsDeprecated = KotlinCommonOptions
