/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

annotation class NeverConvert
annotation class NeverVerify
annotation class AlwaysVerify
annotation class DumpExpEmbeddings

// We annotate the function to indicate that the return value is unique
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Unique
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Borrowed
