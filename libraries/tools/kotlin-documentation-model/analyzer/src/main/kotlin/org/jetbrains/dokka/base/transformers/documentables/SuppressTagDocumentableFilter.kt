/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.Suppress
import org.jetbrains.dokka.plugability.DokkaContext

public class SuppressTagDocumentableFilter(
    public val dokkaContext: DokkaContext
) : SuppressedByConditionDocumentableFilterTransformer(dokkaContext) {
    override fun shouldBeSuppressed(d: Documentable): Boolean =
        d.documentation.any { (_, docs) -> docs.dfs { it is Suppress } != null }
}
