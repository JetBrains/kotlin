/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.analysis.java.parsers.JavadocParser
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.checkDecompiledText
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal fun KaSession.getJavaDocDocumentationFrom(
    symbol: KaSymbol,
    javadocParser: JavadocParser
): DocumentationNode? {
    if (symbol.origin == KaSymbolOrigin.JAVA_SOURCE) {
        return (symbol.psi as? PsiNamedElement)?.let {
            javadocParser.parseDocumentation(it)
        }
    } else if (symbol.origin == KaSymbolOrigin.SOURCE && symbol is KaCallableSymbol) {
        // TODO https://youtrack.jetbrains.com/issue/KT-70326/Analysis-API-Inconsistent-allOverriddenSymbols-and-directlyOverriddenSymbols-for-an-intersection-symbol
        val allOverriddenSymbolsWithIntersection = symbol.intersectionOverriddenSymbols.asSequence() + symbol.allOverriddenSymbols

        // Note: javadocParser searches in overridden JAVA declarations for JAVA method, not Kotlin
        allOverriddenSymbolsWithIntersection.forEach { overrider ->
            if (overrider.origin == KaSymbolOrigin.JAVA_SOURCE)
                return@getJavaDocDocumentationFrom (overrider.psi as? PsiNamedElement)?.let {
                    javadocParser.parseDocumentation(it)
                }
        }
    }
    return null
}

internal fun KaSession.getKDocDocumentationFrom(symbol: KaSymbol, logger: DokkaLogger) = findKDoc(symbol)?.let { kDocContent ->

    val ktElement = symbol.psi
    val kdocLocation = ktElement?.containingFile?.name?.let {
        val name = when(symbol) {
            is KaCallableSymbol -> symbol.callableId?.toString()
            is KaClassSymbol -> symbol.classId?.toString()
            is KaNamedSymbol -> symbol.name.asString()
            else -> null
        }?.replace('/', '.') // replace to be compatible with K1

        if (name != null) "$it/$name"
        else it
    }


    parseFromKDocTag(
        kDocTag = kDocContent.contentTag,
        externalDri = { link -> resolveKDocLinkToDRI(link).ifUnresolved { logger.logUnresolvedLink(link.getLinkText(), kdocLocation) } },
        kdocLocation = kdocLocation
    )
}




// ----------- copy-paste from IDE ----------------------------------------------------------------------------

internal data class KDocContent(
    val contentTag: KDocTag,
    val sections: List<KDocSection>
)

internal fun KaSession.findKDoc(symbol: KaSymbol): KDocContent? {
    // Dokka's HACK: primary constructors can be generated
    // so [KtSymbol.psi] is undefined for [KtSymbolOrigin.SOURCE_MEMBER_GENERATED] origin
    // we need to get psi of a containing class
    if(symbol is KaConstructorSymbol && symbol.isPrimary) {
        val containingClass = symbol.fakeOverrideOriginal.containingSymbol as? KaClassSymbol
        if (containingClass?.origin != KaSymbolOrigin.SOURCE) return null
        val kdoc = (containingClass.psi as? KtDeclaration)?.docComment ?: return null

        // duplicates the logic of IDE's `lookupOwnedKDoc`
        val constructorSection = kdoc.findSectionByTag(KDocKnownTag.CONSTRUCTOR)
        if (constructorSection != null) {
            // if annotated with @constructor tag and the caret is on constructor definition,
            // then show @constructor description as the main content, and additional sections
            // that contain @param tags (if any), as the most relatable ones
            // practical example: val foo = Fo<caret>o("argument") -- show @constructor and @param content
            val paramSections = kdoc.findSectionsContainingTag(KDocKnownTag.PARAM)
            return KDocContent(constructorSection, paramSections)
        }
        return KDocContent(kdoc.getDefaultSection(), kdoc.getAllSections())
    }

    // for generated function (e.g. `copy`) [KtSymbol.psi] is undefined (although actually returns a class psi), see test `data class kdocs over generated methods`
    // for DELEGATED/INTERSECTION_OVERRIDE/SUBSTITUTION_OVERRIDE members, it continues search in overridden symbols
    val ktElement = if (symbol.origin == KaSymbolOrigin.SOURCE) symbol.psi as? KtElement else null
    ktElement?.findKDoc()?.let {
        return it
    }

    if (symbol is KaCallableSymbol) {
        // TODO https://youtrack.jetbrains.com/issue/KT-70326/Analysis-API-Inconsistent-allOverriddenSymbols-and-directlyOverriddenSymbols-for-an-intersection-symbol
        val allOverriddenSymbolsWithIntersection = symbol.intersectionOverriddenSymbols.filterNot { it == symbol }.asSequence() + symbol.allOverriddenSymbols

        allOverriddenSymbolsWithIntersection.forEach { overrider ->
            findKDoc(overrider)?.let {
                return it
            }
        }
    }
    return null
}


