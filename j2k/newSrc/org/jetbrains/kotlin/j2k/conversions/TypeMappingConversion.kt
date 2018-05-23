/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.JKClassType
import org.jetbrains.kotlin.j2k.tree.JKJavaPrimitiveType
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.JKType
import org.jetbrains.kotlin.j2k.tree.impl.JKClassSymbol
import org.jetbrains.kotlin.j2k.tree.impl.JKClassTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.analysisContext
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

class TypeMappingConversion(val context: ConversionContext) : MatchBasedConversion() {
    override fun onElementChanged(new: JKTreeElement, old: JKTreeElement) {
        somethingChanged = true
    }

    override fun runConversion(treeRoot: JKTreeElement, context: ConversionContext): Boolean {
        val root = applyToElement(treeRoot)
        assert(root === treeRoot)
        return somethingChanged
    }

    var somethingChanged = false


    fun applyToElement(element: JKTreeElement): JKTreeElement {
        return when (element) {
            is JKJavaPrimitiveType -> mapPrimitiveType(element)
            is JKClassType -> mapClassType(element)
            else -> applyRecursive(element, this::applyToElement)
        }
    }

    private fun resolveFqName(classId: ClassId, element: PsiElement): PsiElement? {
        val importDirective = KtPsiFactory(element).createImportDirective(ImportPath(classId.asSingleFqName(), false))
        importDirective.containingKtFile.analysisContext = element.containingFile
        return importDirective.getChildOfType<KtDotQualifiedExpression>()
            ?.selectorExpression
            ?.let {
                it.references.mapNotNull { it.resolve() }.first()
            }
    }

    private fun classTypeByFqName(
        contextElement: PsiElement?,
        fqName: ClassId,
        parameters: List<JKType>,
        nullability: Nullability = Nullability.Default
    ): JKType? {
        contextElement ?: return null
        val newTarget = resolveFqName(fqName, contextElement) as? KtClassOrObject ?: return null


        return JKClassTypeImpl(context.symbolProvider.provideSymbol(newTarget) as JKClassSymbol, parameters, nullability)
    }

    fun mapClassType(type: JKClassType): JKType {
        val fqNameStr = (type.classReference as? JKClassSymbol)?.fqName ?: return type

        val newFqName = JavaToKotlinClassMap.mapJavaToKotlin(FqName(fqNameStr)) ?: return type

        return classTypeByFqName(context.backAnnotator(type), newFqName, type.parameters, type.nullability) ?: type
    }

    fun mapPrimitiveType(type: JKJavaPrimitiveType): JKType {
        val fqName = JvmPrimitiveType.get(type.name).primitiveType.typeFqName

        return classTypeByFqName(context.backAnnotator(type), ClassId.topLevel(fqName), emptyList()) ?: type
    }
}