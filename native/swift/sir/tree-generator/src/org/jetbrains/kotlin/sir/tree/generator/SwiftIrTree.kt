/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.jetbrains.kotlin.sir.tree.generator

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
        +listField("attributes", attributeType)
    }

    val classMemberDeclaration by sealedElement {
        customParentInVisitor = declaration
        parent(declaration)

        +field("isOverride", boolean)
        +field("isInstance", boolean)
        +field("modality", modalityKind)
    }

    val constrainedDeclaration by sealedElement {
        +listField("constraints", typeConstraintType)
    }

    val extension: Element by element {
        customParentInVisitor = declaration
        parent(declaration)
        parent(mutableDeclarationContainer)
        parent(constrainedDeclaration)

        +field("extendedType", typeType)
        +listField("protocols", protocol)
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

    val protocol: Element by element {
        customParentInVisitor = namedDeclaration
        parent(namedDeclaration)
        parent(declarationContainer)

        +field("superClass", typeType, nullable = true)
        +listField("protocols", protocol)
    }

    val `class`: Element by element {
        customParentInVisitor = namedDeclaration
        parent(namedDeclaration)
        parent(declarationContainer)

        +field("superClass", typeType, nullable = true)
        +listField("protocols", protocol)
        +field("modality", modalityKind)
    }

    val `typealias`: Element by element {
        customParentInVisitor = namedDeclaration
        parent(namedDeclaration)

        +field("type", typeType)
    }

    val callable by sealedElement {
        parent(declaration)

        +field("body", functionBodyType, nullable = true, mutable = true)

        +field("errorType", typeType)
    }

    val init by element {
        customParentInVisitor = callable
        parent(callable)

        +field("isFailable", boolean)
        +listField("parameters", parameterType)

        +field("isConvenience", boolean)
        +field("isRequired", boolean)

        +field("isOverride", boolean)
    }

    val function by element {
        customParentInVisitor = callable
        parent(callable)
        parent(classMemberDeclaration)

        +field("name", string)
        +field("extensionReceiverParameter", parameterType, nullable = true)
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
        parent(classMemberDeclaration)

        +field("name", string)
        +field("type", typeType)

        +field("getter", getter)
        +field("setter", setter, nullable = true)
    }
}
