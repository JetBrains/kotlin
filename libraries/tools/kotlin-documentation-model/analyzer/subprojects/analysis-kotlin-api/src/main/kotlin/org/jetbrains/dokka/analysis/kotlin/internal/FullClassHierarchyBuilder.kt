package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.SourceSetDependent

@InternalDokkaApi
typealias Supertypes = List<DRI>

@InternalDokkaApi
typealias ClassHierarchy = SourceSetDependent<Map<DRI, Supertypes>>

@InternalDokkaApi
interface FullClassHierarchyBuilder {
    suspend fun build(module: DModule): ClassHierarchy
}
