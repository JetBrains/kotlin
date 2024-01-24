/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.java.JavaAnalysisPlugin
import org.jetbrains.dokka.analysis.java.parsers.JavadocParser
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.DokkaSymbolVisitor
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getClassIdFromDRI
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getDRIFromSymbol
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.analysis.kotlin.documentable.ExternalDocumentableProvider

internal class SymbolExternalDocumentablesProvider(val context: DokkaContext) : ExternalDocumentableProvider {
    private val kotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }

    override fun getClasslike(dri: DRI, sourceSet: DokkaSourceSet): DClasslike? {
        val classId = getClassIdFromDRI(dri)

        return analyze(kotlinAnalysis.getModule(sourceSet)) {
            val symbol = getClassOrObjectSymbolByClassId(classId) as? KtNamedClassOrObjectSymbol?: return@analyze null
            val javadocParser =
                if (sourceSet.analysisPlatform == Platform.jvm)
                    JavadocParser(
                        docCommentParsers = context.plugin<JavaAnalysisPlugin>().query { docCommentParsers },
                        docCommentFinder = context.plugin<JavaAnalysisPlugin>().docCommentFinder
                    )
                else null
            val translator = DokkaSymbolVisitor(sourceSet, sourceSet.displayName, kotlinAnalysis, logger = context.logger, javadocParser)

            val parentDRI = symbol.getContainingSymbol()?.let { getDRIFromSymbol(it) } ?: /* top level */ DRI(dri.packageName)
            with(translator) {
                return@analyze visitNamedClassOrObjectSymbol(symbol, parentDRI)
            }
        }
    }
}
