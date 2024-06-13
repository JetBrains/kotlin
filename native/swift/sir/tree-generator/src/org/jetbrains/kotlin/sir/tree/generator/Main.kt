/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator

import org.jetbrains.kotlin.generators.tree.printer.TreeGenerator
import org.jetbrains.kotlin.sir.tree.generator.model.Element
import org.jetbrains.kotlin.sir.tree.generator.printer.*
import java.io.File

internal const val BASE_PACKAGE = "org.jetbrains.kotlin.sir"

typealias Model = org.jetbrains.kotlin.generators.tree.Model<Element>

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("./native/swift/sir/gen/").canonicalFile
    val model = SwiftIrTree.build()
    TreeGenerator(generationPath, "native/swift/sir/tree-generator/Readme.md").run {
        generateTree(
            model,
            pureAbstractElementType,
            ::ElementPrinter,
            emptyList(),
            ImplementationConfigurator,
            BuilderConfigurator(model.elements),
            ::ImplementationPrinter,
            ::BuilderPrinter,
        )
    }
}

