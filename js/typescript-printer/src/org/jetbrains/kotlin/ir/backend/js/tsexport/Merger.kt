/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

import org.jetbrains.kotlin.js.common.makeValidES5Identifier
import org.jetbrains.kotlin.js.config.ModuleKind

public class TypeScriptMerger(private val moduleKind: ModuleKind) {
    public fun mergeIntoSingleFragment(fragments: List<TypeScriptDefinitionsFragment>): TypeScriptDefinitionsFragment {
        require(fragments.isNotEmpty())
        return fragments.singleOrNull() ?: TypeScriptDefinitionsFragment(fragments.joinToString("\n") { it.raw })
    }

    public fun generateSingleWrappedTypeScriptDefinitions(
        moduleName: String,
        fragment: TypeScriptDefinitionsFragment,
    ): TypeScriptDefinitions {
        val body = generateWrappedTypeScriptBody(moduleName, moduleKind, fragment.raw)
        return TypeScriptDefinitions(body)
    }

    public fun mergeIntoTypeScriptDefinitions(
        moduleName: String,
        fragments: List<TypeScriptDefinitionsFragment>,
    ): TypeScriptDefinitions {
        val merged = mergeIntoSingleFragment(fragments)
        return generateSingleWrappedTypeScriptDefinitions(moduleName, merged)
    }

    private fun generateWrappedTypeScriptBody(
        moduleName: String,
        moduleKind: ModuleKind,
        body: String
    ): String {
        val internalNamespace = """
            type $Nullable<T> = T | null | undefined
            ${moduleKind.intrinsicsPrefix}function $ObjectInheritanceIntrinsic<T>(): T & (abstract new() => any);
        """.trimIndent().prependIndent(moduleKind.initialIndent) + "\n"

        val declarationsDts = internalNamespace + body

        val namespaceName = makeValidES5Identifier(moduleName, withHash = false)

        return when (moduleKind) {
            ModuleKind.PLAIN -> "declare namespace $namespaceName {\n$declarationsDts\n}\n"
            ModuleKind.AMD, ModuleKind.COMMON_JS, ModuleKind.ES -> declarationsDts
            ModuleKind.UMD -> "$declarationsDts\nexport as namespace $namespaceName;"
        }
    }
}
