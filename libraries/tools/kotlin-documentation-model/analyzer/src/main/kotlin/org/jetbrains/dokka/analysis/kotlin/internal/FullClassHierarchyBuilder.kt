/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.SourceSetDependent

@InternalDokkaApi
public typealias Supertypes = List<DRI>

@InternalDokkaApi
public typealias ClassHierarchy = SourceSetDependent<Map<DRI, Supertypes>>

@InternalDokkaApi
public interface FullClassHierarchyBuilder {
    public suspend fun build(module: DModule): ClassHierarchy
}
