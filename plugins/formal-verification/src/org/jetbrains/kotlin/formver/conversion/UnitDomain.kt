/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.scala.silicon.ast.Domain
import org.jetbrains.kotlin.formver.scala.silicon.ast.DomainFunc
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp
import org.jetbrains.kotlin.formver.scala.silicon.ast.Type

const val UNIT_DOMAIN_NAME: String = "unit$"
const val UNIT_DOMAIN_ELEMENT: String = "unit\$element"

/** A representation of the unit type as a Viper domain.
 *
 * We would typically expect a unit type to have only one element, but it doesn't seem possible (or at
 * least easy) to ensure this in Viper: even an axiom of the form `x: Unit, y: Unit :: x == y` doesn't
 * seem to suffice (hence why it is not present here).  It isn't quite clear why this is the case, but
 * since we don't generally need to talk about equality of units this should be fine.
 */
object UnitDomain : Domain(
    UNIT_DOMAIN_NAME,
    listOf(
        DomainFunc(UNIT_DOMAIN_ELEMENT, listOf(), Type.Domain(UNIT_DOMAIN_NAME), true)
    ),
    listOf(),
) {
    val element: Exp = getDomainFuncApp(UNIT_DOMAIN_ELEMENT, listOf())
}