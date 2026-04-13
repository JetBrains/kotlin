/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

class SirImport(
    val moduleName: String,
    val mode: Mode = Mode.Default,
    val spi: List<SirAttribute.SPI> = emptyList(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SirImport) return false

        if (moduleName != other.moduleName) return false
        if (mode != other.mode) return false
        if (spi != other.spi) return false

        return true
    }

    override fun hashCode(): Int {
        var result = moduleName.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + spi.hashCode()
        return result
    }

    enum class Mode {
        ImplementationOnly,
        Default,
        Exported,
    }
}
