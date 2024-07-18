/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import java.io.File
import java.io.Serializable

internal data class SwiftExportedModule(
    val moduleName: String,
    val artifact: File,
) : Serializable