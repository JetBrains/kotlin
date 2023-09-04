/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.domains

import org.jetbrains.kotlin.formver.viper.ast.BuiltinDomain
import org.jetbrains.kotlin.formver.viper.ast.DomainAxiom
import org.jetbrains.kotlin.formver.viper.ast.DomainFunc
import org.jetbrains.kotlin.formver.viper.ast.Type

object AnyDomain : BuiltinDomain("Any") {
    override val typeVars: List<Type.TypeVar> = emptyList()
    override val functions: List<DomainFunc> = emptyList()
    override val axioms: List<DomainAxiom> = emptyList()
}