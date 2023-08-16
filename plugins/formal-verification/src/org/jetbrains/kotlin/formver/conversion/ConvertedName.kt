/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Represents a Kotlin name with its Viper equivalent.
 *
 * We could directly convert names and pass them around as strings, but this
 * approach makes it easier to see where they came from during debugging.
 */
interface ConvertedName {
    val asString: String
}

/**
 * Representation for Kotlin local variable names.
 */
data class LocalName(val name: Name) : ConvertedName {
    override val asString: String
        get() = "local\$${name.asStringStripSpecialMarkers()}"
}

/**
 * Representation for names not present in the original source,
 * e.g. storage for the result of subexpressions.
 */
data class AnonymousName(val n: Int) : ConvertedName {
    override val asString: String
        get() = "anonymous\$$n"
}

/**
 * This is a barebones representation of global names.  We'll need to
 * expand it to include classes, but let's keep things simple for now.
 */
data class GlobalName(val packageName: FqName, val name: Name) : ConvertedName {
    override val asString: String
        get() = "global\$pkg_${packageName.asString()}\$${name.asStringStripSpecialMarkers()}"
}

data object ReturnVariableName : ConvertedName {
    override val asString: String
        get() = "ret\$"
}

/**
 * We also convert domain names and their function and axiom names as
 * they have to be globally unique as well.
 */
data class ConvertedDomainName(val name: String) : ConvertedName {
    // Info: Can't use 'domain' as prefix as Viper recognizes it as a keyword
    override val asString: String = "dom\$$name"
}

data class ConvertedDomainFuncName(val domainName: ConvertedDomainName, val funcName: String) : ConvertedName {
    override val asString: String = "${domainName.asString}\$${funcName}"
}

data class ConvertedDomainAxiomName(val domainName: ConvertedDomainName, val axiomName: String) : ConvertedName {
    override val asString: String = "${domainName.asString}\$${axiomName}"
}

fun FirValueParameterSymbol.convertName(): LocalName = LocalName(name)
fun CallableId.convertName(): ConvertedName = if (isLocal) LocalName(callableName) else GlobalName(packageName, callableName)