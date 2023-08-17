/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.scala.MangledName

/** Utility class to simplify writing domain functions and axioms.
 *
 * This is like a `VariableEmbedding` but already at the Viper level, making expressions
 * that involve variables less cumbersome to write.
 *
 * Note that we do not mangle the name here: it is assumed that these variables are only
 * used in very controlled scopes.
 */
data class Var(val name: String, val type: Type) {
    val mangledName = object : MangledName {
        override val mangled: String = name
    }

    fun use(): Exp.LocalVar = Exp.LocalVar(mangledName, type)
    fun decl(): Declaration.LocalVarDecl = Declaration.LocalVarDecl(mangledName, type)
}
