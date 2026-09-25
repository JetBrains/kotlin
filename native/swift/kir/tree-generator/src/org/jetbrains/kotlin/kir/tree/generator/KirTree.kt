/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

    val module by element {
        parent(rootElement)

        +listField("functions", function, isMutableList = true)
    }

    val function by element {
        parent(rootElement)

        +field("name", string)
        +listField("parameters", parameterType)
    }
}
