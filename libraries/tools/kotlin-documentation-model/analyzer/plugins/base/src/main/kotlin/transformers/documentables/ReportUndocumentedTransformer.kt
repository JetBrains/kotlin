/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin

internal class ReportUndocumentedTransformer : DocumentableTransformer {

    override fun invoke(original: DModule, context: DokkaContext): DModule = original.apply {
        withDescendants().forEach { documentable -> invoke(documentable, context) }
    }

    private fun invoke(documentable: Documentable, context: DokkaContext) {
        documentable.sourceSets.forEach { sourceSet ->
            if (shouldBeReportedIfNotDocumented(documentable, sourceSet, context)) {
                reportIfUndocumented(context, documentable, sourceSet)
            }
        }
    }

    private fun shouldBeReportedIfNotDocumented(
        documentable: Documentable, sourceSet: DokkaSourceSet, context: DokkaContext
    ): Boolean {
        val packageOptionsOrNull = packageOptionsOrNull(sourceSet, documentable)

        if (!(packageOptionsOrNull?.reportUndocumented ?: sourceSet.reportUndocumented)) {
            return false
        }

        if (documentable is DParameter || documentable is DPackage || documentable is DModule) {
            return false
        }

        if (isConstructor(documentable)) {
            return false
        }

        val syntheticDetector = context.plugin<InternalKotlinAnalysisPlugin>().querySingle { syntheticDocumentableDetector }
        if (syntheticDetector.isSynthetic(documentable, sourceSet)) {
            return false
        }

        if (isPrivateOrInternalApi(documentable, sourceSet)) {
            return false
        }

        return true
    }

    private fun reportIfUndocumented(
        context: DokkaContext,
        documentable: Documentable,
        sourceSet: DokkaSourceSet
    ) {
        if (isUndocumented(documentable, sourceSet)) {
            val documentableDescription = with(documentable) {
                buildString {
                    dri.packageName?.run {
                        append(this)
                        append("/")
                    }

                    dri.classNames?.run {
                        append(this)
                        append("/")
                    }

                    dri.callable?.run {
                        append(name)
                        append("/")
                        append(signature())
                        append("/")
                    }

                    val sourceSetName = sourceSet.displayName
                    if (sourceSetName != null.toString()) {
                        append(" ($sourceSetName)")
                    }
                }
            }

            context.logger.warn("Undocumented: $documentableDescription")
        }
    }

    private fun isUndocumented(documentable: Documentable, sourceSet: DokkaSourceSet): Boolean {
        fun resolveDependentSourceSets(sourceSet: DokkaSourceSet): List<DokkaSourceSet> {
            return sourceSet.dependentSourceSets.mapNotNull { sourceSetID ->
                documentable.sourceSets.singleOrNull { it.sourceSetID == sourceSetID }
            }
        }

        fun withAllDependentSourceSets(sourceSet: DokkaSourceSet): Sequence<DokkaSourceSet> = sequence {
            yield(sourceSet)
            for (dependentSourceSet in resolveDependentSourceSets(sourceSet)) {
                yieldAll(withAllDependentSourceSets(dependentSourceSet))
            }
        }


        return withAllDependentSourceSets(sourceSet).all { sourceSetOrDependentSourceSet ->
            documentable.documentation[sourceSetOrDependentSourceSet]?.children?.isEmpty() ?: true
        }
    }

    private fun isConstructor(documentable: Documentable): Boolean {
        if (documentable !is DFunction) return false
        return documentable.isConstructor
    }

    private fun isPrivateOrInternalApi(documentable: Documentable, sourceSet: DokkaSourceSet): Boolean {
        return when ((documentable as? WithVisibility)?.visibility?.get(sourceSet)) {
            KotlinVisibility.Public -> false
            KotlinVisibility.Private -> true
            KotlinVisibility.Protected -> true
            KotlinVisibility.Internal -> true
            JavaVisibility.Public -> false
            JavaVisibility.Private -> true
            JavaVisibility.Protected -> true
            JavaVisibility.Default -> true
            null -> false
        }
    }

    private fun packageOptionsOrNull(
        dokkaSourceSet: DokkaSourceSet,
        documentable: Documentable
    ): DokkaConfiguration.PackageOptions? {
        val packageName = documentable.dri.packageName ?: return null
        return dokkaSourceSet.perPackageOptions
            .filter { packageOptions -> Regex(packageOptions.matchingRegex).matches(packageName) }
            .maxByOrNull { packageOptions -> packageOptions.matchingRegex.length }
    }
}
