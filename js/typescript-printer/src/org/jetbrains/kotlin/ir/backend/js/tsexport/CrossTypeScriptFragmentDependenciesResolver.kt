/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

import org.jetbrains.kotlin.name.ClassId

public data class TypeScriptFragmentHeader(
    val modulePath: String,
    val importedTypes: Map<ClassId, String>,
    val exportedTypes: Map<ClassId, String>,
    var associatedFragment: TypeScriptDefinitionsFragment? = null
)

public data class CrossFragmentTypeImport(
    val importedAs: String,
    val exportedAs: String,
    val sourcePath: String,
)

public data class CrossFragmentReferences(val imports: List<CrossFragmentTypeImport>) {
    public companion object {
        public fun empty(): CrossFragmentReferences = CrossFragmentReferences(emptyList())
    }
}

public class CrossTypeScriptFragmentDependenciesResolver(private val headers: List<TypeScriptFragmentHeader>) {
    public fun resolveCrossFragmentDependencies(): Map<TypeScriptFragmentHeader, CrossFragmentReferences> {
        val headerToBuilder = headers.associateWith { TypeScriptFragmentReferenceBuilder(it) }
        val typeExportedBy = mutableMapOf<ClassId, TypeScriptFragmentReferenceBuilder>()

        for (header in headers) {
            val builder = headerToBuilder.getValue(header)
            for ((typeId, exportName) in header.exportedTypes) {
                typeExportedBy.put(typeId, builder)?.let {
                    error("Duplicate type export: ${typeId.asString()}")
                }
                builder.exports[typeId] = exportName
            }
        }

        for (header in headers) {
            val builder = headerToBuilder.getValue(header)
            for ((typeId, importName) in header.importedTypes) {
                val exportingBuilder = typeExportedBy[typeId]
                    ?: error("Internal error: cannot find export for type '${typeId.asString()}' (name: '$importName')")

                builder.imports[typeId] = CrossFragmentTypeRef(exportingBuilder, typeId, importName)
            }
        }

        return headers.associateWith { headerToBuilder.getValue(it).buildCrossFragmentReferences() }
    }
}

private class CrossFragmentTypeRef(
    val sourceBuilder: TypeScriptFragmentReferenceBuilder,
    val typeId: ClassId,
    val importedAs: String
)

private class TypeScriptFragmentReferenceBuilder(
    val header: TypeScriptFragmentHeader
) {
    val imports = mutableMapOf<ClassId, CrossFragmentTypeRef>()
    val exports = mutableMapOf<ClassId, String>()

    fun buildCrossFragmentReferences(): CrossFragmentReferences {
        val resolvedImports = imports.map { (typeId, ref) ->
            val exportedAs = ref.sourceBuilder.exports[typeId]
                ?: error("Internal error: type ${typeId.asString()} not found in exports of any module")

            CrossFragmentTypeImport(
                exportedAs = exportedAs,
                importedAs = ref.importedAs,
                sourcePath = ref.sourceBuilder.header.modulePath
            )
        }

        return CrossFragmentReferences(imports = resolvedImports)
    }
}