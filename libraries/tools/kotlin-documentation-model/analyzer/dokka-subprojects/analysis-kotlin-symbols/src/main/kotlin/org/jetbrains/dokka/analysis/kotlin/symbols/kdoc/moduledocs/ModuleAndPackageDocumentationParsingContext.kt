/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentation.Classifier.Module
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentation.Classifier.Package
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.resolveKDocTextLink
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.KotlinAnalysis
import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.analyze

internal fun interface ModuleAndPackageDocumentationParsingContext {
    fun markdownParserFor(fragment: ModuleAndPackageDocumentationFragment, location: String): MarkdownParser
}

internal fun ModuleAndPackageDocumentationParsingContext.parse(
    fragment: ModuleAndPackageDocumentationFragment
): DocumentationNode {
    return markdownParserFor(fragment, fragment.source.sourceDescription).parse(fragment.documentation)
}

internal fun ModuleAndPackageDocumentationParsingContext(
    logger: DokkaLogger,
    kotlinAnalysis: KotlinAnalysis? = null,
    sourceSet: DokkaConfiguration.DokkaSourceSet? = null
) = ModuleAndPackageDocumentationParsingContext { fragment, sourceLocation ->

    if (kotlinAnalysis == null || sourceSet == null) {
        MarkdownParser(externalDri = { null }, sourceLocation)
    } else {
        val sourceModule = kotlinAnalysis.getModule(sourceSet)
        val contextPackageFQN = when (fragment.classifier) {
            Module -> null
            Package -> fragment.name
        }
        val locationInformation = when (fragment.classifier) {
            Module -> "module documentation"
            Package -> "'${fragment.name}' package documentation"
        }
        MarkdownParser(
            externalDri = { link ->
                analyze(sourceModule) {
                    resolveKDocTextLink(link, contextPackageFQN, locationInformation, logger, sourceSet)
                }
            },
            sourceLocation
        )
    }
}
