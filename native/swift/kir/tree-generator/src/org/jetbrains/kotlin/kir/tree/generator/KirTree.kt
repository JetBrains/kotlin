/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.jetbrains.kotlin.kir.tree.generator

import org.jetbrains.kotlin.generators.tree.ImplementationKind
import org.jetbrains.kotlin.generators.tree.config.element
import org.jetbrains.kotlin.generators.tree.config.sealedElement
import org.jetbrains.kotlin.kir.tree.generator.config.AbstractKirTreeBuilder

object KirTree : AbstractKirTreeBuilder() {

    override val rootElement by sealedElement(name = "Element") {
        kDoc = "The root interface of the Kotlin IR tree."
        kind = ImplementationKind.Interface
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
    }

    val declaration by sealedElement {
        customParentInVisitor = rootElement
        +field("origin", originType)
        +field("visibility", kotlinVisibilityType)
        +field(name = "documentation", string, nullable = true, mutable = false)
        +field("parent", declarationParent, mutable = true, isChild = false) {
            useInBaseTransformerDetection = false
        }
    }

    val classMemberDeclaration by sealedElement {
        customParentInVisitor = declaration
        parent(declaration)

        +field("isOverride", boolean)
        +field("isInstance", boolean)
        +field("modality", modalityKind)
    }

    val scopeDefiningElement by sealedElement {
        kDoc = "This interface describes elements (module or declaration) that have a name, define a scope and can appear as part of a Swift FQ name"

        +field("name", string)
    }

    val scopeDefiningDeclaration by sealedElement {
        kDoc = "This interface describes declarations that have a name, define a scope and can appear as part of a Swift FQ name"

        customParentInVisitor = declaration
        parent(declaration)
        parent(scopeDefiningElement)

        visitorParameterName = "declaration"
    }

    val `class` by element {
        customParentInVisitor = scopeDefiningDeclaration
        parent(scopeDefiningDeclaration)
        parent(declarationContainer)
        parent(bridged)

        +field("modality", modalityKind)
    }

    val bridged by sealedElement {
        customParentInVisitor = rootElement

        +listField("bridges", bridgeType)
    }

    val callable by sealedElement {
        parent(declaration)
        parent(bridged)

        +field("body", functionBodyType, nullable = true, mutable = true)
    }

    val function by element {
        customParentInVisitor = callable
        parent(callable)
        parent(classMemberDeclaration)

        +field("name", string)
    }

    val constructor by element {
        customParentInVisitor = callable
        parent(callable)

        +field("delegationCallArguments", functionBodyType)
    }
}
