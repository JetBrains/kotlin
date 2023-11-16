/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator

import org.jetbrains.kotlin.generators.tree.InterfaceAndAbstractClassConfigurator
import org.jetbrains.kotlin.generators.tree.addPureAbstractElement
import org.jetbrains.kotlin.generators.tree.detectBaseTransformerTypes
import org.jetbrains.kotlin.generators.tree.printer.printGeneratedType
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.sir.tree.generator.model.Element
import org.jetbrains.kotlin.sir.tree.generator.printer.*
import java.io.File

const val BASE_PACKAGE = "org.jetbrains.kotlin.sir"

typealias Model = org.jetbrains.kotlin.generators.tree.Model<Element>

internal const val TREE_GENERATOR_README = "native/swift/sir/tree-generator/Readme.md"

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("../../tree/gen").canonicalFile

    val model = SwiftIrTree.build()

    detectBaseTransformerTypes(model)
    ImplementationConfigurator.configureImplementations(model)

    val allImplementations = model.elements.flatMap { it.allImplementations }

    InterfaceAndAbstractClassConfigurator(model.elements + allImplementations)
        .configureInterfacesAndAbstractClasses()

    addPureAbstractElement(model.elements, pureAbstractElementType)

    BuilderConfigurator(model.elements).configure()

    val previouslyGeneratedFiles = GeneratorsFileUtil.collectPreviouslyGeneratedFiles(generationPath)
    val generatedFiles = buildList {
        model.elements.mapTo(this) { element ->
            printGeneratedType(generationPath, TREE_GENERATOR_README, element.packageName, element.typeName) {
                ElementPrinter(this).printElement(element)
            }
        }

        allImplementations.mapTo(this) { implementation ->
            printGeneratedType(generationPath, TREE_GENERATOR_README, implementation.packageName, implementation.typeName) {
                ImplementationPrinter(this).printImplementation(implementation)
            }
        }

        allImplementations.mapNotNullTo(this) { implementation ->
            implementation.builder?.let { builder ->
                printGeneratedType(
                    generationPath,
                    TREE_GENERATOR_README,
                    builder.packageName,
                    builder.typeName,
                    fileSuppressions = listOf("DuplicatedCode", "unused"),
                ) {
                    BuilderPrinter(this).printBuilder(builder)
                }
            }
        }

        add(
            printGeneratedType(generationPath, TREE_GENERATOR_README, elementVisitorType.packageName, elementVisitorType.typeName) {
                VisitorPrinter(this, elementVisitorType).printVisitor(model.elements)
            }
        )

        add(
            printGeneratedType(generationPath, TREE_GENERATOR_README, elementTransformerType.packageName, elementTransformerType.typeName) {
                TransformerPrinter(this, elementTransformerType, model.rootElement).printVisitor(model.elements)
            }
        )
    }
    generatedFiles.forEach { GeneratorsFileUtil.writeFileIfContentChanged(it.file, it.newText, logNotChanged = false) }
    GeneratorsFileUtil.removeExtraFilesFromPreviousGeneration(previouslyGeneratedFiles, generatedFiles.map { it.file })
}

