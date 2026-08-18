/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs

internal fun parseModuleAndPackageDocumentation(
    context: ModuleAndPackageDocumentationParsingContext,
    fragment: ModuleAndPackageDocumentationFragment
): ModuleAndPackageDocumentation {
    return ModuleAndPackageDocumentation(
        name = fragment.name,
        classifier = fragment.classifier,
        documentation = context.parse(fragment)
    )
}
