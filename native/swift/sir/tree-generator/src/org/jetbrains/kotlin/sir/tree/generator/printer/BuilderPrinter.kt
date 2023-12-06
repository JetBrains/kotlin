/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.printer

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.sir.tree.generator.model.Element
import org.jetbrains.kotlin.sir.tree.generator.model.Field
import org.jetbrains.kotlin.sir.tree.generator.model.Implementation
import org.jetbrains.kotlin.sir.tree.generator.model.ListField
import org.jetbrains.kotlin.sir.tree.generator.swiftIrBuilderDslAnnotation
import org.jetbrains.kotlin.sir.tree.generator.swiftIrImplementationDetailAnnotation
import org.jetbrains.kotlin.utils.SmartPrinter

internal class BuilderPrinter(printer: SmartPrinter) : AbstractBuilderPrinter<Element, Implementation, Field, Field>(printer) {

    override val implementationDetailAnnotation: ClassRef<*>
        get() = swiftIrImplementationDetailAnnotation

    override val builderDslAnnotation: ClassRef<*>
        get() = swiftIrBuilderDslAnnotation
}