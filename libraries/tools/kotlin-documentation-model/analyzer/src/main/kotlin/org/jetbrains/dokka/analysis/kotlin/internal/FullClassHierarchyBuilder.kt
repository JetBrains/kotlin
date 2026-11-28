/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.kotlin.documentation.KdElementId

@InternalDokkaApi
internal typealias Supertypes = List<KdElementId>

@InternalDokkaApi
internal typealias ClassHierarchy = SourceSetDependent<Map<KdElementId, Supertypes>>

@InternalDokkaApi
internal interface FullClassHierarchyBuilder {
    suspend fun build(module: DModule): ClassHierarchy
}
