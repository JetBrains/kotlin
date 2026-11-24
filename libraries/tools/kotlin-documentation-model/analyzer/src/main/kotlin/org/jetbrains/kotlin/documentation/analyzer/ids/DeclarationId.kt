/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.documentation.analyzer.ids

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaRendererAnnotationsFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaRendererTypeApproximator
import org.jetbrains.kotlin.analysis.api.renderer.render
import org.jetbrains.kotlin.analysis.api.renderer.types.KaExpandedTypeRenderingMode
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance
import java.net.URLEncoder
import kotlin.io.encoding.Base64

/**
 * Generates an opaque identifier.
 * Local and unsupported symbols make the function throw.
 *
 * The format depends on the symbol kind:
 * - Package: `[escaped FQ name]`, e.g. `kotlin.text`; the root package is `/` alone.
 * - Classifier: `[escaped ClassId]`, e.g. `kotlin.collections/List`.
 * - Callable: `[containing ClassId or package for top-level]#[escaped name]#[signature hash]`,
 *   e.g. `kotlin.text#repeat#<hash>`. The callable [kind][CallableKind] is one of `property`,
 *   `enum-entry`, `function`; constructors are `function`, while Java fields and synthetic Java
 *   properties are `property`. The kind participates in the ID only through the signature hash.
 *   Property accessors have no ID.
 *
 * For callables, when [hashSignature] is true (default), the callable part of the signature is hashed with
 * [FNV-1a 64](https://www.rfc-editor.org/info/rfc9923) and encoded as unpadded URL-safe Base64.
 * When false, the readable signature is kept instead,
 * e.g. `kotlin.text#repeat#kind=function; receiver=kotlin/String; parameters=[kotlin/Int];`
 * for `fun String.repeat(n: Int): String`. The flag does not affect package and classifier IDs.
 *
 * Return types, ordinary parameter names, annotations, and expect/actual modifiers do not participate
 * in the signature. The exception is callables imported from ObjC — those annotated with
 * `kotlinx.cinterop.ObjCMethod`/`ObjCConstructor`/`ObjCFactory`, or overriding one that is — whose
 * signature additionally carries `objcParameterNames`: their own value parameters' plain Kotlin
 * names, in declaration order.
 *
 * [typeAliasClassId] allows referencing a member through a type alias (for example, AutoCloseable.close).
 * It replaces the member's owner without expanding the alias. The caller must supply the alias's class ID.
 *
 * The [KaSession] context parameter must be the session owning [symbol]. The format and renderer
 * configuration are part of the ID contract; changing them can invalidate previously generated IDs.
 */
context(session: KaSession)
internal fun generateDeclarationId(
    symbol: KaSymbol,
    typeAliasClassId: ClassId? = null,
    hashSignature: Boolean = true,
): String = when (symbol) {
    is KaPackageSymbol -> symbol.fqName.escapePackageId()
    is KaNamedClassSymbol -> {
        symbol.classId?.escape() ?: throw IllegalStateException(
            "Non-local classifier has no ClassId: ${@OptIn(KaExperimentalApi::class) symbol.render()}"
        )
    }
    is KaTypeAliasSymbol -> {
        symbol.classId?.escape() ?: throw IllegalStateException(
            "Non-local type alias has no ClassId: ${@OptIn(KaExperimentalApi::class) symbol.render()}"
        )
    }
    is KaCallableSymbol -> generateCallableId(symbol, typeAliasClassId, hashSignature)
    else -> throw IllegalArgumentException(
        "Unknown symbol kind: $symbol"
    )
}

context(session: KaSession)
private fun generateCallableId(
    symbol: KaCallableSymbol,
    typeAliasClassId: ClassId?,
    hashSignature: Boolean,
): String {
    val kind = callableKind(symbol)
    val prefix = callableIdPrefix(symbol, typeAliasClassId)
    val signature = callableSignature(symbol, kind)
    return "$prefix#${if (hashSignature) signature.shortHash() else signature}"
}

/**
 * The callable kind as it appears in the `kind=` part of the
 * hashed signature.
 */
internal enum class CallableKind(val id: String) {
    PROPERTY("property"),
    ENUM_ENTRY("enum-entry"),
    FUNCTION("function"),
}

context(session: KaSession)
private fun callableKind(symbol: KaCallableSymbol): CallableKind = when (symbol) {
    is KaFunctionSymbol -> FUNCTION
    is KaPropertySymbol, is KaJavaFieldSymbol -> PROPERTY
    is KaEnumEntrySymbol -> ENUM_ENTRY
    is KaBackingFieldSymbol, is KaLocalVariableSymbol, is KaContextParameterSymbol, is KaReceiverParameterSymbol, is KaValueParameterSymbol -> throw IllegalArgumentException(
        "Unknown symbol kind: ${@OptIn(KaExperimentalApi::class) symbol.render()}"
    )
}

