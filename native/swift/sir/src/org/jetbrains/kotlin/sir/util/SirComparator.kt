/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.visitors.SirVisitor

class SirComparator(val options: Set<Options> = emptySet()) : SirVisitor<Boolean, SirElement>() {
    enum class Options {
        POSITIONAL,
        COMPARE_ORIGINS,
    }

    fun areEqual(lhs: SirElement, rhs: SirElement): Boolean = lhs.accept(this, rhs)

    override fun visitElement(element: SirElement, data: SirElement): Boolean {
        error("Comparison of ${element::class.simpleName} is unsupported")
    }

    override fun visitModule(module: SirModule, data: SirElement): Boolean {
        return data is SirModule &&
                module.name == data.name &&
                visitDeclarationContainer(module, data)
    }

    override fun visitDeclarationContainer(declarationContainer: SirDeclarationContainer, data: SirElement): Boolean {
        return data is SirDeclarationContainer && areEqual(declarationContainer.declarations, data.declarations)
    }

    override fun visitDeclaration(declaration: SirDeclaration, data: SirElement): Boolean {
        return data is SirDeclaration &&
                areEqual(data.origin, declaration.origin) &&
                data.visibility == declaration.visibility
    }

    override fun visitEnum(enum: SirEnum, data: SirElement): Boolean {
        return data is SirEnum &&
                data.name == enum.name &&
                data.cases == enum.cases &&
                visitDeclaration(enum, data) &&
                visitDeclarationContainer(enum, data)
    }

    override fun visitStruct(struct: SirStruct, data: SirElement): Boolean {
        return data is SirStruct &&
                data.name == struct.name &&
                visitDeclaration(struct, data) &&
                visitDeclarationContainer(struct, data)
    }

    override fun visitFunction(function: SirFunction, data: SirElement): Boolean {
        return data is SirFunction &&
                data.name == function.name &&
                data.parameters == function.parameters &&
                data.returnType == function.returnType &&
                visitDeclaration(function, data)
    }
}

private fun SirComparator.areEqual(lhs: List<SirElement>, rhs: List<SirElement>): Boolean {
    return if (options.contains(SirComparator.Options.POSITIONAL)) {
        lhs.size == rhs.size &&
                lhs.zip(rhs).all { areEqual(it.first, it.second) }
    } else {
        areEqualNonPositionally(lhs, rhs)
    }
}

private fun SirComparator.areEqual(lhs: SirOrigin, rhs: SirOrigin): Boolean {
    return !options.contains(SirComparator.Options.COMPARE_ORIGINS) || lhs == rhs
}

private fun SirComparator.areEqualNonPositionally(lhs: List<SirElement>, rhs: List<SirElement>): Boolean {
    // FIXME: in the worst case scenario, this function is quadratic
    if (lhs.size != rhs.size)
        return false

    val probedIndices = rhs.indices.toMutableSet()

    lloop@ for (i in lhs.indices) {
        for (j in probedIndices) {
            if (areEqual(lhs[i], rhs[j])) {
                probedIndices.remove(j)
                continue@lloop
            }
        }
        return false
    }
    return true
}