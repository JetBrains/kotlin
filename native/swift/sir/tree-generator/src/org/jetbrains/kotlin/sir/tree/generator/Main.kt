/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator

import org.jetbrains.kotlin.generators.tree.InterfaceAndAbstractClassConfigurator
import org.jetbrains.kotlin.generators.tree.addPureAbstractElement
import org.jetbrains.kotlin.generators.tree.detectBaseTransformerTypes
import org.jetbrains.kotlin.generators.tree.printer.TreeGenerator
import org.jetbrains.kotlin.sir.tree.generator.model.Element
import org.jetbrains.kotlin.sir.tree.generator.printer.BuilderPrinter
import org.jetbrains.kotlin.sir.tree.generator.printer.ElementPrinter
import org.jetbrains.kotlin.sir.tree.generator.printer.ImplementationPrinter
import java.io.File

internal const val BASE_PACKAGE = "org.jetbrains.kotlin.sir"

typealias Model = org.jetbrains.kotlin.generators.tree.Model<Element>

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("./native/swift/sir/gen/").canonicalFile

    val model = SwiftIrTree.build()
    TreeGenerator(generationPath, "native/swift/sir/tree-generator/Readme.md").run {
        model.inheritFields()
        detectBaseTransformerTypes(model)

        ImplementationConfigurator.configureImplementations(model)
        val implementations = model.elements.flatMap { it.implementations }
        InterfaceAndAbstractClassConfigurator((model.elements + implementations))
            .configureInterfacesAndAbstractClasses()
        addPureAbstractElement(model.elements, pureAbstractElementType)

        val builderConfigurator = BuilderConfigurator(model)
        builderConfigurator.configureBuilders()

        printElements(model, ::ElementPrinter)
        printElementImplementations(implementations, ::ImplementationPrinter)
        printElementBuilders(implementations.mapNotNull { it.builder } + builderConfigurator.intermediateBuilders, :: BuilderPrinter)
    }
}

