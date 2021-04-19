/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelPropertyFqnNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelTypeAliasFqNameIndex
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.ImportPath

class JKResolver(val project: Project, module: Module?, private val contextElement: PsiElement) {
    private val scope = module?.let {
        GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(it)
    } ?: GlobalSearchScope.allScope(project)

    fun resolveDeclaration(fqName: FqName): PsiElement? =
        resolveFqNameOfKtClassByIndex(fqName)
            ?: resolveFqNameOfJavaClassByIndex(fqName)
            ?: resolveFqNameOfKtFunctionByIndex(fqName)
            ?: resolveFqNameOfKtPropertyByIndex(fqName)
            ?: resolveFqName(fqName)

    fun resolveClass(fqName: FqName): PsiElement? =
        resolveFqNameOfKtClassByIndex(fqName)
            ?: resolveFqNameOfJavaClassByIndex(fqName)
            ?: resolveFqName(fqName)

    fun resolveMethod(fqName: FqName): PsiElement? =
        resolveFqNameOfKtFunctionByIndex(fqName)
            ?: resolveFqName(fqName)

    fun resolveField(fqName: FqName): PsiElement? =
        resolveFqNameOfKtPropertyByIndex(fqName)
            ?: resolveFqName(fqName)

    private fun resolveFqNameOfKtClassByIndex(fqName: FqName): KtDeclaration? {
        val fqNameString = fqName.asString()
        val classesPsi = KotlinFullClassNameIndex.getInstance()[fqNameString, project, scope]
        val typeAliasesPsi = KotlinTopLevelTypeAliasFqNameIndex.getInstance()[fqNameString, project, scope]

        return selectNearest(classesPsi, typeAliasesPsi)
    }

    private fun resolveFqNameOfJavaClassByIndex(fqName: FqName): PsiClass? {
        val fqNameString = fqName.asString()
        return JavaFullClassNameIndex.getInstance()[fqNameString.hashCode(), project, scope]
            .firstOrNull {
                it.qualifiedName == fqNameString
            }
    }


    private fun resolveFqNameOfKtFunctionByIndex(fqName: FqName): KtNamedFunction? =
        KotlinTopLevelFunctionFqnNameIndex.getInstance()[fqName.asString(), project, scope].firstOrNull()

    private fun resolveFqNameOfKtPropertyByIndex(fqName: FqName): KtProperty? =
        KotlinTopLevelPropertyFqnNameIndex.getInstance()[fqName.asString(), project, scope].firstOrNull()


    private fun resolveFqName(fqName: FqName): PsiElement? {
        if (fqName.isRoot) return null
        return constructImportDirectiveWithContext(fqName)
            .getChildOfType<KtDotQualifiedExpression>()
            ?.selectorExpression
            ?.references
            ?.firstNotNullOfOrNull(PsiReference::resolve)
    }


    private fun constructImportDirectiveWithContext(fqName: FqName): KtImportDirective {
        val importDirective = KtPsiFactory(contextElement).createImportDirective(ImportPath(fqName, false))
        importDirective.containingKtFile.analysisContext = contextElement.containingFile
        return importDirective
    }

    private fun selectNearest(classesPsi: Collection<KtDeclaration>, typeAliasesPsi: Collection<KtTypeAlias>): KtDeclaration? =
        when {
            typeAliasesPsi.isEmpty() -> classesPsi.firstOrNull()
            classesPsi.isEmpty() -> typeAliasesPsi.firstOrNull()
            else -> (classesPsi.asSequence() + typeAliasesPsi.asSequence()).minWithOrNull(Comparator { o1, o2 ->
                scope.compare(o1.containingFile.virtualFile, o2.containingFile.virtualFile)
            })
        }

    fun multiResolveFqName(fqName: FqName): List<PsiElement> {
        return constructImportDirectiveWithContext(fqName)
            .getChildOfType<KtDotQualifiedExpression>()
            ?.selectorExpression
            ?.let { selector ->
                selector.references.filterIsInstance<PsiPolyVariantReference>()
                    .flatMap { polyVariantReference ->
                        polyVariantReference
                            .multiResolve(false)
                            .mapNotNull { it.element }
                    }
            }.orEmpty()
    }
}
