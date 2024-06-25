/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc

import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaPossibleMemberSymbol
import org.jetbrains.kotlin.builtins.StandardNames

private const val ENUM_ENTRIES_TEMPLATE_PATH = "/dokka/docs/kdoc/EnumEntries.kt.template"
private const val ENUM_VALUEOF_TEMPLATE_PATH = "/dokka/docs/kdoc/EnumValueOf.kt.template"
private const val ENUM_VALUES_TEMPLATE_PATH = "/dokka/docs/kdoc/EnumValues.kt.template"

internal fun KaSession.hasGeneratedKDocDocumentation(symbol: KaSymbol): Boolean =
    getDocumentationTemplatePath(symbol) != null

private fun KaSession.getDocumentationTemplatePath(symbol: KaSymbol): String? =
    when (symbol) {
        is KaPropertySymbol -> if (isEnumEntriesProperty(symbol)) ENUM_ENTRIES_TEMPLATE_PATH else null
        is KaNamedFunctionSymbol -> {
            when {
                isEnumValuesMethod(symbol) -> ENUM_VALUES_TEMPLATE_PATH
                isEnumValueOfMethod(symbol) -> ENUM_VALUEOF_TEMPLATE_PATH
                else -> null
            }
        }

        else -> null
    }

private fun KaSession.isEnumSpecialMember(symbol: KaPossibleMemberSymbol): Boolean =
    symbol.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED
            && (symbol.containingSymbol as? KaClassSymbol)?.classKind == KaClassKind.ENUM_CLASS

private fun KaSession.isEnumEntriesProperty(symbol: KaPropertySymbol): Boolean =
    symbol.name == StandardNames.ENUM_ENTRIES && isEnumSpecialMember(symbol)

private fun KaSession.isEnumValuesMethod(symbol: KaNamedFunctionSymbol): Boolean =
    symbol.name == StandardNames.ENUM_VALUES && isEnumSpecialMember(symbol)

private fun KaSession.isEnumValueOfMethod(symbol: KaNamedFunctionSymbol): Boolean =
    symbol.name == StandardNames.ENUM_VALUE_OF && isEnumSpecialMember(symbol)

internal fun KaSession.getGeneratedKDocDocumentationFrom(symbol: KaSymbol): DocumentationNode? {
    val templatePath = getDocumentationTemplatePath(symbol) ?: return null
    return loadTemplate(templatePath)
}

private fun KaSession.loadTemplate(filePath: String): DocumentationNode? {
    val kdoc = loadContent(filePath) ?: return null
    val externalDriProvider = { link: String ->
        resolveKDocTextLinkToDRI(link)
    }

    val parser = MarkdownParser(externalDriProvider, filePath)
    return parser.parse(kdoc)
}

private fun loadContent(filePath: String): String? =
    SymbolsAnalysisPlugin::class.java.getResource(filePath)?.readText()
