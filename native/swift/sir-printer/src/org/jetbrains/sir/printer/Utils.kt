/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer

import org.jetbrains.kotlin.sir.*

public val SirType.swift: String
    get(): String = when (this) {
        is SirExistentialType -> "Any"
        is SirNominalType -> type.swiftFqName
        is SirErrorType -> "ERROR_TYPE"
        is SirUnsupportedType -> "UNSUPPORTED_TYPE"
    }

public val SirNamedDeclaration.swiftFqName: String
    get() {
        val parentName = (parent as? SirNamedDeclaration)?.swiftFqName
            ?: ((parent as? SirNamed)?.name)
            ?: ((parent as? SirExtension)?.extendedType?.swift)
        return parentName?.let { "$it.$name" } ?: name
    }