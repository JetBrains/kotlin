/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

/**
 * Adhoc marker for APIs that are considered 'User facing dsl'.
 *
 * Annotated APIs will be binary validated.
 *
 * Please consider exposing public DSL in the Kotlin Gradle Plugin API artifact rather than adding this annotation.
 */
internal annotation class KotlinGradlePluginPublicDsl
