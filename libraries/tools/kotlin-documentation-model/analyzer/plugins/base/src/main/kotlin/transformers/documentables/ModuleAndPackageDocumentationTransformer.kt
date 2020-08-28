package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

internal class ModuleAndPackageDocumentationTransformer(
    private val moduleAndPackageDocumentationReader: ModuleAndPackageDocumentationReader
) : PreMergeDocumentableTransformer {

    override fun invoke(modules: List<DModule>): List<DModule> {
        return modules.map { module ->
            module.copy(
                documentation = module.documentation + moduleAndPackageDocumentationReader[module],
                packages = module.packages.map { pkg ->
                    pkg.copy(
                        documentation = pkg.documentation + moduleAndPackageDocumentationReader[pkg]
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
