/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.nullabilityAnalysis

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.asLabel
import org.jetbrains.kotlin.nj2k.parentOfType
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


internal fun TypeVariable.changeNullability(toNullable: Boolean) {
    typeElement?.changeNullability(toNullable)
}

internal fun KtTypeElement.changeNullability(toNullable: Boolean) {
    val factory = KtPsiFactory(this)
    if (this is KtNullableType && !toNullable) {
        replace(factory.createType(innerType?.text ?: return).typeElement ?: return)
    }
    if (this !is KtNullableType && toNullable) {
        replace(factory.createType("$text?").typeElement ?: return)
    }
}


internal fun AnalysisContext.fixTypeVariablesNullability() {
    if (typeElementToTypeVariable.isEmpty()) return

    val deepComparator = Comparator<TypeVariable> { o1, o2 ->
        if (o1.typeElement == null || o2.typeElement == null) return@Comparator -1
        if (o1.typeElement.isAncestor(o2.typeElement)) 1 else -1
    }
    for (typeVariableOwner in typeVariableOwners) {
        for (typeVariable in (typeVariableOwner.innerTypeVariables() + typeVariableOwner.allTypeVariables).sortedWith(deepComparator)) {
            when (typeVariable.nullability) {
                Nullability.NOT_NULL -> typeVariable.changeNullability(toNullable = false)
                Nullability.NULLABLE -> typeVariable.changeNullability(toNullable = true)
                Nullability.UNKNOWN -> {
                    if (typeVariableOwner is FunctionCallTypeArgumentTarget) {
                        typeVariable.changeNullability(toNullable = false)
                    } else {
                        typeVariable.changeNullability(toNullable = true)
                    }
                }
            }
        }
    }
}

internal fun KtTypeElement.classReference(): ClassReference {
    val target = when (this) {
        is KtNullableType -> innerType?.safeAs()
        is KtUserType -> this
        else -> null
    }?.referenceExpression?.resolve()
    return when (target) {
        is KtClassOrObject -> KtClassReference(target)
        is PsiClass -> JavaClassReference(target)
        is KtTypeAlias -> target.getTypeReference()?.typeElement?.classReference()
        is KtTypeParameter -> TypeParameterClassReference(target)
        else -> null
    } ?: UnknownClassReference(text)
}

class NullabilityAnalysisFacade(
    private val conversionContext: NewJ2kConverterContext,
    private val getTypeElementNullability: (KtTypeElement, NewJ2kConverterContext) -> Nullability,
    private val prepareTypeElement: (KtTypeElement, NewJ2kConverterContext) -> Unit,
    private val debugPrint: Boolean
) {
    fun fixNullability(analysisScope: AnalysisScope) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            runWriteAction {
                analysisScope.prepareTypeElements(prepareTypeElement, conversionContext)
                val context = ContextCreator(conversionContext, getTypeElementNullability).createContext(analysisScope)
                if (debugPrint) {
                    with(Printer(context)) {
                        analysisScope.forEach { it.addTypeVariablesNames() }
                    }
                }

                val constraints = ConstraintsCollector(context, conversionContext, debugPrint).collectConstraints(analysisScope)
                Solver(context, debugPrint).solveConstraints(constraints)
                context.fixTypeVariablesNullability()
                analysisScope.clearUndefinedLabels()
            }
        }
    }
}

private fun AnalysisScope.prepareTypeElements(
    prepareTypeElement: (KtTypeElement, NewJ2kConverterContext) -> Unit,
    conversionContext: NewJ2kConverterContext
) {
    val typeElements = flatMap { it.collectDescendantsOfType<KtTypeReference>() }
    typeElements.forEach { typeReference ->
        val typeElement = typeReference.typeElement ?: return@forEach
        if (typeElement.parentOfType<KtSuperTypeCallEntry>() == null) {
            prepareTypeElement(typeElement, conversionContext)
        }
    }
}


private fun AnalysisScope.clearUndefinedLabels() {
    val comments = mutableListOf<PsiComment>()
    forEach { element ->
        element.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }

            override fun visitComment(comment: PsiComment) {
                if (comment.text.asLabel() != null) {
                    comments += comment
                }
            }
        })
    }
    comments.forEach { it.delete() }
}


