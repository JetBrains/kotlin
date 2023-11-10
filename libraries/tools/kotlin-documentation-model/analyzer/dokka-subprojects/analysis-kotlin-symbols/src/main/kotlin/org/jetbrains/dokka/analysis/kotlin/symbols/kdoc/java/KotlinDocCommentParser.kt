/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.java

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.java.doccomment.DocComment
import org.jetbrains.dokka.analysis.java.parsers.DocCommentParser
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.*
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.logUnresolvedLink
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.parseFromKDocTag
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.resolveKDocLink
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.kotlin.analysis.api.analyze

internal class KotlinDocCommentParser(
    private val context: DokkaContext
) : DocCommentParser {

    override fun canParse(docComment: DocComment): Boolean {
        return docComment is KotlinDocComment
    }

    override fun parse(docComment: DocComment, context: PsiNamedElement): DocumentationNode {
        val kotlinDocComment = docComment as KotlinDocComment
        return parseDocumentation(kotlinDocComment)
    }

    fun parseDocumentation(element: KotlinDocComment, parseWithChildren: Boolean = true): DocumentationNode {
        val sourceSet = context.configuration.sourceSets.let { sourceSets ->
            sourceSets.firstOrNull { it.sourceSetID.sourceSetName == "jvmMain" }
                ?: sourceSets.first { it.analysisPlatform == Platform.jvm }
        }
        val kotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }
        val elementName = element.resolveDocContext.ktElement.name
        return analyze(kotlinAnalysis.getModule(sourceSet)) {
            parseFromKDocTag(
                kDocTag = element.comment,
                externalDri = { link -> resolveKDocLink(link).ifUnresolved { context.logger.logUnresolvedLink(link.getLinkText(), elementName) } },
                kdocLocation = null,
                parseWithChildren = parseWithChildren
            )
        }
    }
}

