package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

class EmptyModulesFilterTransformer : PreMergeDocumentableTransformer {
    override fun invoke(modules: List<DModule>): List<DModule> {
        return modules.filter { it.children.isNotEmpty() }
    }
}