internal fun KtElement.findKDoc(): KDocContent? = this.lookupOwnedKDoc()
    ?: this.lookupKDocInContainer()



private fun KtElement.lookupOwnedKDoc(): KDocContent? {
    // KDoc for primary constructor is located inside of its class KDoc
    val psiDeclaration = when (this) {
        is KtPrimaryConstructor -> getContainingClassOrObject()
        else -> this
    }

    if (psiDeclaration is KtDeclaration) {
        val kdoc = psiDeclaration.docComment
        if (kdoc != null) {
            if (this is KtConstructor<*>) {
                // ConstructorDescriptor resolves to the same JetDeclaration
                val constructorSection = kdoc.findSectionByTag(KDocKnownTag.CONSTRUCTOR)
                if (constructorSection != null) {
                    // if annotated with @constructor tag and the caret is on constructor definition,
                    // then show @constructor description as the main content, and additional sections
                    // that contain @param tags (if any), as the most relatable ones
                    // practical example: val foo = Fo<caret>o("argument") -- show @constructor and @param content
                    val paramSections = kdoc.findSectionsContainingTag(KDocKnownTag.PARAM)
                    return KDocContent(constructorSection, paramSections)
                }
            }
            return KDocContent(kdoc.getDefaultSection(), kdoc.getAllSections())
        }
    }

    return null
}

/**
 * Looks for sections that have a deeply nested [tag],
 * as opposed to [KDoc.findSectionByTag], which only looks among the top level
 */
private fun KDoc.findSectionsContainingTag(tag: KDocKnownTag): List<KDocSection> {
    return getChildrenOfType<KDocSection>()
        .filter { it.findTagByName(tag.name.toLowerCaseAsciiOnly()) != null }
}

private fun KtElement.lookupKDocInContainer(): KDocContent? {
    val subjectName = name
    val containingDeclaration =
        PsiTreeUtil.findFirstParent(this, true) {
            it is KtDeclarationWithBody && it !is KtPrimaryConstructor
                    || it is KtClassOrObject
        }

    val containerKDoc = containingDeclaration?.getChildOfType<KDoc>()
    if (containerKDoc == null || subjectName == null) return null
    val propertySection = containerKDoc.findSectionByTag(KDocKnownTag.PROPERTY, subjectName)
    val paramTag =
        containerKDoc.findDescendantOfType<KDocTag> { it.knownTag == KDocKnownTag.PARAM && it.getSubjectName() == subjectName }

    val primaryContent = when {
        // class Foo(val <caret>s: String)
        this is KtParameter && this.isPropertyParameter() -> propertySection ?: paramTag
        // fun some(<caret>f: String) || class Some<<caret>T: Base> || Foo(<caret>s = "argument")
        this is KtParameter || this is KtTypeParameter -> paramTag
        // if this property is declared separately (outside primary constructor), but it's for some reason
        // annotated as @property in class's description, instead of having its own KDoc
        this is KtProperty && containingDeclaration is KtClassOrObject -> propertySection
        else -> null
    }
    return primaryContent?.let {
        // makes little sense to include any other sections, since we found
        // documentation for a very specific element, like a property/param
        KDocContent(it, sections = emptyList())
    }
}

private inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(noinline predicate: (T) -> Boolean = { true }): T? {
    return findDescendantOfType({ true }, predicate)
}

private inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true }
): T? {
    checkDecompiledText()
    var result: T? = null
    this.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is T && predicate(element)) {
                result = element
                stopWalking()
                return
            }

            if (canGoInside(element)) {
                super.visitElement(element)
            }
        }
    })
    return result
}

