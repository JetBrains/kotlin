/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.tree.generator

import org.jetbrains.kotlin.generators.tree.InterfaceAndAbstractClassConfigurator
import org.jetbrains.kotlin.generators.tree.detectBaseTransformerTypes
import org.jetbrains.kotlin.generators.tree.printer.TreeGenerator
import org.jetbrains.kotlin.kir.tree.generator.model.Element
import org.jetbrains.kotlin.kir.tree.generator.printer.BuilderPrinter
import org.jetbrains.kotlin.kir.tree.generator.printer.ElementPrinter
import org.jetbrains.kotlin.kir.tree.generator.printer.ImplementationPrinter
import java.io.File

internal const val BASE_PACKAGE = "org.jetbrains.kotlin.kir"

typealias Model = org.jetbrains.kotlin.generators.tree.Model<Element>

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("./native/swift/kir/gen/").canonicalFile

    val model = KirTree.build()
    TreeGenerator(generationPath, "native/swift/kir/tree-generator/Readme.md").run {
        model.inheritFields()
        detectBaseTransformerTypes(model)

        ImplementationConfigurator.configureImplementations(model)
        val implementations = model.elements.flatMap { it.implementations }
        InterfaceAndAbstractClassConfigurator((model.elements + implementations))
            .configureInterfacesAndAbstractClasses()
        model.addPureAbstractElement(pureAbstractElementType)

        val builderConfigurator = BuilderConfigurator(model)
        builderConfigurator.configureBuilders()

        printElements(model, ::ElementPrinter)
        printElementImplementations(implementations, ::ImplementationPrinter)
        printElementBuilders(implementations.mapNotNull { it.builder } + builderConfigurator.intermediateBuilders, :: BuilderPrinter)
    }
}

