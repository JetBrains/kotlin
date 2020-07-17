package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

class EmptyPackagesFilterTransformer(val context: DokkaContext) : PreMergeDocumentableTransformer {
    override fun invoke(modules: List<DModule>): List<DModule> = modules.map { original ->
        original.let {
            EmptyPackagesFilter(original.sourceSets.single()).processModule(it)
        }
    }

    private class EmptyPackagesFilter(
        val sourceSet: DokkaConfiguration.DokkaSourceSet
    ) {
        fun DPackage.shouldBeSkipped() = sourceSet.skipEmptyPackages &&
                functions.isEmpty() &&
                properties.isEmpty() &&
                classlikes.isEmpty()

        fun processModule(module: DModule) = module.copy(
            packages = module.packages.filter { !it.shouldBeSkipped() }
        )
    }
}
