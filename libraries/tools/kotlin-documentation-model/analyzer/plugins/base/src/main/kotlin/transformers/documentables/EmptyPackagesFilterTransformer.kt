/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.dokka.transformers.documentation.sourceSet

public class EmptyPackagesFilterTransformer(
    public val context: DokkaContext
) : PreMergeDocumentableTransformer {
    override fun invoke(modules: List<DModule>): List<DModule> {
        return modules.mapNotNull(::filterModule)
    }

    private fun filterModule(module: DModule): DModule? {
        val nonEmptyPackages = module.packages.filterNot { pkg ->
            sourceSet(pkg).skipEmptyPackages && pkg.children.isEmpty()
        }

        return when {
            nonEmptyPackages == module.packages -> module
            nonEmptyPackages.isEmpty() -> null
            else -> module.copy(packages = nonEmptyPackages)
        }
    }
}
