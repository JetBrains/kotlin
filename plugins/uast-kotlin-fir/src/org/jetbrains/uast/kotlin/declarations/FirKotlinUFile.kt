/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.*

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

    override val allCommentsInFile: List<UComment> = comments

    override val imports: List<UImportStatement> by lz {
        sourcePsi.importDirectives.map { FirKotlinUImportStatement(it, this) }
    }

    override val classes: List<UClass>
        get() {
            // TODO: Script
            // TODO: Facade: getOrCreateFirLightFacade()
            return sourcePsi.declarations.mapNotNull { (it as? KtClassOrObject)?.toUClass() }
        }

    private fun PsiElement.toUClass() = languagePlugin.convertOpt<UClass>(this, this@FirKotlinUFile)
}
