/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.config

/**
 * @param publicOnly Whether only public declarations of the Swift module should be imported.
 */
public data class SwiftImportConfig(
    val publicOnly: Boolean = true,
)
