/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.passes.utility

import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.SirElement
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.visitors.SirVisitor
import java.util.*


// Copy of DeclarationParentVisitor from Kotlin Backend IR.
internal abstract class DeclarationParentVisitor : SirVisitor<Unit, Nothing?>() {

    protected val declarationParentStack = ArrayDeque<SirDeclarationParent>()

    protected abstract fun handleParent(declaration: SirDeclaration, parent: SirDeclarationParent)

    override fun visitElement(element: SirElement, data: Nothing?) {
        element.acceptChildren(this, data)
    }

    override fun visitModule(module: SirModule, data: Nothing?) {
        declarationParentStack.push(module)
        super.visitModule(module, data)
        declarationParentStack.pop()
    }

    override fun visitDeclaration(declaration: SirDeclaration, data: Nothing?) {
        // Special case if visitor is started not from the module node.
        // In this case we assume that parent is set properly.
        val currentParent = if (declarationParentStack.size == 0) {
            declaration.parent
        } else {
            declarationParentStack.peekFirst()
        }
        handleParent(declaration, currentParent)

        if (declaration is SirDeclarationParent) {
            declarationParentStack.push(declaration)
        }

        super.visitDeclaration(declaration, data)

        if (declaration is SirDeclarationParent) {
            declarationParentStack.pop()
        }
    }
}

/**
 * Fixes parents of declarations.
 *
 * Copy of PatchDeclarationParentVisitor from Backend IR.
 */
internal class PatchDeclarationParentVisitor() : DeclarationParentVisitor() {
    constructor(containingDeclaration: SirDeclarationParent) : this() {
        declarationParentStack.push(containingDeclaration)
    }

    override fun handleParent(declaration: SirDeclaration, parent: SirDeclarationParent) {
        declaration.parent = parent
    }
}
