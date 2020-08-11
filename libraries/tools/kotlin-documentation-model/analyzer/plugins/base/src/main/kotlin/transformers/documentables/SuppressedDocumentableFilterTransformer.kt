package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import java.io.File

class SuppressedDocumentableFilterTransformer(val context: DokkaContext) : PreMergeDocumentableTransformer {
    override fun invoke(modules: List<DModule>): List<DModule> {
        return modules.mapNotNull { module -> filterModule(module) }
    }

    private fun filterModule(module: DModule): DModule? {
        val packages = module.packages.mapNotNull { pkg -> filterPackage(pkg) }
        return when {
            packages == module.packages -> module
            packages.isEmpty() -> null
            else -> module.copy(packages = packages)
        }
    }

    private fun filterPackage(pkg: DPackage): DPackage? {
        val options = pkg.perPackageOptions
        if (options?.suppress == true) {
            return null
        }

        val filteredChildren = pkg.children.filter { child -> !isSuppressed(child) }
        return when {
            filteredChildren == pkg.children -> pkg
            filteredChildren.isEmpty() -> null
            else -> pkg.copy(
                functions = filteredChildren.filterIsInstance<DFunction>(),
                classlikes = filteredChildren.filterIsInstance<DClasslike>(),
                typealiases = filteredChildren.filterIsInstance<DTypeAlias>(),
                properties = filteredChildren.filterIsInstance<DProperty>()
            )
        }
    }

    private fun isSuppressed(documentable: Documentable): Boolean {
        if (documentable !is WithExpectActual) return false
        val sourceFile = File(documentable.source.path).absoluteFile
        return documentable.sourceSet.suppressedFiles.any { suppressedFile ->
            sourceFile.startsWith(File(suppressedFile).absoluteFile)
        }
    }

    /**
     * A [PreMergeDocumentableTransformer] can safely assume that documentables are not merged and therefore
     * only belong to a single source set
     */
    private val Documentable.sourceSet: DokkaSourceSet get() = sourceSets.single()

    private val Documentable.perPackageOptions: DokkaConfiguration.PackageOptions?
        get() {
            val packageName = dri.packageName ?: return null
            return sourceSet.perPackageOptions
                .sortedByDescending { packageOptions -> packageOptions.prefix.length }
                .firstOrNull { packageOptions -> packageName.startsWith(packageOptions.prefix) }
        }

    private val <T> T.source: DocumentableSource where T : Documentable, T : WithExpectActual
        get() = checkNotNull(sources[sourceSet])

}
