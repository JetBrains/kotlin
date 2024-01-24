/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentation.Classifier
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.doc.Deprecated
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.associateWithNotNull
import org.jetbrains.dokka.analysis.kotlin.internal.ModuleAndPackageDocumentationReader

internal fun ModuleAndPackageDocumentationReader(context: DokkaContext): ModuleAndPackageDocumentationReader =
    ContextModuleAndPackageDocumentationReader(context)

private class ContextModuleAndPackageDocumentationReader(
    private val context: DokkaContext
) : ModuleAndPackageDocumentationReader {

    private val kotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }

    private val documentationFragments: SourceSetDependent<List<ModuleAndPackageDocumentationFragment>> =
        context.configuration.sourceSets.associateWith { sourceSet ->
            sourceSet.includes.flatMap { include -> parseModuleAndPackageDocumentationFragments(include) }
        }

    private fun findDocumentationNodes(
        sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
        predicate: (ModuleAndPackageDocumentationFragment) -> Boolean
    ): SourceSetDependent<DocumentationNode> {
        return sourceSets.associateWithNotNull { sourceSet ->
            val fragments = documentationFragments[sourceSet].orEmpty().filter(predicate)
            kotlinAnalysis.getModule(sourceSet)// test: to throw exception for unknown sourceSet
            val documentations = fragments.map { fragment ->
                parseModuleAndPackageDocumentation(
                    context = ModuleAndPackageDocumentationParsingContext(context.logger, kotlinAnalysis, sourceSet),
                    fragment = fragment
                )
            }
            when (documentations.size) {
                0 -> null
                1 -> documentations.single().documentation
                else -> DocumentationNode(documentations.flatMap { it.documentation.children }
                    .mergeDocumentationNodes())
            }
        }
    }

    private val ModuleAndPackageDocumentationFragment.canonicalPackageName: String
        get() {
            check(classifier == Classifier.Package)
            if (name == "[root]") return ""
            return name
        }

    override fun read(module: DModule): SourceSetDependent<DocumentationNode> {
        return findDocumentationNodes(module.sourceSets) { fragment ->
            fragment.classifier == Classifier.Module && (fragment.name == module.name)
        }
    }

    override fun read(pkg: DPackage): SourceSetDependent<DocumentationNode> {
        return findDocumentationNodes(pkg.sourceSets) { fragment ->
            fragment.classifier == Classifier.Package && fragment.canonicalPackageName == pkg.dri.packageName
        }
    }

    override fun read(module: DokkaConfiguration.DokkaModuleDescription): DocumentationNode? {
        val parsingContext = ModuleAndPackageDocumentationParsingContext(context.logger)

        val documentationFragment = module.includes
            .flatMap { include -> parseModuleAndPackageDocumentationFragments(include) }
            .firstOrNull { fragment -> fragment.classifier == Classifier.Module && fragment.name == module.name }
            ?: return null

        val moduleDocumentation = parseModuleAndPackageDocumentation(parsingContext, documentationFragment)
        return moduleDocumentation.documentation
    }

    private fun List<TagWrapper>.mergeDocumentationNodes(): List<TagWrapper> =
        groupBy { it::class }.values.map {
            it.reduce { acc, tagWrapper ->
                val newRoot = CustomDocTag(
                    acc.children + tagWrapper.children,
                    name = (tagWrapper as? NamedTagWrapper)?.name.orEmpty()
                )
                when (acc) {
                    is See -> acc.copy(newRoot)
                    is Param -> acc.copy(newRoot)
                    is Throws -> acc.copy(newRoot)
                    is Sample -> acc.copy(newRoot)
                    is Property -> acc.copy(newRoot)
                    is CustomTagWrapper -> acc.copy(newRoot)
                    is Description -> acc.copy(newRoot)
                    is Author -> acc.copy(newRoot)
                    is Version -> acc.copy(newRoot)
                    is Since -> acc.copy(newRoot)
                    is Return -> acc.copy(newRoot)
                    is Receiver -> acc.copy(newRoot)
                    is Constructor -> acc.copy(newRoot)
                    is Deprecated -> acc.copy(newRoot)
                    is org.jetbrains.dokka.model.doc.Suppress -> acc.copy(newRoot)
                }
            }
        }
}