context(session: KaSession)
private fun callableIdPrefix(declaration: KaCallableSymbol, typeAliasClassId: ClassId?): String {
    require(typeAliasClassId?.isLocalClassId != true) { "Type alias class ID cannot be local" }

    val classId = when (declaration) {
        is KaConstructorSymbol -> declaration.containingClassId
        else -> declaration.callableId?.classId
    }
    val callableId = declaration.callableId
    if (classId?.isLocalClassId == true || (classId == null && callableId == null)) {
        throw IllegalStateException(
            "Cannot determine the owner of the local or containerless callable: ${@OptIn(KaExperimentalApi::class) declaration.render()}"
        )
    }

    val owner = (typeAliasClassId ?: classId)?.escape() ?: callableId!!.packageName.escape()
    val name = if (declaration is KaConstructorSymbol) CONSTRUCTOR_NAME else callableId!!.callableName.asString()
    return "$owner#${name.escapeName()}"
}

/** Narrows the opt-in for [ClassId.isLocal] (see its KDoc) to this single accessor. */
@OptIn(org.jetbrains.kotlin.name.ClassIdBasedLocality::class)
private val ClassId.isLocalClassId: Boolean
    get() = isLocal

/**
 * A synthetic, stable name for a type parameter, e.g. `T1_0`: the number of declarations enclosing
 * its owner (excluding the file), and the parameter's index among the owner's type parameters.
 * Names are ignored, so renames do not change the ID.
 */
context(session: KaSession)
private fun typeParameterIdName(symbol: KaTypeParameterSymbol): String {
    val owner = symbol.containingSymbol
    val index = (owner as? KaDeclarationSymbol)?.typeParameters?.indexOf(symbol)
        ?: error("Type parameter $symbol has no owning declaration")
    var depth = 0
    var current: KaSymbol? = owner.containingSymbol
    while (current != null && current !is KaFileSymbol) {
        depth++
        current = current.containingSymbol
    }
    return "<T${depth}_$index>"
}

context(session: KaSession)
private fun callableSignature(declaration: KaCallableSymbol, kind: CallableKind): String {

    fun <T> StringBuilder.printList(list: List<T>, name: String, transform: ((T) -> CharSequence)? = null) {
        if (list.isNotEmpty()) {
            append(name)
            append("=")
            list.joinTo(this, prefix = "[", postfix = "]") { transform?.invoke(it) ?: it.toString() }
            append("; ")
        }
    }

    fun render(type: KaType): String =
        @OptIn(KaExperimentalApi::class) type.render(idTypeRenderer, Variance.INVARIANT)

    return buildString {
        append("kind=").append(kind.id).append("; ")

        printList(declaration.typeParameters, "typeParameters") { parameter ->
            val bounds = parameter.upperBounds.map { render(it) }.sorted().joinToString(" & ")
            "${typeParameterIdName(parameter)}: $bounds"
        }
        printList(declaration.contextParameters, "contextParameters") { parameter ->
            render(parameter.returnType)
        }

        declaration.receiverType?.let { append("receiver=").append(render(it)).append("; ") }

        val parameters = (declaration as? KaFunctionSymbol)?.valueParameters.orEmpty()
        printList(parameters, "parameters") { parameter ->
            (if (parameter.isVararg) "vararg " else "") + render(parameter.returnType)
        }

        if ((declaration as? KaNamedFunctionSymbol)?.isSuspend == true) append("suspend; ")
        @OptIn(KaExperimentalApi::class)
        if (declaration.isCompanion) append("companion; ")

        // ObjC overload resolution is selector-based, so two overloads can differ only in parameter
        // names (unlike ordinary Kotlin overloads, where names never affect binary compatibility).
        if (declaration is KaFunctionSymbol && hasObjCSelectorNames(declaration)) {
            printList(parameters, "objcParameterNames") {
                it.name.asString().escapeName()
            }
        }
    }
}

/** Unpadded URL-safe Base64 encoding of the big-endian bytes of the full FNV-1a 64-bit hash. */
private fun String.shortHash(): String {
    val hash = fnv1aHash64()
    val bytes = ByteArray(ULong.SIZE_BYTES) { (hash shr (8 * (Long.SIZE_BYTES - 1 - it))).toByte() }
    return Base64.UrlSafe.encode(bytes).trimEnd('=')
}

/**
 * FNV-1a 64-bit hash of the UTF-8 bytes, as specified in
 * [RFC 9923](https://www.rfc-editor.org/info/rfc9923), with the offset basis and prime taken from
 * its [FNV parameter table](https://www.rfc-editor.org/info/rfc9923/#section-5).
 */
private fun String.fnv1aHash64(): ULong {
    var hash = FNV_64_OFFSET_BASIS
    for (byte in encodeToByteArray()) {
        hash = hash xor (byte.toULong() and 0xffUL)
        hash *= FNV_64_PRIME
    }
    return hash
}

/** The FNV-1a 64-bit offset basis and prime from RFC 9923, §5. */
private const val FNV_64_OFFSET_BASIS = 0xcbf29ce484222325UL
private const val FNV_64_PRIME = 0x100000001b3UL

