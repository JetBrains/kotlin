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
