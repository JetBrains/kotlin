/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.domains

import org.jetbrains.kotlin.formver.viper.ast.*

/** A representation of the unit type as a Viper domain.
 *
 * We would typically expect a unit type to have only one element, but it doesn't seem possible (or at
 * least easy) to ensure this in Viper: even an axiom of the form `x: Unit, y: Unit :: x == y` doesn't
 * seem to suffice (hence why it is not present here).  It isn't quite clear why this is the case, but
 * since we don't generally need to talk about equality of units this should be fine.
 */
object UnitDomain : BuiltinDomain("Unit") {
    override val typeVars: List<Type.TypeVar> = emptyList()

    val elementFunc = createDomainFunc("element", emptyList(), toType())
    val element: Exp = elementFunc()

    override val functions: List<DomainFunc> = listOf(elementFunc)
    override val axioms: List<DomainAxiom> = emptyList()
}