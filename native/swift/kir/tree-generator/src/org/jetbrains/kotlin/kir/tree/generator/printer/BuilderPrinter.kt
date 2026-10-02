/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.tree.generator.printer

import org.jetbrains.kotlin.generators.tree.AbstractBuilderPrinter
import org.jetbrains.kotlin.generators.tree.PrintableAnnotation
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.kir.tree.generator.model.Element
import org.jetbrains.kotlin.kir.tree.generator.model.Field
import org.jetbrains.kotlin.kir.tree.generator.model.Implementation
import org.jetbrains.kotlin.kir.tree.generator.kirBuilderDslAnnotation
import org.jetbrains.kotlin.kir.tree.generator.kirImplementationDetailAnnotation

internal class BuilderPrinter(printer: ImportCollectingPrinter) : AbstractBuilderPrinter<Element, Implementation, Field>(printer) {

    override val implementationDetailAnnotation: PrintableAnnotation
        get() = kirImplementationDetailAnnotation

    override val builderDslAnnotation: PrintableAnnotation
        get() = kirBuilderDslAnnotation
}
