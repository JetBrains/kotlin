/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.java.JavaAnalysisPlugin
import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.dokka.analysis.java.parsers.JavadocParser
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.DokkaSymbolVisitor
import org.jetbrains.kotlin.documentation.analyzer.ids.classIdFromDeclarationId
import org.jetbrains.kotlin.documentation.analyzer.ids.isClassifierDeclarationId
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.analysis.kotlin.documentable.ExternalDocumentableProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.containingSymbol
import org.jetbrains.kotlin.analysis.api.symbols.findClass

internal class SymbolExternalDocumentablesProvider(val context: DokkaContext) : ExternalDocumentableProvider {
    private val kotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }
    private val lightMethodChecker = context.plugin<JavaAnalysisPlugin>().querySingle { kotlinLightMethodChecker }

    override fun getClasslike(declarationId: KdElementId, sourceSet: DokkaSourceSet): DClasslike? {
        // Only classifier IDs denote a classlike; package and callable IDs (e.g. a function ID used
        // by mistake) simply have no matching classlike.
        if (!isClassifierDeclarationId(declarationId)) return null
        val classId = classIdFromDeclarationId(declarationId)

        return analyze(kotlinAnalysis.getModule(sourceSet)) {
            val symbol = findClass(classId) ?: return@analyze null
            val javadocParser =
                if (sourceSet.analysisPlatform == Platform.jvm)
                    JavadocParser(
                        docCommentParsers = context.plugin<JavaAnalysisPlugin>().query { docCommentParsers },
                        docCommentFinder = context.plugin<JavaAnalysisPlugin>().docCommentFinder
                    )
                else null
            val translator = DokkaSymbolVisitor(
                sourceSet = sourceSet,
                moduleName = sourceSet.displayName,
                analysisContext = kotlinAnalysis,
                logger = context.logger,
                javadocParser = javadocParser,
                lightMethodChecker = lightMethodChecker
            )

            with(translator) {
                return@analyze visitClassSymbol(symbol)
            }
        }
    }
}
