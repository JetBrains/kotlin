package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

class EmptyPackagesFilterTransformer(val context: DokkaContext) : PreMergeDocumentableTransformer {
    override fun invoke(modules: List<DModule>): List<DModule> {
        return modules.mapNotNull { module -> filterModule(module) }
    }

    private fun filterModule(module: DModule): DModule? {
        val nonEmptyPackages = module.packages.filterNot { pkg ->
            pkg.sourceSet.skipEmptyPackages && pkg.children.isEmpty()
        }

        return when {
            nonEmptyPackages == module.packages -> module
            nonEmptyPackages.isEmpty() -> null
            else -> module.copy(packages = nonEmptyPackages)
        }
    }
}
