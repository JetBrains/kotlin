/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.DocumentableSource
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.javaInterop.callableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.scopes.*
import org.jetbrains.kotlin.analysis.api.javaInterop.namedClassSymbol
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.kotlin.documentation.analyzer.ids.generateDeclarationId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration


internal val PsiElement.parentsWithSelf: Sequence<PsiElement>
    get() = generateSequence(this) { if (it is PsiFile) null else it.parent }

/**
 * The declaration ID of the given resolved [psi] element, built via Analysis API symbols exclusively,
 * or `null` if no symbol can be obtained for [psi].
 *
 * Requires an ambient [KaSession] which must see [psi] (for binary declarations,
 * a session of a source module that has the declaration on its classpath). Elements outside
 * the session's analysis scope (e.g. a source class of another module) have no symbol and
 * yield `null`.
 */
context(_: KaSession)
internal fun declarationIdFrom(psi: PsiElement): KdElementId? {
    val symbol = psi.toKaSymbol() ?: return null
    return generateDeclarationId(symbol)
}

/**
 * Overload for callers without an ambient [KaSession]:
 * the analysis is performed from [useSite]'s module instead.
 * [useSite] must be an element from a source file (a doc tag, an annotation, etc.).
 */
internal fun declarationIdFrom(psi: PsiElement, useSite: PsiElement): KdElementId? =
    analyze(useSite) { declarationIdFrom(psi) }

/**
 * Returns a [KaSymbol] corresponding to the given [PsiElement].
 *
 * Kotlin light elements (Javadoc references on Kotlin declarations are resolved to those)
 * are unwrapped to their original Kotlin PSI, so both Kotlin and Java declarations
 * are represented by symbols.
 */
context(_: KaSession)
private fun PsiElement.toKaSymbol(): KaSymbol? {
    val kotlinOrigin = (this as? KtLightElement<*, *>)?.kotlinOrigin
    return when {
        kotlinOrigin is KtDeclaration -> kotlinOrigin.symbol
        // A JVM file facade (e.g. `example.TestKt`) has no class symbol: its declarations are
        // top-level callables of the package, so the package symbol is the closest representation.
        this is KtLightClassForFacade -> findPackage(facadeClassFqName.parent())
        this is PsiPackage -> findPackage(FqName(qualifiedName))
        this is PsiTypeParameter -> (containingDeclarationSymbol() as? KaDeclarationSymbol)
            ?.typeParameters?.firstOrNull { it.psi == this }
        this is PsiParameter -> (containingDeclarationSymbol() as? KaFunctionSymbol)
            ?.valueParameters?.firstOrNull { it.psi == this }
        this is PsiClass -> classSymbol()
        this is PsiMember -> memberSymbol()
        else -> null
    }
}

/**
 * Returns a class symbol for the given [PsiClass].
 *
 * Classes from compiled class files (e.g. the JDK or other libraries) have no PSI-based symbol,
 * so they are resolved by [ClassId] instead.
 */
context(_: KaSession)
private fun PsiClass.classSymbol(): KaNamedClassSymbol? {
    if (containingFile is ClsFileImpl) {
        // TODO KT-92120: decompiled classes are outside every module's analysis scope, so
        //  `namedClassSymbol` throws for them; they are resolved by `ClassId` instead.
        return classIdOrNull()?.let { findClass(it) }
    }
    // `namedClassSymbol` throws `KaFirIllegalPsiException` for PSI outside the session's analysis scope
    // (e.g. a source class of another source set, resolved via a Javadoc link). Resolving such a class
    // by `ClassId` does not help either: the foreign source PSI shadows the classpath candidate for the
    // class finder, so the lookup yields nothing. Instead of failing the whole run, the failure is caught
    // and the link degrades to an unresolved-link warning.
    return runCatching { namedClassSymbol }.getOrNull()
}

