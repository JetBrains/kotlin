/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc

import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtPossibleMemberSymbol
import org.jetbrains.kotlin.builtins.StandardNames

private const val ENUM_ENTRIES_TEMPLATE_PATH = "/dokka/docs/kdoc/EnumEntries.kt.template"
private const val ENUM_VALUEOF_TEMPLATE_PATH = "/dokka/docs/kdoc/EnumValueOf.kt.template"
private const val ENUM_VALUES_TEMPLATE_PATH = "/dokka/docs/kdoc/EnumValues.kt.template"

internal fun KtAnalysisSession.hasGeneratedKDocDocumentation(symbol: KtSymbol): Boolean =
    getDocumentationTemplatePath(symbol) != null

private fun KtAnalysisSession.getDocumentationTemplatePath(symbol: KtSymbol): String? =
    when (symbol) {
        is KtPropertySymbol -> if (isEnumEntriesProperty(symbol)) ENUM_ENTRIES_TEMPLATE_PATH else null
        is KtFunctionSymbol -> {
            when {
                isEnumValuesMethod(symbol) -> ENUM_VALUES_TEMPLATE_PATH
                isEnumValueOfMethod(symbol) -> ENUM_VALUEOF_TEMPLATE_PATH
                else -> null
            }
        }

        else -> null
    }

private fun KtAnalysisSession.isEnumSpecialMember(symbol: KtPossibleMemberSymbol): Boolean =
    symbol.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED
            && (symbol.getContainingSymbol() as? KtClassOrObjectSymbol)?.classKind == KtClassKind.ENUM_CLASS

private fun KtAnalysisSession.isEnumEntriesProperty(symbol: KtPropertySymbol): Boolean =
    symbol.name == StandardNames.ENUM_ENTRIES && isEnumSpecialMember(symbol)

private fun KtAnalysisSession.isEnumValuesMethod(symbol: KtFunctionSymbol): Boolean =
    symbol.name == StandardNames.ENUM_VALUES && isEnumSpecialMember(symbol)

private fun KtAnalysisSession.isEnumValueOfMethod(symbol: KtFunctionSymbol): Boolean =
    symbol.name == StandardNames.ENUM_VALUE_OF && isEnumSpecialMember(symbol)

internal fun KtAnalysisSession.getGeneratedKDocDocumentationFrom(symbol: KtSymbol): DocumentationNode? {
    val templatePath = getDocumentationTemplatePath(symbol) ?: return null
    return loadTemplate(templatePath)
}

private fun KtAnalysisSession.loadTemplate(filePath: String): DocumentationNode? {
    val kdoc = loadContent(filePath) ?: return null
    val externalDriProvider = { link: String ->
        resolveKDocTextLinkToDRI(link)
    }

    val parser = MarkdownParser(externalDriProvider, filePath)
    return parser.parse(kdoc)
}

private fun loadContent(filePath: String): String? =
    SymbolsAnalysisPlugin::class.java.getResource(filePath)?.readText()
