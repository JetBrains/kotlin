/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.jetbrains.kotlin.sir.tree.generator

import org.jetbrains.kotlin.generators.tree.StandardTypes.boolean
import org.jetbrains.kotlin.generators.tree.StandardTypes.string
import org.jetbrains.kotlin.generators.tree.config.element
import org.jetbrains.kotlin.generators.tree.config.sealedElement
import org.jetbrains.kotlin.sir.tree.generator.config.AbstractSwiftIrTreeBuilder
import org.jetbrains.kotlin.sir.tree.generator.model.Element

object SwiftIrTree : AbstractSwiftIrTreeBuilder() {

    override val rootElement by sealedElement(name = "Element") {
        kDoc = "The root interface of the Swift IR tree."
    }

    val declarationParent by sealedElement()

    val declarationContainer by sealedElement {
        parent(declarationParent)
        customParentInVisitor = rootElement

        +listField("declarations", declaration)
    }

    val mutableDeclarationContainer by sealedElement {
        parent(declarationParent)
        parent(declarationContainer)
        customParentInVisitor = rootElement

        +listField("declarations", declaration, isMutableList = true)
    }

    val module by element {
        customParentInVisitor = rootElement
        parent(mutableDeclarationContainer)
        parent(named)
        +listField("imports", importType, isMutableList = true)
    }

    val declaration by sealedElement {
        customParentInVisitor = rootElement
        +field("origin", originType)
        +field("visibility", swiftVisibilityType)
        +field(name = "documentation", string, nullable = true, mutable = false)
        +field("parent", declarationParent, mutable = true, isChild = false) {
            useInBaseTransformerDetection = false
        }
    }

    val extension: Element by element {
        customParentInVisitor = declaration
        parent(declaration)
        parent(mutableDeclarationContainer)

        +field("extendedType", typeType)
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
        parent(mutableDeclarationContainer)

        +listField("cases", enumCaseType)
    }

    val struct: Element by element {
        customParentInVisitor = namedDeclaration
        parent(namedDeclaration)
        parent(declarationContainer)
    }

    val `class`: Element by element {
        customParentInVisitor = namedDeclaration
        parent(namedDeclaration)
        parent(declarationContainer)

        +field("superClass", typeType, nullable = true)
    }

    val `typealias`: Element by element {
        customParentInVisitor = namedDeclaration
        parent(namedDeclaration)

        +field("type", typeType)
    }

    val callable by sealedElement {
        parent(declaration)

        +field("kind", callableKind)
        +field("body", functionBodyType, nullable = true, mutable = true)
    }

    val init by element {
        customParentInVisitor = callable
        parent(callable)

        +field("isFailable", boolean)
        +listField("parameters", parameterType)

        +field("initKind", initKind)

        +field("isOverride", boolean)
    }

    val function by element {
        customParentInVisitor = callable
        parent(callable)

        +field("name", string)
        +listField("parameters", parameterType)
        +field("returnType", typeType)
    }

    val accessor by sealedElement {
        customParentInVisitor = callable
        parent(callable)
    }

    val getter by element {
        parent(accessor)
    }

    val setter by element {
        parent(accessor)

        +field("parameterName", string, initializer = { })
    }

    val variable by element {
        customParentInVisitor = declaration
        parent(declaration)
        parent(declarationParent)

        +field("name", string)
        +field("type", typeType)

        +field("getter", getter)
        +field("setter", setter, nullable = true)
    }
}
