/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.scala.MangledName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Representation for Kotlin local variable names.
 */
data class LocalName(val name: Name) : MangledName {
    override val mangled: String
        get() = "local\$${name.asStringStripSpecialMarkers()}"
}

/**
 * This is a barebones representation of global names.  We'll need to
 * expand it to include classes, but let's keep things simple for now.
 */
data class GlobalName(val packageName: FqName, val name: Name) : MangledName {
    override val mangled: String
        get() = "global\$pkg_${packageName.asString()}\$${name.asStringStripSpecialMarkers()}"
}

fun FirValueParameterSymbol.embedName(): LocalName = LocalName(name)
fun CallableId.embedName(): MangledName = if (isLocal) LocalName(callableName) else GlobalName(packageName, callableName)