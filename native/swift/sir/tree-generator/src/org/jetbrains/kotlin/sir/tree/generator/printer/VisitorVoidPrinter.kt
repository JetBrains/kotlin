/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.printer

import org.jetbrains.kotlin.generators.tree.AbstractVisitorVoidPrinter
import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.PositionTypeParameterRef
import org.jetbrains.kotlin.sir.tree.generator.elementVisitorType
import org.jetbrains.kotlin.sir.tree.generator.model.Element
import org.jetbrains.kotlin.sir.tree.generator.model.Field
import org.jetbrains.kotlin.utils.SmartPrinter

internal class VisitorVoidPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorVoidPrinter<Element, Field>(printer) {

    override val visitorSuperClass: ClassRef<PositionTypeParameterRef>
        get() = elementVisitorType

    override val useAbstractMethodForRootElement: Boolean
        get() = false

    override val overriddenVisitMethodsAreFinal: Boolean
        get() = true
}
