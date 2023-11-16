/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.jetbrains.kotlin.sir.tree.generator

import org.jetbrains.kotlin.generators.tree.StandardTypes.string
import org.jetbrains.kotlin.sir.tree.generator.config.AbstractSwiftIrTreeBuilder

object SwiftIrTree : AbstractSwiftIrTreeBuilder() {

    override val rootElement by sealedElement(name = "Element") {
        kDoc = "The root interface of the Swift IR tree."
    }

    val module by element {
        customParentInVisitor = rootElement
        // TODO
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
        +listField("attributes", attributeType)
        +field("visibility", swiftVisibilityType)
        +field("parent", declarationParent, mutable = true) {
            needAcceptAndTransform = false
            useInBaseTransformerDetection = false
        }
    }

    val declarationWithName by sealedElement {
        parent(declaration)

        +field("name", string)
    }

    val namedTypeDeclaration by sealedElement {
        parent(declarationWithName)
    }

    val typeAlias by element {
        parent(namedTypeDeclaration)
    }

    val `class` by element {
        customParentInVisitor = namedTypeDeclaration
        parent(namedTypeDeclaration)
        parent(declarationContainer)
    }

    val actor by element {
        customParentInVisitor = namedTypeDeclaration
        parent(namedTypeDeclaration)
        parent(declarationContainer)
    }

    val struct by element {
        customParentInVisitor = namedTypeDeclaration
        parent(namedTypeDeclaration)
        parent(declarationContainer)
    }

    val enum by element {
        customParentInVisitor = namedTypeDeclaration
        parent(namedTypeDeclaration)
        parent(declarationContainer)

        +listField("cases", enumCase)
    }

    val enumCase by element {
        parent(declarationWithName)
    }

    val protocol by element {
        customParentInVisitor = namedTypeDeclaration
        parent(namedTypeDeclaration)
        parent(declarationContainer)
    }

    val callable by sealedElement {
        parent(declaration)
    }

    val function by element {
        customParentInVisitor = callable
        parent(callable)
        parent(declarationWithName)
    }

    val init by element {
        parent(callable)
    }

    val accessor by sealedElement {
        parent(callable)
    }

    val getter by element {
        parent(accessor)
    }

    val setter by element {
        parent(accessor)
    }

    val accessorContainer by sealedElement {
        customParentInVisitor = rootElement
        +field("getter", getter)
        +field("setter", setter, nullable = true)
    }

    val property by element {
        customParentInVisitor = accessorContainer
        parent(declarationWithName)
        parent(accessorContainer)
    }

    val subscript by element {
        customParentInVisitor = accessorContainer
        parent(declaration)
        parent(accessorContainer)
    }

    val expression by element {
        customParentInVisitor = rootElement
    }
}