/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.IdentifierChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

// Note: IdentifierChecker doesn't check typealiases, so inheriting DeclarationChecker as well.
// Originally based on JvmSimpleNameBacktickChecker.
class NativeIdentifierChecker(private val languageVersionSettings: LanguageVersionSettings) : IdentifierChecker, DeclarationChecker {
    // Also includes characters used by IR mangler (see MangleConstant).
    private val invalidChars = setOf(
        '.', ';', ',', '(', ')', '[', ']', '{', '}', '/', '<', '>',
        ':', '\\', '$', '&', '~', '*', '?', '#', '|', '§', '%', '@',
    )

    override fun checkIdentifier(simpleNameExpression: KtSimpleNameExpression, diagnosticHolder: DiagnosticSink) {
        reportIfNeeded(simpleNameExpression.getReferencedName(), { simpleNameExpression.getIdentifier() }, diagnosticHolder)
    }

    override fun checkDeclaration(declaration: KtDeclaration, diagnosticHolder: DiagnosticSink) {
        if (declaration is KtDestructuringDeclaration) {
            declaration.entries.forEach { checkNamed(it, diagnosticHolder) }
        }
        if (declaration is KtCallableDeclaration) {
            declaration.valueParameters.forEach { checkNamed(it, diagnosticHolder) }
        }
        if (declaration is KtTypeParameterListOwner) {
            declaration.typeParameters.forEach { checkNamed(it, diagnosticHolder) }
        }
        if (declaration is KtNamedDeclaration) {
            checkNamed(declaration, diagnosticHolder)
        }
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration is KtTypeAlias) {
            checkNamed(declaration, context.trace)
        }
    }

    private fun checkNamed(declaration: KtNamedDeclaration, diagnosticHolder: DiagnosticSink) {
        val name = declaration.name ?: return

        reportIfNeeded(name, { declaration.nameIdentifier ?: declaration }, diagnosticHolder)
    }

    private fun reportIfNeeded(name: String, reportOn: () -> PsiElement?, diagnosticHolder: DiagnosticSink) {
        val text = KtPsiUtil.unquoteIdentifier(name)
        when {
            text.isEmpty() -> {
                diagnosticHolder.report(
                    ErrorsNative.INVALID_CHARACTERS_NATIVE.on(
                        languageVersionSettings,
                        reportOn() ?: return,
                        "should not be empty"
                    )
                )
            }
            text.any { it in invalidChars } -> {
                diagnosticHolder.report(
                    ErrorsNative.INVALID_CHARACTERS_NATIVE.on(
                        languageVersionSettings,
                        reportOn() ?: return,
                        "contains illegal characters: " +
                                invalidChars.intersect(text.toSet()).joinToString("", prefix = "\"", postfix = "\"")
                    )
                )
            }
        }
    }
}
