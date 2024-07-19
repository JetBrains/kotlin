/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.swiftexport

// We write explicitly about OptIn, because IDEA doesn't suggest it.
@RequiresOptIn(
    "This API is experimental and can be unstable. Add @OptIn(org.jetbrains.kotlin.swiftexport.ExperimentalSwiftExportDsl::class) annotation.",
    level = RequiresOptIn.Level.WARNING
)
annotation class ExperimentalSwiftExportDsl