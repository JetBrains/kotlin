/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedType
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedType.Primitive
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedTypeParameter
import org.jetbrains.kotlin.js.util.NameTable

internal typealias TypeParameterScope = Map<KaTypeParameterSymbol, ExportedTypeParameter>

/**
 * We need to keep track of the type parameters that are currently in scope in order to correctly generate type parameters for inner
 * classes.
 *
 * In TypeScript, there is no notion of inner classes, so each type parameter that a Kotlin inner class captures from its outer class
 * (or classes) has to be declared explicitly in its TypeScript counterpart.
 *
 * For example, for the following Kotlin code:
 * ```kotlin
 * class Outer<T> {
 *   inner class Inner<S>
 * }
 * ```
 *
 * we need to generate the following TS code:
 * ```typescript
 * class Outer<T> {
 *   get Inner(): {
 *      new<S>(): Outer.Inner<S, T>;
 *   }
 * }
 * namespace Outer {
 *   class Inner<S, T$Outer> {
 *     // ...
 *   }
 * }
 * ```
 *
 * The explicit type parameters of the outer classes go immediately after the inner class's own type parameters.
 * The relative order of type parameters of each class is preserved.
 *
 * Note that we rename the captured type parameters by appending the names of its original parents separated with `$`.
 * This is done for clarity and to avoid name clashes.
 */
@OptIn(KaExperimentalApi::class)
context(_: KaSession)
internal fun TypeParameterScope(
    container: KaDeclarationSymbol,
    config: TypeScriptExportConfig,
    outerScope: TypeParameterScope = emptyMap(),
    renameOuterTypeParameters: Boolean = false,
): TypeParameterScope {
    val newTypeParameters = container.typeParameters
    if (!renameOuterTypeParameters && newTypeParameters.isEmpty()) return outerScope
    val nameTable = NameTable<KaTypeParameterSymbol>()
    if (!renameOuterTypeParameters) {
        for ((tp, exported) in outerScope) {
            nameTable.declareStableName(tp, exported.name)
        }
    }
    return buildMap {
        // First, create all the exported type parameters without constraints, because constraints may reference a type parameter
        // that we haven't yet met.
        for (tp in newTypeParameters) {
            this[tp] = ExportedTypeParameter(nameTable.declareFreshName(tp, tp.name.identifier))
        }

        var shouldRecomputeOuterConstraints = false
        if (renameOuterTypeParameters) {
            for ((tp, exported) in outerScope) {
                shouldRecomputeOuterConstraints = true
                val disambiguatedName = tp.parentDeclarationsWithSelf.filterIsInstance<KaNamedSymbol>().joinToString(separator = "\$") {
                    it.getExportedIdentifier()
                }
                this[tp] = exported.copy(name = nameTable.declareFreshName(tp, disambiguatedName))
            }
        } else {
            putAll(outerScope)
        }

        // Then compute the constraints
        var i = 0
        for ((tp, exported) in this) {
            if (!shouldRecomputeOuterConstraints && i == newTypeParameters.size) {
                // Don't compute constraints for type parameters from the `outerScope` map, they should already be computed at this point.
                // Unless we've renamed those type parameters, in which case we have to compute the constraints for them again.
                break
            }
            i += 1
            val constraints = tp.upperBounds
                .mapNotNull {
                    val exportedType = TypeExporter(config, this).exportType(it)
                    if (exportedType is ExportedType.ErrorType) return@mapNotNull null
                    if (exportedType is ExportedType.ImplicitlyExportedType && exportedType.exportedSupertype == Primitive.Any) {
                        exportedType.copy(exportedSupertype = Primitive.Unknown)
                    } else {
                        exportedType
                    }
                }

            exported.constraint = when (constraints.size) {
                0 -> null
                1 -> constraints[0]
                else -> constraints.reduce(ExportedType::IntersectionType)
            }
        }
    }
}