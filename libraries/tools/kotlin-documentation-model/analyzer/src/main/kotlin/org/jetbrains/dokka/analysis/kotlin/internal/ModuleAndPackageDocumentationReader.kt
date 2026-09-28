/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.DocumentationNode

@InternalDokkaApi
public interface ModuleAndPackageDocumentationReader {
    public fun read(module: DModule): SourceSetDependent<DocumentationNode>
    public fun read(pkg: DPackage): SourceSetDependent<DocumentationNode>
    public fun read(module: DokkaConfiguration.DokkaModuleDescription): DocumentationNode?
}
