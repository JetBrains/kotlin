/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class IdeaKotlinEntity

@IdeaKotlinEntity
annotation class IdeaKotlinModel

@IdeaKotlinEntity
annotation class IdeaKotlinService

@IdeaKotlinEntity
annotation class IdeaKotlinExtra