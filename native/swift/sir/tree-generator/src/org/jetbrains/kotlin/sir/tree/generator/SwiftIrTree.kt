/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.jetbrains.kotlin.sir.tree.generator

import org.jetbrains.kotlin.generators.tree.StandardTypes.boolean
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

        +field("isStatic", boolean) // todo: KT-65046 Method|function distinction in SIR
        +field("name", string)
        +listField("parameters", parameterType)
        +field("returnType", typeType)
        +field("body", functionBodyType, nullable = true, mutable = true)

        +field(name = "documentation", string, nullable = true, mutable = true)
    }

    val accessor by sealedElement {
        customParentInVisitor = callable
        parent(callable)

        +field("body", functionBodyType, nullable = true, mutable = true)
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

        +field("isStatic", boolean) // todo: KT-65046 Method|function distinction in SIR
    }

    val import by element {
        customParentInVisitor = declaration
        parent(declaration)

        +field("moduleName", string)
    }
}
