/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.Documentable

// TODO [beresnev] isSynthetic could be a property of Documentable
@InternalDokkaApi
public interface SyntheticDocumentableDetector {
    public fun isSynthetic(documentable: Documentable, sourceSet: DokkaConfiguration.DokkaSourceSet): Boolean
}
