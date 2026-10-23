/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone

import java.nio.file.Path

/**
 * @param name The name of the Swift module.
 * @param sources The Swift source files of the module.
 */
public data class SwiftInputModule(
    val name: String,
    val sources: List<Path>,
)
