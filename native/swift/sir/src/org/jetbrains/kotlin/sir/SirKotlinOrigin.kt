/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.name.FqName

sealed interface SirKotlinOrigin : SirOrigin.Foreign {
    val fqName: FqName

    override val path: List<String>
        get() = fqName.pathSegments().map { it.asString() }

    interface Function : SirKotlinOrigin {
        val documentation: Documentation?
        val parameters: List<Parameter>
        val returnType: Type
    }

    interface Property : SirKotlinOrigin {
        val type: Type
        val isWriteable: Boolean
    }

    interface Parameter {
        val name: String
        val type: Type
    }

    interface Type {
        val name: String
    }

    interface Documentation {
        val content: String
    }
}
