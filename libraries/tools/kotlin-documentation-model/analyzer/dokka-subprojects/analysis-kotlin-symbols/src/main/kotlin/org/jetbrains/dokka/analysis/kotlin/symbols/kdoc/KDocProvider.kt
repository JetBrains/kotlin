/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.java.parsers.JavadocParser
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNonPublicApi

internal fun KaSession.getJavaDocDocumentationFrom(
    symbol: KaSymbol,
    javadocParser: JavadocParser,
    sourceSet: DokkaSourceSet
): DocumentationNode? {
    if (symbol.origin == KaSymbolOrigin.JAVA_SOURCE) {
        return (symbol.psi as? PsiNamedElement)?.let {
            javadocParser.parseDocumentation(it, sourceSet)
        }
    } else if (symbol.origin == KaSymbolOrigin.SOURCE && symbol is KaCallableSymbol) {
        // TODO https://youtrack.jetbrains.com/issue/KT-70326/Analysis-API-Inconsistent-allOverriddenSymbols-and-directlyOverriddenSymbols-for-an-intersection-symbol
        val allOverriddenSymbolsWithIntersection = symbol.intersectionOverriddenSymbols.asSequence() + symbol.allOverriddenSymbols

        // Note: javadocParser searches in overridden JAVA declarations for JAVA method, not Kotlin
        allOverriddenSymbolsWithIntersection.forEach { overrider ->
            if (overrider.origin == KaSymbolOrigin.JAVA_SOURCE)
                return@getJavaDocDocumentationFrom (overrider.psi as? PsiNamedElement)?.let {
                    javadocParser.parseDocumentation(it, sourceSet)
                }
        }
    }
    return null
}

@OptIn(KaNonPublicApi::class, KtNonPublicApi::class)
internal fun KaSession.getKDocDocumentationFrom(
    symbol: KaSymbol,
    logger: DokkaLogger,
    sourceSet: DokkaSourceSet,
): DocumentationNode? = (symbol as? KaDeclarationSymbol)?.findKDoc()?.let { kDocContent ->
    val kdocSymbolName = when (symbol) {
        is KaCallableSymbol -> symbol.callableId?.asSingleFqName()?.asString()
        is KaClassSymbol -> symbol.classId?.asFqNameString()
        is KaNamedSymbol -> symbol.name.asString()
        else -> null
    }
    val kdocFileName = symbol.psi?.containingFile?.name
    val kdocLocation = when {
        kdocFileName != null && kdocSymbolName != null -> "$kdocFileName/$kdocSymbolName"
        kdocFileName != null -> kdocFileName
        kdocSymbolName != null -> kdocSymbolName
        else -> null
    }

    parseFromKDocTag(
        kDocTag = kDocContent.primaryTag,
        externalDri = { resolveKDocLink(it, kdocLocation, logger, sourceSet) },
        kdocLocation = kdocLocation
    )
}

@OptIn(KtNonPublicApi::class, KaNonPublicApi::class)
internal fun KtDeclaration.findKDoc() = analyze(this) { this@findKDoc.findKDoc() }
