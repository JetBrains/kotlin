/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.errortolerance.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.backend.FirErroneousCodePlan
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.contains
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

/**
 * Computes which declarations of a module with frontend errors must have their bodies replaced with a `throw`.
 *
 * Every error diagnostic is attributed to the innermost non-local declaration containing it:
 * - an error inside a body (function body, property initializer or delegate, default value, init block) stubs only that body;
 * - an error in the header of a function or a property stubs its body (and accessors);
 * - an error in the header of a class stubs the bodies of all its members;
 * - an error which can't be attributed to any declaration (except for imports) stubs all the declarations of the file.
 *
 * Besides, if the signature of a declaration contains error types (e.g. an unresolved parameter type, or an implicit return type inferred
 * from an erroneous body), its JVM signature mentions a non-existent class, so all the bodies which reference it are stubbed as well.
 */
@OptIn(DirectDeclarationsAccess::class)
class ErroneousCodePlanBuilder(private val files: List<FirFile>, private val errorsByFile: Map<KtSourceFile?, List<KtDiagnostic>>) {
    private val problems = LinkedHashMap<FirDeclaration, MutableList<String>>()
    private val declarationsWithErroneousSignature = LinkedHashMap<FirBasedSymbol<*>, FirDeclaration>()

    fun build(): FirErroneousCodePlan {
        for (file in files) {
            val errors = errorsByFile[file.sourceFile] ?: continue
            for (error in errors) {
                processError(file, error)
            }
        }
        for (file in files) {
            file.declarations.forEach(::collectDeclarationsWithErroneousSignature)
        }
        if (declarationsWithErroneousSignature.isNotEmpty()) {
            stubUsagesOfDeclarationsWithErroneousSignature()
        }
        return Plan(problems.mapValues { renderStubMessage(it.value) })
    }

    private fun processError(file: FirFile, error: KtDiagnostic) {
        if (error !is KtDiagnosticWithSource) return
        val offset = error.element.startOffset
        val problem = error.renderProblem(file)
        for (declaration in file.declarations) {
            if (locate(declaration, offset, problem)) return
        }
        if (file.imports.any { it.source.containsOffset(offset) }) {
            // Usages of unresolved imports are reported separately
            return
        }
        for (declaration in file.declarations) {
            stubDeclarationWithAllMembers(declaration, problem)
        }
    }

    /**
     * Returns `true` if the error at [offset] belongs to [declaration], and stubs the corresponding parts of it.
     */
    private fun locate(declaration: FirDeclaration, offset: Int, problem: String): Boolean {
        // Synthetic declarations (e.g. implicit primary constructors or properties from constructor parameters) might share the source
        // with real ones, so errors are attributed to the latter
        val source = declaration.source
        if (source?.kind is KtFakeSourceElementKind || !source.containsOffset(offset)) return false
        when (declaration) {
            is FirClass -> {
                if (declaration.declarations.any { locate(it, offset, problem) }) return true
                stubDeclarationWithAllMembers(declaration, problem)
            }
            is FirFunction -> {
                for (parameter in declaration.valueParameters) {
                    if (parameter.defaultValue?.source.containsOffset(offset)) {
                        stub(parameter, problem)
                        return true
                    }
                }
                stub(declaration, problem)
            }
            is FirProperty -> {
                for (accessor in listOfNotNull(declaration.getter, declaration.setter)) {
                    if (locate(accessor, offset, problem)) return true
                }
                if (declaration.initializer?.source.containsOffset(offset) || declaration.delegate?.source.containsOffset(offset)) {
                    stub(declaration, problem)
                } else {
                    stubDeclarationWithAllMembers(declaration, problem)
                }
            }
            is FirAnonymousInitializer -> stub(declaration, problem)
            // Errors in type aliases and in enum entry arguments are left for the IR-based fallback, which replaces error expressions
            else -> {}
        }
        return true
    }

    private fun stubDeclarationWithAllMembers(declaration: FirDeclaration, problem: String) {
        when (declaration) {
            is FirClass -> declaration.declarations.forEach { member ->
                if (member !is FirClass) stubDeclarationWithAllMembers(member, problem)
            }
            is FirProperty -> {
                stub(declaration, problem)
                declaration.getter?.let { stub(it, problem) }
                declaration.setter?.let { stub(it, problem) }
            }
            is FirFunction, is FirAnonymousInitializer -> stub(declaration, problem)
            else -> {}
        }
    }

