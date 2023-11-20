/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.analysisapi

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirForeignFunction

/**
 * A root interface for classes that produce Swift IR elements.
 */
interface SirFactory {
    fun build(fromFile: KtFile): List<SirDeclaration>
}

class SirGenerator : SirFactory {
    override fun build(fromFile: KtFile): List<SirDeclaration> = analyze(fromFile) {
        val res = mutableListOf<SirForeignFunction>()
        fromFile.accept(Visitor(res))
        return res.toList()
    }

    context(KtAnalysisSession)
    private class Visitor(val res: MutableList<SirForeignFunction>) : KtTreeVisitorVoid() {
        override fun visitNamedFunction(function: KtNamedFunction) {
            super.visitNamedFunction(function)

            val fqName = function
                .takeIf { function.isPublic }
                ?.fqName
                ?.pathSegments()
                ?.toListString()
                ?: return

            val f = function.getFunctionLikeSymbol()

            val params: MutableList<Pair<String, String>> = mutableListOf()
            function.accept(PropVisitor(params))

            val returnType = f.returnType.toString()

            val result = SirForeignFunction(
                fqName = fqName,
                arguments = params,
                returnType = returnType,
            )

            res.add(result)
        }
    }

    context(KtAnalysisSession)
    private class PropVisitor(val result: MutableList<Pair<String, String>>) : KtTreeVisitorVoid() {
        override fun visitParameter(parameter: KtParameter) {
            super.visitParameter(parameter)
            val s = parameter.getParameterSymbol()
            result.add(Pair(s.name.toString(), s.returnType.toString()))
        }
    }
}

private fun List<Name>.toListString() = map { it.asString() }
