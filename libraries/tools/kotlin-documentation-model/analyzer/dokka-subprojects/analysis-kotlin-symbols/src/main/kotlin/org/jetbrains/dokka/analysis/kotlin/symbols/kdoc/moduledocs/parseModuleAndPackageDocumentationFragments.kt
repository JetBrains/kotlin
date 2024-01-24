/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs

import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentation.Classifier.Module
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentation.Classifier.Package
import java.io.File

internal fun parseModuleAndPackageDocumentationFragments(source: File): List<ModuleAndPackageDocumentationFragment> {
    return parseModuleAndPackageDocumentationFragments(ModuleAndPackageDocumentationFile(source))
}

internal fun parseModuleAndPackageDocumentationFragments(
    source: ModuleAndPackageDocumentationSource
): List<ModuleAndPackageDocumentationFragment> {
    val fragmentStrings = source.documentation.split(Regex("(|^)#\\s*(?=(Module|Package))"))
    return fragmentStrings
        .filter(String::isNotBlank)
        .map { fragmentString -> parseModuleAndPackageDocFragment(source, fragmentString) }
}

private fun parseModuleAndPackageDocFragment(
    source: ModuleAndPackageDocumentationSource,
    fragment: String
): ModuleAndPackageDocumentationFragment {
    val firstLineAndDocumentation = fragment.split("\r\n", "\n", "\r", limit = 2)
    val firstLine = firstLineAndDocumentation[0]

    val classifierAndName = firstLine.split(Regex("\\s+"), limit = 2)

    val classifier = when (classifierAndName[0].trim()) {
        "Module" -> Module
        "Package" -> Package
        else -> throw IllegalStateException(
            """Unexpected classifier: "${classifierAndName[0]}", expected either "Module" or "Package". 
            |For more information consult the specification: https://kotlinlang.org/docs/dokka-module-and-package-docs.html""".trimMargin()
        )
    }

    if (classifierAndName.size != 2 && classifier == Module) {
        throw IllegalModuleAndPackageDocumentation(source, "Missing Module name")
    }

    val name = classifierAndName.getOrNull(1)?.trim().orEmpty()
    if (classifier == Package && name.contains(Regex("\\s"))) {
        throw IllegalModuleAndPackageDocumentation(
            source, "Package name cannot contain whitespace in '$firstLine'"
        )
    }

    return ModuleAndPackageDocumentationFragment(
        name = name,
        classifier = classifier,
        documentation = firstLineAndDocumentation.getOrNull(1)?.trim().orEmpty(),
        source = source
    )
}