    private fun stub(declaration: FirDeclaration, problem: String) {
        val messages = problems.getOrPut(declaration) { mutableListOf() }
        if (problem !in messages) messages += problem
    }

    private fun collectDeclarationsWithErroneousSignature(declaration: FirDeclaration) {
        when (declaration) {
            is FirClass -> declaration.declarations.forEach(::collectDeclarationsWithErroneousSignature)
            is FirCallableDeclaration -> if (declaration.hasErrorTypeInSignature()) {
                declarationsWithErroneousSignature[declaration.symbol] = declaration
                // The body of such a declaration might reference its own parameters of non-existent types
                if (declaration !in problems) {
                    stubDeclarationWithAllMembers(declaration, "Declaration has unresolved types in its signature")
                }
            }
            else -> {}
        }
    }

    private fun FirCallableDeclaration.hasErrorTypeInSignature(): Boolean {
        val typeRefs = buildList<FirTypeRef> {
            add(returnTypeRef)
            receiverParameter?.let { add(it.typeRef) }
            contextParameters.forEach { add(it.returnTypeRef) }
            if (this@hasErrorTypeInSignature is FirFunction) {
                valueParameters.forEach { add(it.returnTypeRef) }
            }
        }
        return typeRefs.any { typeRef -> typeRef.coneTypeOrNull?.contains { it is ConeErrorType } == true }
    }

    private fun stubUsagesOfDeclarationsWithErroneousSignature() {
        for (file in files) {
            for (declaration in file.declarations) {
                visitBodies(declaration)
            }
        }
    }

    /**
     * Visits all "bodies" of non-local declarations which aren't stubbed yet and stubs the ones which reference a declaration with
     * an erroneous signature.
     */
    private fun visitBodies(declaration: FirDeclaration) {
        when (declaration) {
            is FirClass -> declaration.declarations.forEach(::visitBodies)
            is FirFunction -> {
                declaration.valueParameters.forEach { parameter -> parameter.defaultValue?.let { checkBody(parameter, it) } }
                declaration.body?.let { checkBody(declaration, it) }
            }
            is FirProperty -> {
                declaration.initializer?.let { checkBody(declaration, it) }
                declaration.delegate?.let { checkBody(declaration, it) }
                declaration.getter?.let(::visitBodies)
                declaration.setter?.let(::visitBodies)
            }
            is FirAnonymousInitializer -> declaration.body?.let { checkBody(declaration, it) }
            else -> {}
        }
    }

    private fun checkBody(owner: FirDeclaration, body: FirElement) {
        if (owner in problems) return
        val finder = ErroneousReferenceFinder()
        body.accept(finder)
        val referenced = finder.found ?: return
        val referencedProblems = problems[referenced].orEmpty()
        val name = (referenced.symbol as? FirCallableSymbol<*>)?.name?.asString() ?: "<unknown>"
        stub(owner, "Usage of '$name' which has compilation errors in its declaration")
        referencedProblems.forEach { stub(owner, it) }
    }

    private inner class ErroneousReferenceFinder : FirVisitorVoid() {
        var found: FirDeclaration? = null

        override fun visitElement(element: FirElement) {
            if (found != null) return
            element.acceptChildren(this)
        }

        override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
            val symbol = resolvedNamedReference.resolvedSymbol
            val unwrapped = (symbol as? FirCallableSymbol<*>)?.unwrapFakeOverrides() ?: symbol
            declarationsWithErroneousSignature[unwrapped]?.let { found = it }
        }
    }

    private fun KtDiagnosticWithSource.renderProblem(file: FirFile): String {
        val lineAndColumn = file.sourceFileLinesMapping?.getLineAndColumnByOffset(element.startOffset)
        val position = if (lineAndColumn != null && lineAndColumn.first >= 0) {
            ":${lineAndColumn.first + 1}:${lineAndColumn.second + 1}"
        } else ""
        return "${renderMessage()} (${file.name}$position)"
    }

    private fun KtSourceElement?.containsOffset(offset: Int): Boolean =
        this != null && offset >= startOffset && offset < endOffset

    private fun renderStubMessage(problems: List<String>): String =
        problems.joinToString(
            separator = "\n\t",
            prefix = if (problems.size == 1) "Unresolved compilation problem:\n\t" else "Unresolved compilation problems:\n\t"
        )

    private class Plan(private val messages: Map<FirDeclaration, String>) : FirErroneousCodePlan {
        override fun getStubMessage(declaration: FirDeclaration): String? = messages[declaration]
    }
}
