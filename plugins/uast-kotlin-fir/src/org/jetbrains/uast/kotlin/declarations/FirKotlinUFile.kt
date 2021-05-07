/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.*
import java.util.ArrayList

class FirKotlinUFile(
    override val psi: KtFile,
    override val languagePlugin: UastLanguagePlugin = firKotlinUastPlugin
) : UFile {
    override val javaPsi: PsiElement? = null

    override val sourcePsi: KtFile = psi

    override val uAnnotations: List<UAnnotation>
        get() {
            // TODO: Not yet implemented
            return emptyList()
        }

    override val packageName: String by lz {
        sourcePsi.packageFqName.asString()
    }

    override val allCommentsInFile by lz {
        val comments = ArrayList<UComment>(0)
        psi.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitComment(comment: PsiComment) {
                comments += UComment(comment, this@FirKotlinUFile)
            }
        })
        comments
    }

    override val imports: List<UImportStatement> by lz {
        sourcePsi.importDirectives.map { FirKotlinUImportStatement(it, this) }
    }

    override val classes: List<UClass> by lz {
        val facadeOrScriptClass = if (sourcePsi.isScript()) sourcePsi.script?.toLightClass() else sourcePsi.findFacadeClass()
        val facadeOrScriptUClass = facadeOrScriptClass?.toUClass()?.let { listOf(it) } ?: emptyList()
        val classes = sourcePsi.declarations.mapNotNull { (it as? KtClassOrObject)?.toUClass() }
        facadeOrScriptUClass + classes
    }

    private fun PsiElement.toUClass() = languagePlugin.convertOpt<UClass>(this, this@FirKotlinUFile)
}
