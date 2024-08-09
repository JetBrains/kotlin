/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

internal const val DSL_REPLACE_PLACEHOLDER = "/*REPLACE_ME*/"

internal object SimpleSwiftExportProperties {
    const val DSL_EXPORT = "swiftexport.dsl.export"
    const val DSL_PLACEHOLDER = "swiftexport.dsl.placeholder"
    const val DSL_CUSTOM_NAME = "swiftexport.dsl.customName"
    const val DSL_FLATTEN_PACKAGE = "swiftexport.dsl.flattenPackage"
}