/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

import org.jetbrains.kotlin.js.common.makeValidES5Identifier
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.relativeRequirePathBetweenSources

public fun List<TypeScriptDefinitionsFragment>.merge(): TypeScriptDefinitionsFragment {
    require(isNotEmpty())
    singleOrNull()?.let { return it }

    var metFirst = false
    val raw = StringBuilder()
    val exportedTypes = mutableMapOf<ClassId, String>()
    val importedTypes = mutableMapOf<ClassId, String>()

    forEach { fragment ->
        if (metFirst) raw.append('\n')
        else metFirst = true

        raw.append(fragment.raw)
        exportedTypes.putAll(fragment.exportedTypes)
        importedTypes.putAll(fragment.importedTypes)
    }


    return TypeScriptDefinitionsFragment(raw.toString(), importedTypes, exportedTypes)
}


public fun List<TypeScriptFragmentHeader>.merge(): TypeScriptFragmentHeader {
    require(isNotEmpty())
    singleOrNull()?.let { return it }

    val exportedTypes = mutableMapOf<ClassId, String>()
    val importedTypes = mutableMapOf<ClassId, String>()

    forEach { header ->
        exportedTypes.putAll(header.exportedTypes)
        importedTypes.putAll(header.importedTypes)
    }


    return TypeScriptFragmentHeader(first().moduleName, importedTypes, exportedTypes)
}


public class TypeScriptDefinitionGenerator(private val moduleKind: ModuleKind) {
    public fun generateSingleWrappedTypeScriptDefinitions(
        moduleName: String,
        fragment: TypeScriptDefinitionsFragment,
        references: CrossFragmentReferences?
    ): TypeScriptDefinitions {
        val body = generateWrappedTypeScriptBody(moduleName, moduleKind, fragment.raw, references)
        return TypeScriptDefinitions(body)
    }

    public fun mergeIntoTypeScriptDefinitions(moduleName: String, fragments: List<TypeScriptDefinitionsFragment>): TypeScriptDefinitions =
        generateSingleWrappedTypeScriptDefinitions(moduleName, fragments.merge(), null)

    private fun generateWrappedTypeScriptBody(
        moduleName: String,
        moduleKind: ModuleKind,
        body: String,
        references: CrossFragmentReferences? = null,
    ): String {
        val internalNamespace = """
            type $Nullable<T> = T | null | undefined
            ${moduleKind.intrinsicsPrefix}function $ObjectInheritanceIntrinsic<T>(): T & (abstract new() => any);
        """.trimIndent().prependIndent(moduleKind.initialIndent) + "\n"

        val declarationsDts = internalNamespace + body
        val imports = references?.imports
            ?.groupBy { it.importModuleName }
            ?.map { (importSource, references) -> emitCrossFragmentTypeImport(moduleName, importSource, references) }
            ?.ifNotEmpty { plus("\n") }
            ?.joinToString("\n")
            .orEmpty()

        val namespaceName = makeValidES5Identifier(moduleName, withHash = false)

        return when (moduleKind) {
            ModuleKind.PLAIN -> "declare namespace $namespaceName {\n$declarationsDts\n}\n"
            ModuleKind.AMD, ModuleKind.COMMON_JS, ModuleKind.ES -> imports + declarationsDts
            ModuleKind.UMD -> "$declarationsDts\nexport as namespace $namespaceName;"
        }
    }

    private fun emitCrossFragmentTypeImport(
        currentModule: String,
        importSource: String,
        imports: List<CrossFragmentTypeImport>
    ): String {
        val names = imports.joinToString(", ") {
            if (it.importedAs == it.exportedAs) it.importedAs else "${it.exportedAs} as ${it.importedAs}"
        }
        return "import type { $names } from \"${relativeRequirePathBetweenSources(currentModule, importSource)}${moduleKind.jsExtension}\";"
    }
}
