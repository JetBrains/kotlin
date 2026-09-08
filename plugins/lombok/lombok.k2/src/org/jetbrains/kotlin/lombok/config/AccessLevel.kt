/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.config

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.lombok.generators.hasJavaOrigin

enum class AccessLevel {
    PUBLIC, MODULE, PROTECTED, PACKAGE, PRIVATE,

    /** Represents not generating anything or the complete lack of a method. */
    NONE;

    fun toVisibility(symbol: FirBasedSymbol<*>): Visibility? = when (this) {
        PUBLIC -> Visibilities.Public
        PROTECTED -> {
            if (symbol.hasJavaOrigin) {
                JavaVisibilities.ProtectedAndPackage
            } else {
                Visibilities.Protected
            }
        }
        PACKAGE, MODULE -> {
            if (symbol.hasJavaOrigin) {
                JavaVisibilities.PackageVisibility
            } else {
                null
            }
        }
        PRIVATE -> Visibilities.Private
        NONE -> null
    }
}
