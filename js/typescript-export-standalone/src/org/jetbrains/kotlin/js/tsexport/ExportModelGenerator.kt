/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaContextParameterApi::class)

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.components.containingFile
import org.jetbrains.kotlin.analysis.api.components.klibSourceFileName
import org.jetbrains.kotlin.analysis.api.klib.reader.getAllDeclarations
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.ir.backend.js.tsexport.ErrorDeclaration
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedDeclaration
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedNamespace
import org.jetbrains.kotlin.js.common.safeModuleName
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExport
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExportIgnore
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsImplicitExport
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.compactIfPossible

internal class ExportModelGenerator(
    moduleKind: ModuleKind,
) {
    private val generateNamespacesForPackages = moduleKind != ModuleKind.ES

    context(_: KaSession)
    fun generateExport(library: KaLibraryModule, config: TypeScriptModuleConfig): ProcessedModule {
        // TODO: Collect implicitly exported declarations, see ImplicitlyExportedDeclarationsMarkingLowering
        val fileMap = buildMap {
            for (declaration in library.getAllDeclarations()) {
                val packageFqName = when (declaration) {
                    is KaClassLikeSymbol -> declaration.classId!!.packageFqName
                    is KaCallableSymbol -> declaration.callableId!!.packageName
                    else -> error("Unexpected declaration kind: $declaration")
                }

                // TODO(KT-82224): Respect @JsFileName
                @OptIn(KaNonPublicApi::class)
                val fileName = declaration.klibSourceFileName ?: continue

                val key = FileArtifactKey(packageFqName, fileName)
                computeIfAbsent(key) { _ -> mutableListOf() }.addIfNotNull(
                    exportTopLevelDeclaration(declaration)
                )
            }
        }

        return ProcessedModule(
            library,
            fileMap.mapValues { (key, exports) ->
                when {
                    exports.isEmpty() -> emptyList()
                    !generateNamespacesForPackages || key.packageFqName.isRoot -> exports.compactIfPossible()
                    else -> listOf(ExportedNamespace(key.packageFqName.asString(), exports.compactIfPossible()))
                }
            },
            jsOutputName = config.outputName ?: library.libraryName.safeModuleName,
        )
    }

    context(_: KaSession)
    private fun exportTopLevelDeclaration(declaration: KaDeclarationSymbol): ExportedDeclaration? {
        // FIXME(KT-82224): `containingFile` is always null for declarations deserialized from KLIBs
        val isWholeFileExported = declaration.containingFile?.isJsExport() ?: false
        if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(declaration, isWholeFileExported)) return null

        return when (declaration) {
            is KaNamedFunctionSymbol -> ErrorDeclaration("Top level function declarations are not implemented yet")
            is KaPropertySymbol -> ErrorDeclaration("Top level property declarations are not implemented yet")
            is KaClassSymbol -> ErrorDeclaration("Class declarations are not implemented yet")
            is KaTypeAliasSymbol -> ErrorDeclaration("Type alias declarations are not implemented yet")
            else -> null
        }
    }
}

private fun KaAnnotated.isJsImplicitExport(): Boolean =
    annotations.contains(JsImplicitExport)

private fun KaAnnotated.isJsExportIgnore(): Boolean =
    annotations.contains(JsExportIgnore)

private fun KaAnnotated.isJsExport(): Boolean =
    annotations.contains(JsExport)

private val KaSymbolVisibility.isPublicApi: Boolean
    get() = this == KaSymbolVisibility.PUBLIC || this == KaSymbolVisibility.PROTECTED

private fun shouldDeclarationBeExportedImplicitlyOrExplicitly(
    declaration: KaDeclarationSymbol,
    parentIsExported: Boolean,
): Boolean = declaration.isJsImplicitExport() || shouldDeclarationBeExported(declaration, parentIsExported)

private fun shouldDeclarationBeExported(
    declaration: KaDeclarationSymbol,
    parentIsExported: Boolean,
): Boolean {
    if (declaration.isExpect || declaration.isJsExportIgnore() || !declaration.visibility.isPublicApi) {
        return false
    }
    if (declaration.isJsExport()) {
        return true
    }

    return parentIsExported
}