context(_: KaSession)
private fun PsiClass.classIdOrNull(): ClassId? {
    val name = name ?: return null
    val outerClassId = containingClass?.classIdOrNull()
    if (outerClassId != null) {
        return outerClassId.createNestedClassId(Name.identifier(name))
    }
    val qualifiedName = qualifiedName ?: return null
    return ClassId(FqName(qualifiedName.substringBeforeLast('.', "")), FqName(name), false)
}

/**
 * Returns a callable symbol for the given [PsiMember].
 *
 * Members of decompiled classes (Cls PSI, e.g. Javadoc references to JDK members) are resolved
 * through the owner class symbol by name/signature: decompiled files are outside every module's
 * analysis scope, so `callableSymbol` throws there, and PSI identity is unreliable for them
 * (the same declaration can be represented by distinct Cls PSI trees).
 *
 * For source members, [org.jetbrains.kotlin.analysis.api.javaInterop.callableSymbol] is used;
 * other out-of-scope PSI (e.g. a source class of another source set) yields no symbol.
 *
 * Java enum constants are represented by [KaEnumEntrySymbol], so their declaration ID is generated
 * the same way as for Kotlin enum entries.
 */
context(_: KaSession)
private fun PsiMember.memberSymbol(): KaCallableSymbol? {
    val ownerClass = containingClass ?: return null

    // TODO KT-92120: once the Analysis API supports creating symbols from decompiled Java PSI,
    //  drop this branch and use `callableSymbol` for Cls PSI as well.
    if (containingFile is ClsFileImpl) {
        val owner = ownerClass.classSymbol() ?: return null
        if (this is PsiMethod && isConstructor) {
            val constructors = owner.combinedDeclaredMemberScope.constructors.toList()
            return constructors.firstOrNull { it.psi == this }
                ?: constructors.firstOrNull { it.valueParameters.size == parameterList.parametersCount }
        }

        val memberName = name?.let(Name::identifier) ?: return null
        val callables = owner.staticDeclaredMemberScope.callables(memberName).toList() +
                owner.declaredMemberScope.callables(memberName).toList()
        return callables.firstOrNull { it.psi == this } ?: callables.firstOrNull()
    }

    // `callableSymbol` throws `KaFirIllegalPsiException` for PSI outside the session's analysis
    // scope (see [classSymbol]); for such members (e.g. a source class of another source set)
    // no symbol is resolvable, and the link degrades to an unresolved-link warning.
    return runCatching { callableSymbol }.getOrNull()
}

context(_: KaSession)
private fun PsiElement.containingDeclarationSymbol(): KaSymbol? {
    val owner = parentsWithSelf.drop(1).firstOrNull { it is PsiClass || it is PsiMethod } ?: return null
    return owner.toKaSymbol()
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
internal class PsiDocumentableSource(
    val psi: PsiNamedElement
) : DocumentableSource {
    override val path: String = psi.containingFile.virtualFile.path

    override fun computeLineNumber(): Int? {
        val range = psi.getChildOfType<PsiIdentifier>()?.textRange ?: psi.textRange
        val doc = PsiDocumentManager.getInstance(psi.project).getDocument(psi.containingFile)
        // IJ uses 0-based line-numbers; external source browsers use 1-based
        return doc?.getLineNumber(range.startOffset)?.plus(1)
    }
}

internal inline fun <reified T : PsiElement> PsiElement.getChildOfType(): T? {
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

/**
 * Copy-paste from [org.jetbrains.dokka.analysis.kotlin.symbols.utils.getLocation]
 * @return a string in the format `file://file.kt:<line>:<column>`,
 * or `null` if [psiElement] is not from a source file
 */
internal fun getLocation(psiElement: PsiElement): String? {
    val filePath = psiElement.containingFile?.virtualFile?.path ?: return null
    val offset = psiElement.textOffset
    val fileDocument = psiElement.containingFile?.fileDocument
    val lineNumber = fileDocument?.getLineNumber(offset)

    return if (lineNumber != null) {
        val column =  offset - fileDocument.getLineStartOffset(lineNumber)
        "file:///$filePath:${lineNumber + 1}:${column + 1}"
    } else
        "file:///$filePath"
}
