/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.backend.konan.KonanPrimitiveType
import org.jetbrains.kotlin.backend.konan.cKeywords
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import kotlin.collections.contains

/*
 * C export implementation consists of three phases:
 *  1. discovery — walk the exported declarations and build the representation-neutral model in this file;
 *  2. LLVM bridges — generate the runtime bridge functions (codegen);
 *  3. C API generation — emit the C header/source text.
 *
 * The model in this file is produced by phase 1 and consumed by phases 2 and 3. Nothing here depends on an actual
 * representation (`KotlinType`/descriptors or IR) beyond [IrSymbol], which is the handle phase 2 needs into codegen.
 *
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
 * Two exported types are equal iff they translate to the same C type.
 * Most of the exported types are translated by CAdapterTypeTranslator from KotlinType or IrType, except for
 * enum entries in IR mode. See `CExportedReferenceTypeByFqName` for details.
 */
internal abstract class CExportedType {
    // The C type of the exported ("public") API, e.g. `int`, `const char*`, `<prefix>_kref_foo_Bar`.
    abstract fun translateType(): String

    // The C type used by the runtime bridge, e.g. `KObjHeader*` for references/strings.
    abstract fun translateTypeBridge(): String

    abstract fun isMappedToVoid(): Boolean
    abstract fun isMappedToString(): Boolean
    abstract fun isMappedToReference(): Boolean

    final override fun equals(other: Any?): Boolean =
            other is CExportedType && other.translateType() == translateType()

    final override fun hashCode(): Int = translateType().hashCode()
}

/**
 * A Kotlin type viewed through the handful of queries C export needs, hiding whether it is backed by a K1
 * `KotlinType` or an `IrType`. This is the single representation seam: the one [CAdapterTypeTranslator] maps a
 * [CExportableType] to its C spellings without knowing which representation produced it.
 */
internal interface CExportableType {
    fun isUnit(): Boolean
    fun isNothing(): Boolean
    fun binaryTypeIsReference(): Boolean

    /** The class this type's binary representation references, or `null` if that binary type is primitive. */
    fun referenceClassOrNull(): CExportableClass?

    /**
     * Unwraps the type's inline-class layers: [eachInlinedClass] is called for each inlined layer and may
     * short-circuit by returning a non-null value; otherwise the terminal is [ifPrimitive] for a primitive or
     * [ifReference] for a reference.
     */
    fun <R : Any> unwrapToPrimitiveOrReference(
            eachInlinedClass: (CExportableClass) -> R?,
            ifPrimitive: (KonanPrimitiveType) -> R,
            ifReference: (CExportableClass) -> R,
    ): R
}

/** The class a [CExportableType] erases to or references, hiding `ClassDescriptor` vs `IrClass`. */
internal interface CExportableClass {
    val classId: ClassId?
    fun isString(): Boolean
    val fqName: String
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
    val generator: CAdapterModelOwner
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
