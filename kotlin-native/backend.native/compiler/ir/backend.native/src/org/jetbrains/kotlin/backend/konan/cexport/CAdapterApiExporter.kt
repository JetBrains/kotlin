/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import java.io.PrintWriter
import java.io.File

/**
 * Third phase of C export:
 *  1. Create a C++ file with runtime bindings
 *  2. Create a header file with API
 *  3. (MinGW only) create EXPORTS def file.
 *
 *  @param headerFile C header file that will be populated with the exported C API.
 *  @param defFile DLL module definition file
 *  @param cppAdapterFile C++ source that will be populated with glue code between K/N runtime and exported API.
 */
internal class CAdapterApiExporter(
        private val elements: CAdapterExportedElements,
        private val headerFile: File,
        private val defFile: File?,
        private val cppAdapterFile: File,
        private val target: KonanTarget,
) {
    private val typeTranslator = elements.typeTranslator
    private val builtIns = elements.typeTranslator.builtIns

    private val prefix = elements.typeTranslator.prefix
    private lateinit var outputStreamWriter: PrintWriter

    // Primitive built-ins and unsigned types
    private val predefinedTypes = listOf(
            builtIns.byteType, builtIns.shortType,
            builtIns.intType, builtIns.longType,
            builtIns.floatType, builtIns.doubleType,
            builtIns.charType, builtIns.booleanType,
            builtIns.unitType
    ) + UnsignedType.values().map {
        // Unfortunately, `context.ir` and `context.irBuiltins` are not initialized, so `context.ir.symbols.ubyte`, etc, are unreachable.
        builtIns.builtInsModule.findClassAcrossModuleDependencies(it.classId)!!.defaultType
    }

    private fun output(string: String, indent: Int = 0) {
        if (indent != 0) outputStreamWriter.print("  " * indent)
        outputStreamWriter.println(string)
    }

    private fun makeElementDefinition(element: ExportedElement, kind: DefinitionKind, indent: Int) {
        when (kind) {
            DefinitionKind.C_HEADER_DECLARATION -> {
                when {
                    element.isTopLevelFunction -> {
                        val (name, declaration) = element.makeTopLevelFunctionString()
                        exportedSymbols += name
                        output(declaration, 0)
                    }
                }
            }

            DefinitionKind.C_HEADER_STRUCT -> {
                when {
                    element.isFunction ->
                        output(element.makeFunctionPointerString(), indent)
                    element.isClass -> {
                        output("${prefix}_KType* (*_type)(void);", indent)
                        if (element.isSingletonObject) {
                            output("${typeTranslator.translateType((element.declaration as ClassDescriptor).defaultType)} (*_instance)();", indent)
                        }
                    }
                    element.isEnumEntry -> {
                        val enumClass = element.declaration.containingDeclaration as ClassDescriptor
                        output("${typeTranslator.translateType(enumClass.defaultType)} (*get)(); /* enum entry for ${element.name}. */", indent)
                    }
                    // TODO: handle properties.
                }
            }

            DefinitionKind.C_SOURCE_DECLARATION -> {
                when {
                    element.isFunction ->
                        output(element.makeFunctionDeclaration(), 0)
                    element.isClass ->
                        output(element.makeClassDeclaration(), 0)
                    element.isEnumEntry ->
                        output(element.makeEnumEntryDeclaration(), 0)
                    // TODO: handle properties.
                }
            }

            DefinitionKind.C_SOURCE_STRUCT -> {
                when {
                    element.isFunction ->
                        output("/* ${element.name} = */ ${element.cnameImpl}, ", indent)
                    element.isClass -> {
                        output("/* Type for ${element.name} = */  ${element.cname}_type, ", indent)
                        if (element.isSingletonObject)
                            output("/* Instance for ${element.name} = */ ${element.cname}_instance_impl, ", indent)
                    }
                    element.isEnumEntry ->
                        output("/* enum entry getter ${element.name} = */  ${element.cname}_impl,", indent)
                    // TODO: handle properties.
                }
            }
        }
    }

    private fun ExportedElementScope.hasNonEmptySubScopes(): Boolean = elements.isNotEmpty() || scopes.any { it.hasNonEmptySubScopes() }

    private fun makeScopeDefinitions(scope: ExportedElementScope, kind: DefinitionKind, indent: Int) {
        if (!scope.hasNonEmptySubScopes())
            return
        if (kind == DefinitionKind.C_HEADER_STRUCT) output("struct {", indent)
        if (kind == DefinitionKind.C_SOURCE_STRUCT) output(".${scope.name} = {", indent)
        scope.scopes.forEach {
            scope.collectInnerScopeName(it)
            makeScopeDefinitions(it, kind, indent + 1)
        }
        scope.elements.forEach { makeElementDefinition(it, kind, indent + 1) }
        if (kind == DefinitionKind.C_HEADER_STRUCT) output("} ${scope.name};", indent)
        if (kind == DefinitionKind.C_SOURCE_STRUCT) output("},", indent)
    }

    private fun defineUsedTypesImpl(scope: ExportedElementScope, set: MutableSet<KotlinType>) {
        scope.elements.forEach {
            it.addUsedTypes(set)
        }
        scope.scopes.forEach {
            defineUsedTypesImpl(it, set)
        }
    }

    private fun defineUsedTypes(scope: ExportedElementScope, indent: Int) {
        val usedTypes = mutableSetOf<KotlinType>()
        defineUsedTypesImpl(scope, usedTypes)
        val usedReferenceTypes = usedTypes.filter { typeTranslator.isMappedToReference(it) }
        // Add nullable primitives, which are used in prototypes of "(*createNullable<PRIMITIVE_TYPE_NAME>)"
        val predefinedNullableTypes: List<KotlinType> = predefinedTypes.map { it.makeNullable() }

        (predefinedNullableTypes + usedReferenceTypes)
                .map { typeTranslator.translateType(it) }
                .toSet()
                .forEach {
                    output("typedef struct {", indent)
                    output("${prefix}_KNativePtr pinned;", indent + 1)
                    output("} $it;", indent)
                }
    }

    private val exportedSymbols = mutableListOf<String>()

    // TODO: Pass temp and output files explicitly and untie from `NativeGenerationState`.
    fun makeGlobalStruct() {
        val top = elements.scopes.first()
        outputStreamWriter = headerFile.printWriter()

        val exportedSymbol = "${prefix}_symbols"
        exportedSymbols += exportedSymbol

        output("#ifndef KONAN_${prefix.uppercase()}_H")
        output("#define KONAN_${prefix.uppercase()}_H")
        // TODO: use namespace for C++ case?
        output("""
    #ifdef __cplusplus
    extern "C" {
    #endif""".trimIndent())
        output("""
    #ifdef __cplusplus
    typedef bool            ${prefix}_KBoolean;
    #else
    typedef _Bool           ${prefix}_KBoolean;
    #endif
    """.trimIndent())
        output("typedef unsigned short     ${prefix}_KChar;")
        output("typedef signed char        ${prefix}_KByte;")
        output("typedef short              ${prefix}_KShort;")
        output("typedef int                ${prefix}_KInt;")
        output("typedef long long          ${prefix}_KLong;")
        output("typedef unsigned char      ${prefix}_KUByte;")
        output("typedef unsigned short     ${prefix}_KUShort;")
        output("typedef unsigned int       ${prefix}_KUInt;")
        output("typedef unsigned long long ${prefix}_KULong;")
        output("typedef float              ${prefix}_KFloat;")
        output("typedef double             ${prefix}_KDouble;")

        val typedef_KVector128 = "typedef float __attribute__ ((__vector_size__ (16))) ${prefix}_KVector128;"
        if (target.family == Family.MINGW) {
            // Separate `output` for each line to ensure Windows EOL (LFCR), otherwise generated file will have inconsistent line ending.
            output("#ifndef _MSC_VER")
            output(typedef_KVector128)
            output("#else")
            output("#include <xmmintrin.h>")
            output("typedef __m128 ${prefix}_KVector128;")
            output("#endif")
        } else {
            output(typedef_KVector128)
        }

        output("typedef void*              ${prefix}_KNativePtr;")
        output("struct ${prefix}_KType;")
        output("typedef struct ${prefix}_KType ${prefix}_KType;")

        output("")
        defineUsedTypes(top, 0)

        output("")
        makeScopeDefinitions(top, DefinitionKind.C_HEADER_DECLARATION, 0)

        output("")
        output("typedef struct {")
        output("/* Service functions. */", 1)
        output("void (*DisposeStablePointer)(${prefix}_KNativePtr ptr);", 1)
        output("void (*DisposeString)(const char* string);", 1)
        output("${prefix}_KBoolean (*IsInstance)(${prefix}_KNativePtr ref, const ${prefix}_KType* type);", 1)
        predefinedTypes.forEach {
            val nullableIt = it.makeNullable()
            val argument = if (!it.isUnit()) typeTranslator.translateType(it) else "void"
            output("${typeTranslator.translateType(nullableIt)} (*${it.createNullableNameForPredefinedType})($argument);", 1)
            if (!it.isUnit())
                output("$argument (*${it.createGetNonNullValueOfPredefinedType})(${typeTranslator.translateType(nullableIt)});", 1)
        }

        output("")
        output("/* User functions. */", 1)
        makeScopeDefinitions(top, DefinitionKind.C_HEADER_STRUCT, 1)
        output("} ${prefix}_ExportedSymbols;")

        output("extern ${prefix}_ExportedSymbols* $exportedSymbol(void);")
        output("""
    #ifdef __cplusplus
    }  /* extern "C" */
    #endif""".trimIndent())

        output("#endif  /* KONAN_${prefix.uppercase()}_H */")

        outputStreamWriter.close()

        outputStreamWriter = cppAdapterFile.printWriter()

        // Include header into C++ source.
        headerFile.forEachLine { it -> output(it) }

        output("""
    |struct KObjHeader;
    |typedef struct KObjHeader KObjHeader;
    |struct KTypeInfo;
    |typedef struct KTypeInfo KTypeInfo;
    |
    |struct FrameOverlay;
    |typedef struct FrameOverlay FrameOverlay;
    |
    |#define RUNTIME_NOTHROW __attribute__((nothrow))
    |
    |#if __has_attribute(retain)
    |#define RUNTIME_EXPORT __attribute__((used,retain))
    |#else
    |#define RUNTIME_EXPORT __attribute__((used))
    |#endif
    |
    |#define RUNTIME_NORETURN __attribute__((noreturn))
    |
    |extern "C" {
    |KObjHeader* Kotlin_CExport_allocInstance(const KTypeInfo*, KObjHeader**) RUNTIME_NOTHROW;
    |KObjHeader* Kotlin_CExport_derefStablePointer(void*, KObjHeader**) RUNTIME_NOTHROW;
    |void* Kotlin_CExport_createStablePointer(KObjHeader*) RUNTIME_NOTHROW;
    |void Kotlin_CExport_disposeStablePointer(${prefix}_KNativePtr) RUNTIME_NOTHROW;
    |${prefix}_KBoolean Kotlin_CExport_isInstance(${prefix}_KNativePtr, const KTypeInfo*) RUNTIME_NOTHROW;
    |void Kotlin_CExport_disposeCString(const char*);
    |${prefix}_KNativePtr Kotlin_CExport_createSingleton(KObjHeader* (instance)(KObjHeader**));
    |KObjHeader* Kotlin_CExport_createKotlinStringFromCString(const char*, KObjHeader**);
    |char* Kotlin_CExport_createCStringFromKotlinString(KObjHeader*);
    |FrameOverlay* Kotlin_CExport_enterBridge();
    |void Kotlin_CExport_exitBridge() RUNTIME_NOTHROW;
    |void Kotlin_CExport_handleBridgeException(FrameOverlay*);
    |void Kotlin_CExport_bridgeAddStackVariable(KObjHeader**);
    |void Kotlin_CExport_bridgeRemoveStackVariable(KObjHeader**);
    |}  // extern "C"
    |
    |class ScopedBridge {
    |public:
    |    ScopedBridge() : frame_(Kotlin_CExport_enterBridge()) {}
    |    ~ScopedBridge() { Kotlin_CExport_exitBridge(); }
    |    ScopedBridge(const ScopedBridge&) = delete;
    |    ScopedBridge(ScopedBridge&&) = delete;
    |    ScopedBridge& operator=(const ScopedBridge&) = delete;
    |    ScopedBridge& operator=(ScopedBridge&&) = delete;
    |
    |    void onException() { Kotlin_CExport_handleBridgeException(frame_); }
    |private:
    |    FrameOverlay* frame_;
    |};
    |
    |struct ${prefix}_FrameOverlay {
    |  ${prefix}_FrameOverlay* previous;
    |  ${prefix}_KInt parameters;
    |  ${prefix}_KInt count;
    |};
    |
    |class KObjHolder {
    |public:
    |  KObjHolder() : obj_(nullptr) {
    |    Kotlin_CExport_bridgeAddStackVariable(frame());
    |  }
    |  ~KObjHolder() {
    |    Kotlin_CExport_bridgeRemoveStackVariable(frame());
    |  }
    |  KObjHeader** slot() { return &obj_; }
    | private:
    |  ${prefix}_FrameOverlay frame_;
    |  KObjHeader* obj_;
    |
    |  KObjHeader** frame() { return reinterpret_cast<KObjHeader**>(&frame_); }
    |};
    |
    |static void DisposeStablePointerImpl(${prefix}_KNativePtr ptr) {
    |  Kotlin_CExport_disposeStablePointer(ptr);
    |}
    |static void DisposeStringImpl(const char* ptr) {
    |  Kotlin_CExport_disposeCString(ptr);
    |}
    |static ${prefix}_KBoolean IsInstanceImpl(${prefix}_KNativePtr ref, const ${prefix}_KType* type) {
    |  return Kotlin_CExport_isInstance(ref, (const KTypeInfo*)type);
    |}
    """.trimMargin())
        predefinedTypes.forEach {
            assert(!it.isNothing())
            val nullableIt = it.makeNullable()
            val needArgument = !it.isUnit()
            val (parameter, maybeComma) = if (needArgument)
                ("${typeTranslator.translateType(it)} value" to ",") else ("" to "")
            val argument = if (needArgument) "value" else ""
            output("extern \"C\" ${prefix}_KNativePtr Kotlin_CExport_box${it.shortNameForPredefinedType}($parameter);")
            output("static ${typeTranslator.translateType(nullableIt)} ${it.createNullableNameForPredefinedType}Impl($parameter) {")
            output("${prefix}_KNativePtr result = Kotlin_CExport_box${it.shortNameForPredefinedType}($argument);", 1)
            output("return ${typeTranslator.translateType(nullableIt)} { .pinned = result };", 1)
            output("}")

            if (!it.isUnit()) {
                output("extern \"C\" ${typeTranslator.translateType(it)} Kotlin_CExport_unbox${it.shortNameForPredefinedType}(${prefix}_KNativePtr);")
                output("static ${typeTranslator.translateType(it)} ${it.createGetNonNullValueOfPredefinedType}Impl(${typeTranslator.translateType(nullableIt)} value) {")
                output("return Kotlin_CExport_unbox${it.shortNameForPredefinedType}(value.pinned);", 1)
                output("}")
            }
        }
        makeScopeDefinitions(top, DefinitionKind.C_SOURCE_DECLARATION, 0)
        output("static ${prefix}_ExportedSymbols __konan_symbols = {")
        output(".DisposeStablePointer = DisposeStablePointerImpl,", 1)
        output(".DisposeString = DisposeStringImpl,", 1)
        output(".IsInstance = IsInstanceImpl,", 1)
        predefinedTypes.forEach {
            output(".${it.createNullableNameForPredefinedType} = ${it.createNullableNameForPredefinedType}Impl,", 1)
            if (!it.isUnit()) {
                output(".${it.createGetNonNullValueOfPredefinedType} = ${it.createGetNonNullValueOfPredefinedType}Impl,", 1)
            }
        }

        makeScopeDefinitions(top, DefinitionKind.C_SOURCE_STRUCT, 1)
        output("};")
        output("RUNTIME_EXPORT ${prefix}_ExportedSymbols* $exportedSymbol(void) { return &__konan_symbols;}")
        outputStreamWriter.close()

        if (defFile != null) {
            outputStreamWriter = defFile.printWriter()
            output("EXPORTS")
            exportedSymbols.forEach { output(it) }
            outputStreamWriter.close()
        }
    }
}

private val KotlinType.createNullableNameForPredefinedType
    get() = "createNullable${this.shortNameForPredefinedType}"

private val KotlinType.createGetNonNullValueOfPredefinedType
    get() = "getNonNullValueOf${this.shortNameForPredefinedType}"

private operator fun String.times(count: Int): String {
    val builder = StringBuilder()
    repeat(count, { builder.append(this) })
    return builder.toString()
}

private val KotlinType.shortNameForPredefinedType
    get() = this.toString().split('.').last()
