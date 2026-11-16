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
        parent(rootElement)
        parent(callable)

        +field("name", string)
    }
}
