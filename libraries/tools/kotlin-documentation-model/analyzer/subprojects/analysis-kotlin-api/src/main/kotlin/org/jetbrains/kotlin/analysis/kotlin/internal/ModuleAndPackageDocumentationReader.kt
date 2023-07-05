package org.jetbrains.kotlin.analysis.kotlin.internal

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.DocumentationNode

@InternalDokkaApi
interface ModuleAndPackageDocumentationReader {
    fun read(module: DModule): SourceSetDependent<DocumentationNode>
    fun read(pkg: DPackage): SourceSetDependent<DocumentationNode>
    fun read(module: DokkaConfiguration.DokkaModuleDescription): DocumentationNode?
}
