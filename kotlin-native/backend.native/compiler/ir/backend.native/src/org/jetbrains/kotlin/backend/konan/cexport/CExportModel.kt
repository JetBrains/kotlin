/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.backend.konan.cKeywords
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.name.Name
import kotlin.collections.contains

/**
 * The representation-neutral C export model produced by phase 1 and consumed by phase 2 (LLVM bridges) and
 * phase 3 (C API generation). Nothing here depends on an actual representation (`KotlinType`/descriptors or IR)
 * beyond [IrSymbol], which is the handle phase 2 needs into codegen.
 *
 * Each phase-1 mode supplies implementations of the interfaces below: the K1 mode in [CAdapterGenerator]
 * ([ExportedElementK1], [ExportedDeclarationKey] via a descriptor-backed key) and [CAdapterTypeTranslator]
 * ([CExportedTypeK1]). [ExportedElementScope]'s naming is representation-neutral: callers supply a base name and
 * an [ExportedDeclarationKey], and the scope handles clash mangling.
 */

internal enum class ScopeKind {
    TOP,
    CLASS,
    PACKAGE
}

internal enum class ElementKind {
    FUNCTION,
    PROPERTY,
    TYPE
}

internal enum class DefinitionKind {
    C_HEADER_DECLARATION,
    C_HEADER_STRUCT,
    C_SOURCE_DECLARATION,
    C_SOURCE_STRUCT
}

/**
 * Representation-neutral view of an exported type. Phase 3 asks its questions through this interface and never
 * touches the backing `KotlinType`/`IrType`.
 */
internal interface CExportedType {
    // The C type of the exported ("public") API, e.g. `int`, `const char*`, `<prefix>_kref_foo_Bar`.
    fun translateType(): String

    // The C type used by the runtime bridge, e.g. `KObjHeader*` for references/strings.
    fun translateTypeBridge(): String

    fun isMappedToVoid(): Boolean
    fun isMappedToString(): Boolean
    fun isMappedToReference(): Boolean
}

internal data class SignatureElement(val name: String, val type: CExportedType)

/**
 * Identifies the declaration a unique C name is computed for; used as the key of [ExportedElementScope]'s
 * name cache. Implementations must have value-based [equals]/[hashCode] (e.g. a data class wrapping the backing
 * declaration) so repeated look-ups for the same declaration hit the cache.
 */
internal interface ExportedDeclarationKey

/**
 * The per-model naming/indexing state shared by the elements and the C API generation: the exported-name
 * [prefix] and the C-function index counter. Kept minimal so phases 2/3 do not depend on the K1
 * [CAdapterGenerator]; the K1 mode's generator implements it.
 */
internal interface CAdapterModelOwner {
    val prefix: String
    fun nextFunctionIndex(): Int
}

internal sealed interface ExportedElement {
    val kind: ElementKind
    val scope: ExportedElementScope
    val owner: CAdapterModelOwner
    val isFunction: Boolean
    val isTopLevelFunction: Boolean
    val isConstructor: Boolean
    val isClass: Boolean
    val isEnumEntry: Boolean
    val isSingletonObject: Boolean
    val name: String
    var cname: String
    val cnameImpl: String
    val irSymbol: IrSymbol
    val classType: String
    val enumEntryContainingType: String
    fun addUsedTypes(set: MutableSet<CExportedType>)
    fun makeCFunctionSignature(shortName: Boolean): List<SignatureElement>
    fun makeBridgeSignature(): List<String>
}

internal data class CAdapterExportedElements(
        val prefix: String,
        val scopes: MutableList<ExportedElementScope>,
)

internal class ExportedElementScope(val kind: ScopeKind, val name: String) {
    val elements = mutableListOf<ExportedElement>()
    val scopes = mutableListOf<ExportedElementScope>()
    private val scopeNames = mutableSetOf<String>()
    private val uniqueNameCache = mutableMapOf<Pair<ExportedDeclarationKey, Boolean>, String>()

    override fun toString(): String {
        return "$kind: $name ${elements.joinToString(", ")} ${scopes.joinToString("\n")}"
    }

    // collects names of inner scopes to make sure function<->scope name clashes would be detected, and functions would be mangled with "_" suffix
    fun collectInnerScopeName(innerScope: ExportedElementScope) {
        scopeNames += innerScope.name
    }

    /**
     * Returns a scope-unique C name for [declaration]'s [baseName], mangling with a "_" suffix on clashes with
     * sibling scopes, previously assigned names, or C keywords. Idempotent per ([declaration], [shortName]): the
     * same declaration always yields the same name and is registered only once, so callers may invoke it
     * repeatedly.
     *
     * Representation-neutral: the caller computes [baseName] from its backing declaration.
     */
    fun scopeUniqueName(declaration: ExportedDeclarationKey, shortName: Boolean, baseName: String): String {
        uniqueNameCache[declaration to shortName]?.let { return it }
        var computedName = baseName
        while (scopeNames.contains(computedName) || cKeywords.contains(computedName)) {
            computedName += "_"
        }
        scopeNames += computedName
        uniqueNameCache[declaration to shortName] = computedName
        return computedName
    }
}

private val simpleNameMapping = mapOf(
        "<this>" to "thiz",
        "<set-?>" to "set"
)

internal fun translateName(name: Name): String {
    val nameString = name.asString()
    return when {
        simpleNameMapping.contains(nameString) -> simpleNameMapping[nameString]!!
        cKeywords.contains(nameString) -> "${nameString}_"
        name.isSpecial -> nameString.replace("[<> ]".toRegex(), "_")
        else -> nameString
    }
}
