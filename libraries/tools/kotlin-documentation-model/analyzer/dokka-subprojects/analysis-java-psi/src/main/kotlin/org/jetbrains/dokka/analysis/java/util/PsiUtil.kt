/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.model.DocumentableSource
import org.jetbrains.dokka.utilities.firstIsInstanceOrNull

// TODO [beresnev] copy-paste

internal val PsiElement.parentsWithSelf: Sequence<PsiElement>
    get() = generateSequence(this) { if (it is PsiFile) null else it.parent }

@InternalDokkaApi
public fun DRI.Companion.from(psi: PsiElement): DRI = psi.parentsWithSelf.run {
    if (psi is PsiPackage) {
        return@run DRI(
            packageName = psi.qualifiedName,
        )
    }
    val psiMethod = firstIsInstanceOrNull<PsiMethod>()
    val psiField = firstIsInstanceOrNull<PsiField>()
    val classes = filterIsInstance<PsiClass>().filterNot { it is PsiTypeParameter }
        .toList() // We only want exact PsiClass types, not PsiTypeParameter subtype
    val additionalClasses = if (psi is PsiEnumConstant) listOfNotNull(psiField?.name) else emptyList()
    DRI(
        packageName = classes.lastOrNull()?.qualifiedName?.substringBeforeLast('.', "") ?: "",
        classNames = (additionalClasses + classes.mapNotNull { it.name }).takeIf { it.isNotEmpty() }
            ?.asReversed()?.joinToString("."),
        // The fallback strategy test whether psi is not `PsiEnumConstant`. The reason behind this is that
        // we need unified DRI for both Java and Kotlin enums, so we can link them properly and treat them alike.
        // To achieve that, we append enum name to classNames list and leave the callable part set to null. For Kotlin enums
        // it is by default, while for Java enums we have to explicitly test for that in this `takeUnless` condition.
        callable = psiMethod?.let { Callable.from(it) } ?: psiField?.takeUnless { psi is PsiEnumConstant }?.let { Callable.from(it) },
        target = DriTarget.from(psi),
        extra = if (psi is PsiEnumConstant)
            DRIExtraContainer().also { it[EnumEntryDRIExtra] = EnumEntryDRIExtra }.encode()
        else null
    )
}

internal fun Callable.Companion.from(psi: PsiMethod) = with(psi) {
    Callable(
        name,
        null,
        parameterList.parameters.map { param -> JavaClassReference(param.type.canonicalText) })
}

internal fun Callable.Companion.from(psi: PsiField): Callable {
    return Callable(
        name = psi.name,
        receiver = null,
        params = emptyList()
    )
}

internal fun DriTarget.Companion.from(psi: PsiElement): DriTarget = psi.parentsWithSelf.run {
    return when (psi) {
        is PsiTypeParameter -> PointingToGenericParameters(psi.index)
        else -> firstIsInstanceOrNull<PsiParameter>()?.let {
            val callable = firstIsInstanceOrNull<PsiMethod>()
            val params = (callable?.parameterList?.parameters).orEmpty()
            PointingToCallableParameters(params.indexOf(it))
        } ?: PointingToDeclaration
    }
}

// TODO [beresnev] copy-paste
internal fun PsiElement.siblings(forward: Boolean = true, withItself: Boolean = true): Sequence<PsiElement> {
    return object : Sequence<PsiElement> {
        override fun iterator(): Iterator<PsiElement> {
            var next: PsiElement? = this@siblings
            return object : Iterator<PsiElement> {
                init {
                    if (!withItself) next()
                }

                override fun hasNext(): Boolean = next != null
                override fun next(): PsiElement {
                    val result = next ?: throw NoSuchElementException()
                    next = if (forward) result.nextSibling else result.prevSibling
                    return result
                }
            }
        }
    }
}

// TODO [beresnev] copy-paste
internal fun PsiElement.getNextSiblingIgnoringWhitespace(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself).filter { it !is PsiWhiteSpace }.firstOrNull()
}

@InternalDokkaApi
public class PsiDocumentableSource(
    public val psi: PsiNamedElement
) : DocumentableSource {
    override val path: String = psi.containingFile.virtualFile.path

    override fun computeLineNumber(): Int? {
        val range = psi.getChildOfType<PsiIdentifier>()?.textRange ?: psi.textRange
        val doc = PsiDocumentManager.getInstance(psi.project).getDocument(psi.containingFile)
        // IJ uses 0-based line-numbers; external source browsers use 1-based
        return doc?.getLineNumber(range.startOffset)?.plus(1)
    }
}

public inline fun <reified T : PsiElement> PsiElement.getChildOfType(): T? {
    return PsiTreeUtil.getChildOfType(this, T::class.java)
}

internal fun PsiElement.getKotlinFqName(): String? = this.kotlinFqNameProp

//// from import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
internal val PsiElement.kotlinFqNameProp: String?
    get() = when (val element = this) {
        is PsiPackage -> element.qualifiedName
        is PsiClass -> element.qualifiedName
        is PsiMember -> element.name?.let { name ->
            val prefix = element.containingClass?.qualifiedName
            if (prefix != null) "$prefix.$name" else name
        }
//        is KtNamedDeclaration -> element.fqName TODO [beresnev] decide what to do with it
        is PsiQualifiedNamedElement -> element.qualifiedName
        else -> null
    }
