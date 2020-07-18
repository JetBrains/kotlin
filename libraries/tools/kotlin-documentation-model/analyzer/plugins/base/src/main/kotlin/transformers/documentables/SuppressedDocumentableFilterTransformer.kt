package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.dokka.transformers.documentation.perPackageOptions
import org.jetbrains.dokka.transformers.documentation.source
import org.jetbrains.dokka.transformers.documentation.sourceSet
import java.io.File

class SuppressedDocumentableFilterTransformer(val context: DokkaContext) : PreMergeDocumentableTransformer {
    override fun invoke(modules: List<DModule>): List<DModule> {
        return modules.mapNotNull(::filterModule)
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
        val options = perPackageOptions(pkg)
        if (options?.suppress == true) {
            return null
        }

        val filteredChildren = pkg.children.filterNot(::isSuppressed)
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
        val sourceFile = File(source(documentable).path).absoluteFile
        return sourceSet(documentable).suppressedFiles.any { suppressedFile ->
            sourceFile.startsWith(suppressedFile.absoluteFile)
        }
    }
}
