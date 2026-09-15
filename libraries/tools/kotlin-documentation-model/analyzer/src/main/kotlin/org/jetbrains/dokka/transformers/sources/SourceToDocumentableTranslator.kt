/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.transformers.sources

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext

public fun interface SourceToDocumentableTranslator {
    public fun invoke(sourceSet: DokkaSourceSet, context: DokkaContext): DModule
}
