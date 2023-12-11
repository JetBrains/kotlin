/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.jetbrains.kotlin.sir.tree.generator

import org.jetbrains.kotlin.generators.tree.StandardTypes.string
import org.jetbrains.kotlin.sir.tree.generator.config.AbstractSwiftIrTreeBuilder
import org.jetbrains.kotlin.sir.tree.generator.model.Element

object SwiftIrTree : AbstractSwiftIrTreeBuilder() {

    override val rootElement by sealedElement(name = "Element") {
        kDoc = "The root interface of the Swift IR tree."
    }

    val module by element {
        customParentInVisitor = rootElement
        parent(declarationContainer)
        parent(named)
    }

    val declarationParent by sealedElement()

    val declarationContainer by sealedElement {
        parent(declarationParent)
        customParentInVisitor = rootElement

        +listField("declarations", declaration)
    }

    val declaration by sealedElement {
        customParentInVisitor = rootElement
        +field("origin", originType)
        +field("visibility", swiftVisibilityType)
        +field("parent", declarationParent, mutable = true, isChild = false) {
            useInBaseTransformerDetection = false
        }
    }

    val foreignDeclaration by sealedElement {
        parent(declaration)

        visitorParameterName = "declaration"
    }

    val named by sealedElement {
        +field("name", string)
    }

    val namedDeclaration by sealedElement {
        customParentInVisitor = declaration
        parent(declaration)
        parent(named)

        visitorParameterName = "declaration"
    }

    val enum: Element by element {
        customParentInVisitor = namedDeclaration
        parent(namedDeclaration)
        parent(declarationContainer)

        +listField("cases", enumCaseType)
    }

    val struct: Element by element {
        customParentInVisitor = namedDeclaration
        parent(namedDeclaration)
        parent(declarationContainer)
    }

    val callable by sealedElement {
        parent(declaration)
    }

    val function by element {
        customParentInVisitor = callable
        parent(callable)

        +field("name", string)
        +listField("parameters", parameterType)
        +field("returnType", typeType)
    }

    val foreignFunction by element {
        customParentInVisitor = callable
        parent(callable)
        parent(foreignDeclaration)

        visitorParameterName = "function"
    }
}