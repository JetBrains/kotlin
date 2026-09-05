/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.internal.ModuleAndPackageDocumentationReader

internal class ModuleAndPackageDocumentationTransformer(
    private val moduleAndPackageDocumentationReader: ModuleAndPackageDocumentationReader
) : PreMergeDocumentableTransformer {

    constructor(context: DokkaContext) : this(
        context.plugin<InternalKotlinAnalysisPlugin>().querySingle { moduleAndPackageDocumentationReader }
    )

    override fun invoke(modules: List<DModule>): List<DModule> {
        return modules.map { module ->
            module.copy(
                documentation = module.documentation + moduleAndPackageDocumentationReader.read(module),
                packages = module.packages.map { pkg ->
                    pkg.copy(
                        documentation = pkg.documentation + moduleAndPackageDocumentationReader.read(pkg)
                    )
                }
            )
        }
    }

    private operator fun SourceSetDependent<DocumentationNode>.plus(
        other: SourceSetDependent<DocumentationNode>
    ): Map<DokkaSourceSet, DocumentationNode> =
        (asSequence() + other.asSequence())
            .distinct()
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, values) -> DocumentationNode(values.flatMap { it.children }) }

}
