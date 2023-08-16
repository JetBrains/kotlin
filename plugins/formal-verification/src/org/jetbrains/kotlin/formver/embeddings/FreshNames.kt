/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

/**
 * Representation for names not present in the original source,
 * e.g. storage for the result of subexpressions.
 */
data class AnonymousName(val n: Int) : MangledName {
    override val mangled: String
        get() = "anonymous\$$n"
}

data object ReturnVariableName : MangledName {
    override val mangled: String
        get() = "ret\$"
}

/**
 * We also convert domain names and their function and axiom names as
 * they have to be globally unique as well.
 */
data class DomainName(val name: String) : MangledName {
    // Info: Can't use 'domain' as prefix as Viper recognizes it as a keyword
    override val mangled: String = "dom\$$name"
}

data class DomainFuncName(val domainName: DomainName, val funcName: String) : MangledName {
    override val mangled: String = "${domainName.mangled}\$${funcName}"
}

data class DomainAxiomName(val domainName: DomainName, val axiomName: String) : MangledName {
    override val mangled: String = "${domainName.mangled}\$${axiomName}"
}
