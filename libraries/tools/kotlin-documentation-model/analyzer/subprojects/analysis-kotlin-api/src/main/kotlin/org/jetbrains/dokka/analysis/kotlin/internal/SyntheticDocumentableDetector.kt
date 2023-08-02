package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.Documentable

// TODO [beresnev] isSynthetic could be a property of Documentable
@InternalDokkaApi
interface SyntheticDocumentableDetector {
    fun isSynthetic(documentable: Documentable, sourceSet: DokkaConfiguration.DokkaSourceSet): Boolean
}
