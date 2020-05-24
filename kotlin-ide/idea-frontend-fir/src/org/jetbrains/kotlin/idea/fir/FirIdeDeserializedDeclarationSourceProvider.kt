/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelTypeAliasFqNameIndex
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction


//todo introduce LibraryModificationTracker based cache?
object FirIdeDeserializedDeclarationSourceProvider {
    fun findPsi(fir: FirElement, project: Project): PsiElement? {
        return when (fir) {
            is FirSimpleFunction -> provideSourceForFunction(fir, project)
            is FirProperty -> provideSourceForProperty(fir, project)
            is FirClass<*> -> provideSourceForClass(fir, project)
            is FirTypeAlias -> provideSourceForTypeAlias(fir, project)
            is FirConstructor -> provideSourceForConstructor(fir, project)
            else -> null
        }
    }

    private fun provideSourceForFunction(
        function: FirSimpleFunction,
        project: Project
    ): PsiElement? {
        val candidates = if (function.isTopLevel) {
            KotlinTopLevelFunctionFqnNameIndex.getInstance().get(
                function.symbol.callableId.asFqNameForDebugInfo().asString(),
                project,
                function.scope(project)
            ).filter(KtNamedFunction::isCompiled)
        } else {
            function.containingKtClass(project)?.body?.functions
                ?.filter { it.name == function.name.asString() && it.isCompiled() }
                .orEmpty()
        }

        return function.chooseCorrespondingPsi(candidates)
    }

    private fun provideSourceForProperty(property: FirProperty, project: Project): PsiElement? {
        val candidates = if (property.isTopLevel) {
            KotlinTopLevelFunctionFqnNameIndex.getInstance().get(
                property.symbol.callableId.asFqNameForDebugInfo().asString(),
                project,
                property.scope(project)
            )
        } else {
            property.containingKtClass(project)?.declarations
                ?.filter { it.name == property.name.asString() }
                .orEmpty()
        }

        return candidates.firstOrNull(KtElement::isCompiled)
    }

    private fun provideSourceForClass(klass: FirClass<*>, project: Project): PsiElement? =
        classByClassId(klass.symbol.classId, klass.scope(project), project)

    private fun provideSourceForTypeAlias(alias: FirTypeAlias, project: Project): PsiElement? {
        val candidates = KotlinTopLevelTypeAliasFqNameIndex.getInstance().get(
            alias.symbol.classId.asStringForUsingInIndexes(),
            project,
            alias.scope(project)
        )
        return candidates.firstOrNull(KtElement::isCompiled)
    }

    private fun provideSourceForConstructor(
        constructor: FirConstructor,
        project: Project
    ): PsiElement? {
        val containingKtClass = constructor.containingKtClass(project) ?: return null
        if (constructor.isPrimary) return containingKtClass.primaryConstructor

        return constructor.chooseCorrespondingPsi(containingKtClass.secondaryConstructors)
    }

    private fun FirFunction<*>.chooseCorrespondingPsi(
        candidates: Collection<KtFunction>
    ): KtFunction? {
        if (candidates.isEmpty()) return null
        for (candidate in candidates) {
            assert(candidate.isCompiled()) {
                "Candidate should be decompiled from metadata because it should have fqName types as we don't use resolve here"
            }
            if (KtDeclarationAndFirDeclarationEqualityChecker.representsTheSameDeclaration(candidate, this)) {
                return candidate
            }
        }
        return null
    }

    private fun FirDeclaration.scope(project: Project): GlobalSearchScope {
        return GlobalSearchScope.allScope(project)
        /* TODO:
         val session = session as? FirLibrarySession
         return session?.scope ?: GlobalSearchScope.allScope(project)*/
    }

    private fun FirCallableDeclaration<*>.containingKtClass(project: Project): KtClassOrObject? =
        symbol.callableId.classId?.let { classByClassId(it, scope(project), project) }

    private fun classByClassId(classId: ClassId, scope: GlobalSearchScope, project: Project): KtClassOrObject? {
        val fqName = classId.asStringForUsingInIndexes().let { classIdMapping[it] ?: it }
        return KotlinFullClassNameIndex.getInstance().get(
            fqName,
            project,
            scope
        ).firstOrNull(KtElement::isCompiled)
    }

    private val FirCallableDeclaration<*>.isTopLevel
        get() = symbol.callableId.className == null

    private fun ClassId.asStringForUsingInIndexes() = asString().replace('/', '.')

    private val classIdMapping = (0..23).associate { i ->
        "kotlin.Function$i" to "kotlin.jvm.functions.Function$i"
    }
}

private fun KtElement.isCompiled(): Boolean = containingKtFile.isCompiled

fun FirElement.findPsi(project: Project): PsiElement? =
    psi ?: FirIdeDeserializedDeclarationSourceProvider.findPsi(this, project)

fun FirElement.findPsi(session: FirSession): PsiElement? =
    findPsi(session.sessionProvider!!.project)