/**
 * Whether the callable is imported from ObjC: the Kotlin/Native cinterop stub generator marks every
 * Kotlin binding it generates for an Objective-C method, `init`, or factory with one of these
 * annotations (see `ObjCStubs.kt` in the cinterop stub generator). They are the only reliable signal
 * that a callable's parameter names originate from an ObjC selector rather than from Kotlin source.
 *
 * `@kotlin.native.ObjCName` is a different, unrelated annotation (used to customize the name ObjC/Swift
 * consumers see when *exporting* Kotlin declarations, the opposite direction from importing), so it is
 * deliberately not treated as a selector-name signal here.
 */
private fun KaCallableSymbol.isObjCImported(): Boolean = annotations.any { it.classId in objcAnnotations }

private val objcAnnotations = setOf(
    ClassId.topLevel(FqName("kotlinx.cinterop.ObjCMethod")),
    ClassId.topLevel(FqName("kotlinx.cinterop.ObjCConstructor")),
    ClassId.topLevel(FqName("kotlinx.cinterop.ObjCFactory")),
)

/**
 * Whether the callable's ObjC selector names can differ from its Kotlin signature, its own or inherited.
 * A plain Kotlin override of an ObjC-imported member (no cinterop annotation of its own) is still
 * selector-sensitive, because Kotlin lets it rename parameters freely while the selector it implements
 * does not change; see `FirNativeObjcOverrideApplicabilityChecker.isInheritedFromObjc`, which walks the
 * same override chain for the equivalent compiler-side check.
 */
context(session: KaSession)
private fun hasObjCSelectorNames(symbol: KaFunctionSymbol): Boolean =
    symbol.isObjCImported() || symbol.allOverriddenSymbols.any { it.isObjCImported() }

private fun ClassId.escape(): String = packageFqName.escape() + "/" + relativeClassName.escape()

private fun FqName.escape(): String = pathSegments().joinToString(".") { it.asString().escapeName() }

/** The root package's name is empty, so it is identified by the `/` separator alone. */
private fun FqName.escapePackageId(): String = if (isRoot) "/" else escape()

/** Normalize form encoding to preserve percent-encoded spaces and escaped structural separators. */
private fun String.escapeName(): String =
    URLEncoder.encode(this, Charsets.UTF_8.name())
        .replace("+", "%20")
        .replace(".", "%2E")
        .replace("*", "%2A")

@OptIn(KaExperimentalApi::class)
private val idTypeRenderer: KaTypeRenderer =
    KaTypeRendererForSource.WITH_QUALIFIED_NAMES.with {
        expandedTypeRenderingMode = KaExpandedTypeRenderingMode.RENDER_ABBREVIATED_TYPE
        typeApproximator = KaRendererTypeApproximator.NO_APPROXIMATION
        annotationsRenderer = annotationsRenderer.with { annotationFilter = KaRendererAnnotationsFilter.NONE }
        functionalTypeRenderer = KaFunctionalTypeRenderer.AS_CLASS_TYPE_FOR_REFLECTION_TYPES
        flexibleTypeRenderer = KaFlexibleTypeRenderer.AS_RANGE
        // original names of type parameters should be ignored
        typeParameterTypeRenderer = object : KaTypeParameterTypeRenderer {
            override fun renderType(
                analysisSession: KaSession,
                type: KaTypeParameterType,
                typeRenderer: KaTypeRenderer,
                printer: PrettyPrinter,
            ) {
                with(analysisSession) {
                    printer.append(typeParameterIdName(type.symbol)) // e.g. renders as <T0_0>
                    if (type.isMarkedNullable) printer.append('?')
                }
            }
        }
        /**
         * Like [KaClassTypeQualifierRenderer.WITH_QUALIFIED_NAMES], but tailored to the ID format:
         * the package is percent-encoded and separated from class qualifiers by `/`, nested
         * qualifiers keep `.` as separator, and every name is [encoded][escapeName].
         *
         * For `kotlin.collections.List<T>`, the stock `WITH_QUALIFIED_NAMES` renders
         * `kotlin.collections.List<T>`. This renderer produces
         * `kotlin.collections/List<T>`, which is unambiguous.
         */
        classIdRenderer = object : KaClassTypeQualifierRenderer {
            override fun renderClassTypeQualifier(
                analysisSession: KaSession,
                type: KaType,
                qualifiers: List<KaClassTypeQualifier>,
                typeRenderer: KaTypeRenderer,
                printer: PrettyPrinter,
            ) {
                // TODO KT-90413 KDM: Handling of unresolved references
                val classType = type as? KaClassType ?: return
                printer.append(classType.classId.packageFqName.escape()).append('/')
                printer.printCollection(qualifiers, separator = ".") { qualifier ->
                    append(qualifier.name.asString().escapeName())
                    printCollectionIfNotEmpty(qualifier.typeArguments, prefix = "<", postfix = ">") {
                        typeRenderer.typeProjectionRenderer.renderTypeProjection(analysisSession, it, typeRenderer, this)
                    }
                }
            }
        }
    }


private const val CONSTRUCTOR_NAME = ""
