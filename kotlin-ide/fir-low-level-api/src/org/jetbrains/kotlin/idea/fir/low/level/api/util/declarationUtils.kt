/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.util.classIdIfNonLocal
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal fun KtDeclaration.findNonLocalFirDeclaration(
    firFileBuilder: FirFileBuilder,
    provider: FirProvider,
    moduleFileCache: ModuleFileCache
): FirDeclaration {
    require(!KtPsiUtil.isLocal(this))
    return when {
        this is KtClassOrObject -> findFir(provider)
        this is KtNamedDeclaration && (this is KtProperty || this is KtNamedFunction) -> {
            val containerClass = containingClassOrObject
            val declarations = if (containerClass != null) {
                val containerClassFir = containerClass.findFir(provider)
                containerClassFir.declarations
            } else {
                val ktFile = containingKtFile
                val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile, moduleFileCache)
                firFile.declarations
            }
            val original = originalDeclaration
            declarations.first { it.psi === this || it.psi == original }
        }
        this is KtConstructor<*> -> {
            val containerClassFir = containingClassOrObject?.findFir(provider)
                ?: error("Container class should be not null for KtConstructor")
            containerClassFir.declarations.first { it.psi === this }
        }
        this is KtTypeAlias -> findFir(provider)
        else -> error("Invalid container $this::class")
    }
}

val ORIGINAL_DECLARATION_KEY = com.intellij.openapi.util.Key<KtDeclaration>("ORIGINAL_DECLARATION_KEY")

var KtDeclaration.originalDeclaration by UserDataProperty(ORIGINAL_DECLARATION_KEY)


private fun KtClassOrObject.findFir(provider: FirProvider): FirRegularClass {
    val classId = classIdIfNonLocal()
        ?: error("Container classId should not be null for non-local declaration")
    return executeWithoutPCE {
        provider.getFirClassifierByFqName(classId) as? FirRegularClass
            ?: error("Could not find class $classId")
    }
}

private fun KtTypeAlias.findFir(provider: FirProvider): FirTypeAlias {
    val typeAlias = ClassId(containingKtFile.packageFqName, nameAsSafeName)
    return executeWithoutPCE {
        provider.getFirClassifierByFqName(typeAlias) as? FirTypeAlias
            ?: error("Could not find type alias $typeAlias")
    }
}