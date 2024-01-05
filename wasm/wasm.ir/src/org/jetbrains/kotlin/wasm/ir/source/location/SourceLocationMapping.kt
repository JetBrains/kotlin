/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.source.location

abstract class SourceLocationMapping {
    abstract val sourceLocation: SourceLocation
    abstract val generatedLocation: SourceLocation.Location
}
