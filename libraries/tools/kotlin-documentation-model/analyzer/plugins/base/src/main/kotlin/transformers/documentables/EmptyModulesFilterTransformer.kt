/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

public class EmptyModulesFilterTransformer : PreMergeDocumentableTransformer {
    override fun invoke(modules: List<DModule>): List<DModule> {
        return modules.filter { it.children.isNotEmpty() }
    }
}
