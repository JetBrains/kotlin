/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.passes.utility

import org.jetbrains.kotlin.sir.*
import org.jetbrains.sir.passes.SirPass
import org.jetbrains.sir.passes.utility.ValidationError.WrongParent
import kotlin.collections.plusAssign

/**
 * Collection of errors that might arise during SIR validation.
 */
public sealed interface ValidationError {
    public class WrongParent(public val declaration: SirDeclaration, public val expectedParent: SirDeclarationParent) : ValidationError
}

public class SirValidatorConfig(
    public val checkParents: Boolean = true,
)

/**
 * Validate various invariants about Swift IR.
 */
public fun validate(sirElement: SirElement, config: SirValidatorConfig): List<ValidationError> {
    return SirValidator(config).run(sirElement, null)
}

/**
 * Run full set of SIR validators, and fail if any error encountered.
 */
public fun SirElement.assertValid() {
    val errors = validate(this, SirValidatorConfig())
    if (errors.isNotEmpty()) {
        val messages = errors.map {
            when (it) {
                // TODO: better rendering of SIR elements.
                is WrongParent -> "Wrong declaration parent of ${it.declaration}. Expected: ${it.expectedParent}. Got: ${it.declaration.parent}"
            }
        }
        error("SIR is invalid:\n" + messages.joinToString(separator = "\n"))
    }
}

private fun interface ErrorHandler {
    fun handle(error: ValidationError)
}

internal class SirValidator(private val config: SirValidatorConfig) : SirPass<SirElement, Nothing?, List<ValidationError>> {
    override fun run(element: SirElement, data: Nothing?): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        val handler = ErrorHandler {
            errors += it
        }
        if (config.checkParents) {
            element.accept(ParentValidator(handler), data)
        }
        return errors
    }
}

private class ParentValidator(private val errorHandler: ErrorHandler) : DeclarationParentVisitor() {
    override fun handleParent(declaration: SirDeclaration, parent: SirDeclarationParent) {
        if (declaration.parent != parent) {
            errorHandler.handle(WrongParent(declaration, parent))
        }
    }
}
