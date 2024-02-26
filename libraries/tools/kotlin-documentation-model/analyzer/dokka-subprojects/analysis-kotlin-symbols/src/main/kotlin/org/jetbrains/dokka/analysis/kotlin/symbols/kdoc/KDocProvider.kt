/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.analysis.java.parsers.JavadocParser
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal fun KtAnalysisSession.getJavaDocDocumentationFrom(
    symbol: KtSymbol,
    javadocParser: JavadocParser
): DocumentationNode? {
    if (symbol.origin == KtSymbolOrigin.JAVA) {
        return (symbol.psi as? PsiNamedElement)?.let {
            javadocParser.parseDocumentation(it)
        }
    } else if (symbol.origin == KtSymbolOrigin.SOURCE && symbol is KtCallableSymbol) {
        // Note: javadocParser searches in overridden JAVA declarations for JAVA method, not Kotlin
        symbol.getAllOverriddenSymbols().forEach { overrider ->
            if (overrider.origin == KtSymbolOrigin.JAVA)
                return@getJavaDocDocumentationFrom (overrider.psi as? PsiNamedElement)?.let {
                    javadocParser.parseDocumentation(it)
                }
        }
    }
    return null
}

internal fun KtAnalysisSession.getKDocDocumentationFrom(symbol: KtSymbol, logger: DokkaLogger) = findKDoc(symbol)?.let { kDocContent ->

    val ktElement = symbol.psi
    val kdocLocation = ktElement?.containingFile?.name?.let {
        val name = when(symbol) {
            is KtCallableSymbol -> symbol.callableIdIfNonLocal?.toString()
            is KtClassOrObjectSymbol -> symbol.classIdIfNonLocal?.toString()
            is KtNamedSymbol -> symbol.name.asString()
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

internal fun KtAnalysisSession.findKDoc(symbol: KtSymbol): KDocContent? {
    // Dokka's HACK: primary constructors can be generated
    // so [KtSymbol.psi] is undefined for [KtSymbolOrigin.SOURCE_MEMBER_GENERATED] origin
    // we need to get psi of a containing class
    if(symbol is KtConstructorSymbol && symbol.isPrimary) {
        val containingClass = symbol.originalContainingClassForOverride
        if (containingClass?.origin != KtSymbolOrigin.SOURCE) return null
        val kdoc = (containingClass.psi as? KtDeclaration)?.docComment ?: return null
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

    // for generated function (e.g. `copy`) [KtSymbol.psi] is undefined (although actually returns a class psi), see test `data class kdocs over generated methods`
    if (symbol.origin != KtSymbolOrigin.SOURCE) return null


    val ktElement = symbol.psi as? KtElement
    ktElement?.findKDoc()?.let {
        return it
    }

    if (symbol is KtCallableSymbol) {
        symbol.getAllOverriddenSymbols().forEach { overrider ->
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

