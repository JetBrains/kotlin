package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs

import org.jetbrains.dokka.DokkaException

internal class IllegalModuleAndPackageDocumentation(
    source: ModuleAndPackageDocumentationSource, message: String
) : DokkaException("[$source] $message")
