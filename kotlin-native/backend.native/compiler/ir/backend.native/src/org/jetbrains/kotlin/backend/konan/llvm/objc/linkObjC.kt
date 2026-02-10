/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.isFinalBinary
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.runtime.RuntimeModule
import org.jetbrains.kotlin.backend.konan.objcexport.NSNumberKind
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer

internal fun patchObjCRuntimeModule(generationState: NativeGenerationState): LLVMModuleRef? {
    val config = generationState.config
    if (!(config.isFinalBinary && config.target.family.isAppleFamily)) return null

    // objCExport may not be initialized yet (e.g., during hot reload split compilation
    // when collecting runtime modules before codegen has run)
    if (!generationState.hasObjCExport()) return null

    val patchBuilder = PatchBuilder(generationState.objCExport.namer)
    patchBuilder.addObjCPatches()

    val bitcodeFile = generationState.runtimeModulesConfig.absolutePathFor(RuntimeModule.OBJC)
    val parsedModule = parseBitcodeFile(generationState, generationState.messageCollector, generationState.llvmContext, bitcodeFile)

    patchBuilder.buildAndApply(parsedModule, generationState)
    return parsedModule
}

/**
 * Creates an LLVM module containing WritableTypeInfo structures with ObjC export converters
 * for special Kotlin classes (String, collections).
 *
 * This is needed for hot reload mode because:
 * 1. stdlib-cache.a is built without ObjCExportCodeGenerator (isFinalBinary=false)
 * 2. Therefore WritableTypeInfo for String, List, etc. are zero-initialized (no converters)
 * 3. The bootstrap module imports TypeInfo from host, which points to these empty WritableTypeInfos
 * 4. This causes String->NSString conversion to fail, creating wrapper classes instead
 *
 * By emitting WritableTypeInfo globals with EXTERNAL linkage in the host module,
 * the linker will pick these over the COMMON linkage zero-initialized ones from stdlib-cache.a.
 *
 * @param generationState The native generation state
 * @return An LLVM module with WritableTypeInfo globals, or null if not applicable
 */
internal fun createObjCExportConvertersModule(generationState: NativeGenerationState): LLVMModuleRef? {
    val config = generationState.config
    if (!config.target.family.isAppleFamily) return null

    val llvmContext = generationState.llvmContext
    val module = LLVMModuleCreateWithNameInContext("objc_export_converters", llvmContext)!!

    // Get the WritableTypeInfo struct type from runtime
    // WritableTypeInfo contains: { TypeInfoObjCExportAddition }
    // TypeInfoObjCExportAddition contains: { convertToRetained (i8*), objCClass (i8*), swiftClass (i8*), typeAdapter (i8*) }
    val int8PtrType = LLVMPointerType(LLVMInt8TypeInContext(llvmContext), 0)!!

    // Create the TypeInfoObjCExportAddition struct type: { i8*, i8*, i8*, i8* }
    val objCExportAdditionType = memScoped {
        val elementTypes = allocArrayOf(int8PtrType, int8PtrType, int8PtrType, int8PtrType)
        LLVMStructTypeInContext(llvmContext, elementTypes, 4, 0)!!
    }

    // Create the WritableTypeInfo struct type: { TypeInfoObjCExportAddition }
    val writableTypeInfoType = memScoped {
        val elementTypes = allocArrayOf(objCExportAdditionType)
        LLVMStructTypeInContext(llvmContext, elementTypes, 1, 0)!!
    }

    // Converter function type: id (*)(ObjHeader*) -> i8* (*)(i8*)
    val converterFuncType = memScoped {
        val paramTypes = allocArrayOf(int8PtrType)
        LLVMFunctionType(int8PtrType, paramTypes, 1, 0)!!
    }

    // Helper to declare an external converter function
    fun declareConverter(name: String): LLVMValueRef {
        return LLVMAddFunction(module, name, converterFuncType)!!
    }

    // Helper to create WritableTypeInfo global with a converter
    fun createWritableTypeInfo(symbolName: String, converter: LLVMValueRef) {
        // Create the struct value: { { converter, null, null, null } }
        val nullPtr = LLVMConstNull(int8PtrType)!!
        val converterPtr = LLVMConstBitCast(converter, int8PtrType)!!

        val objCExportAddition = memScoped {
            val values = allocArrayOf(converterPtr, nullPtr, nullPtr, nullPtr)
            LLVMConstStructInContext(llvmContext, values, 4, 0)!!
        }

        val writableTypeInfo = memScoped {
            val values = allocArrayOf(objCExportAddition)
            LLVMConstStructInContext(llvmContext, values, 1, 0)!!
        }

        // Create the global with external linkage (overrides common linkage from stdlib-cache.a)
        val global = LLVMAddGlobal(module, writableTypeInfoType, symbolName)!!
        LLVMSetInitializer(global, writableTypeInfo)
        LLVMSetLinkage(global, LLVMLinkage.LLVMExternalLinkage)
    }

    // Declare converter functions (these are defined in stdlib-cache.a runtime)
    val stringConverter = declareConverter("Kotlin_ObjCExport_CreateRetainedNSStringFromKString")
    val listConverter = declareConverter("Kotlin_Interop_CreateRetainedNSArrayFromKList")
    val mutableListConverter = declareConverter("Kotlin_Interop_CreateRetainedNSMutableArrayFromKList")
    val setConverter = declareConverter("Kotlin_Interop_CreateRetainedNSSetFromKSet")
    val mutableSetConverter = declareConverter("Kotlin_Interop_CreateRetainedKotlinMutableSetFromKSet")
    val mapConverter = declareConverter("Kotlin_Interop_CreateRetainedNSDictionaryFromKMap")
    val mutableMapConverter = declareConverter("Kotlin_Interop_CreateRetainedKotlinMutableDictionaryFromKMap")

    // Create WritableTypeInfo globals for special classes
    // Symbol names follow the pattern: ktypew:<FQName>
    createWritableTypeInfo("ktypew:kotlin.String", stringConverter)
    createWritableTypeInfo("ktypew:kotlin.collections.List", listConverter)
    createWritableTypeInfo("ktypew:kotlin.collections.MutableList", mutableListConverter)
    createWritableTypeInfo("ktypew:kotlin.collections.Set", setConverter)
    createWritableTypeInfo("ktypew:kotlin.collections.MutableSet", mutableSetConverter)
    createWritableTypeInfo("ktypew:kotlin.collections.Map", mapConverter)
    createWritableTypeInfo("ktypew:kotlin.collections.MutableMap", mutableMapConverter)

    return module
}

private class PatchBuilder(val objCExportNamer: ObjCExportNamer) {
    enum class GlobalKind(val prefix: String) {
        OBJC_CLASS("OBJC_CLASS_\$_"),
        OBJC_METACLASS("OBJC_METACLASS_\$_"),
        OBJC_IVAR("OBJC_IVAR_\$_"),
    }

    data class GlobalPatch(val kind: GlobalKind, val suffix: String, val newSuffix: String) {
        val globalName: String
            get() = "${kind.prefix}$suffix"

        val newGlobalName: String
            get() = "${kind.prefix}$newSuffix"
    }

    data class LiteralPatch(
            val generator: ObjCDataGenerator.CStringLiteralsGenerator,
            val value: String,
            val newValue: String
    )

    val globalPatches = mutableListOf<GlobalPatch>()
    val literalPatches = mutableListOf<LiteralPatch>()

    // Note: exported classes anyway use the same prefix,
    // so using more unique private prefix wouldn't help to prevent any clashes.
    private val privatePrefix = objCExportNamer.topLevelNamePrefix

    fun addProtocolImport(name: String) {
        literalPatches += LiteralPatch(ObjCDataGenerator.classNameGenerator, name, name)
        // So that protocol name literal wouldn't be detected as unhandled class.
    }

    fun addExportedClass(publicName: ObjCExportNamer.ClassOrProtocolName, runtimeName: String, vararg ivars: String) {
        addRenameClass(runtimeName, publicName.binaryName, ivars)
    }

    fun addPrivateClass(name: String, vararg ivars: String) {
        addRenameClass(name, "$privatePrefix$name", ivars)
    }

    private fun addRenameClass(oldName: String, newName: String, ivars: Array<out String>)  {
        globalPatches += GlobalPatch(GlobalKind.OBJC_CLASS, oldName, newName)
        globalPatches += GlobalPatch(GlobalKind.OBJC_METACLASS, oldName, newName)

        ivars.mapTo(globalPatches) {
            GlobalPatch(GlobalKind.OBJC_IVAR, "$oldName.$it", "$newName.$it")
        }

        literalPatches += LiteralPatch(ObjCDataGenerator.classNameGenerator, oldName, newName)
    }

    fun addPrivateCategory(name: String) {
        literalPatches += LiteralPatch(ObjCDataGenerator.classNameGenerator, name, "$privatePrefix$name")
    }

    fun addPrivateSelector(name: String) {
        literalPatches += LiteralPatch(ObjCDataGenerator.selectorGenerator, name, "${privatePrefix}_$name")
    }
}

/**
 * Add patches for objc.bc.
 */
private fun PatchBuilder.addObjCPatches() {
    addProtocolImport("NSCopying")

    addPrivateSelector("toKotlin:")
    addPrivateSelector("releaseAsAssociatedObject")

    addPrivateClass("KIteratorAsNSEnumerator", "iteratorHolder")
    addPrivateClass("KListAsNSArray", "listHolder")
    addPrivateClass("KMutableListAsNSMutableArray", "listHolder")
    addPrivateClass("KSetAsNSSet", "setHolder")
    addPrivateClass("KMapAsNSDictionary", "mapHolder")

    addPrivateClass("KotlinObjectHolder", "refHolder")
    addPrivateClass("KotlinObjCWeakReference", "referred")

    addPrivateCategory("NSObjectToKotlin")
    addPrivateCategory("NSStringToKotlin")
    addPrivateCategory("NSNumberToKotlin")
    addPrivateCategory("NSDecimalNumberToKotlin")
    addPrivateCategory("NSArrayToKotlin")
    addPrivateCategory("NSSetToKotlin")
    addPrivateCategory("NSDictionaryToKotlin")
    addPrivateCategory("NSEnumeratorAsAssociatedObject")

    addExportedClass(objCExportNamer.kotlinAnyName, "KotlinBase", "refHolder")

    addExportedClass(objCExportNamer.mutableSetName, "KotlinMutableSet", "setHolder")
    addExportedClass(objCExportNamer.mutableMapName, "KotlinMutableDictionary", "mapHolder")

    addExportedClass(objCExportNamer.kotlinNumberName, "KotlinNumber")
    NSNumberKind.values().mapNotNull { it.mappedKotlinClassId }.forEach {
        addExportedClass(objCExportNamer.numberBoxName(it), "Kotlin${it.shortClassName}", "value_")
    }
}

private fun PatchBuilder.buildAndApply(llvmModule: LLVMModuleRef, state: NativeGenerationState) {
    val nameToGlobalPatch = globalPatches.associateNonRepeatingBy { it.globalName }

    val sectionToValueToLiteralPatch = literalPatches.groupBy { it.generator.section }
            .mapValues { (_, patches) ->
                patches.associateNonRepeatingBy { it.value }
            }

    val unusedPatches = (globalPatches + literalPatches).toMutableSet()

    val globals = generateSequence(LLVMGetFirstGlobal(llvmModule), { LLVMGetNextGlobal(it) }).toList()
    for (global in globals) {
        val initializer = LLVMGetInitializer(global) ?: continue
        val name = LLVMGetValueName(global)?.toKString().orEmpty()

        val globalPatch = nameToGlobalPatch[name]
        if (globalPatch != null) {
            LLVMSetValueName(global, globalPatch.newGlobalName)
            unusedPatches -= globalPatch
        } else if (PatchBuilder.GlobalKind.values().any { name.startsWith(it.prefix) }) {
            error("Objective-C global '$name' is not patched")
        }

        val section = LLVMGetSection(global)?.toKString()
        sectionToValueToLiteralPatch[section]?.let { valueToLiteralPatch ->
            val value = getStringValue(initializer)
            val patch = valueToLiteralPatch[value]
            if (patch != null) {
                if (patch.newValue != value) patchLiteral(global, state, patch.generator, patch.newValue)
                unusedPatches -= patch
            } else if (section == ObjCDataGenerator.classNameGenerator.section) {
                error("Objective-C class name literal is not patched: $value")
            }
        }
    }

    unusedPatches.firstOrNull()?.let {
        error("Patch is not applied: $it")
    }
}

private fun getStringValue(initializer: LLVMValueRef): String? = when (LLVMGetValueKind(initializer)) {
    LLVMValueKind.LLVMConstantDataArrayValueKind -> memScoped {
        require(LLVMIsConstantString(initializer) != 0) { "not a constant string: ${llvm2string(initializer)}" }

        val lengthVar = alloc<size_tVar>()
        val bytePtr = LLVMGetAsString(initializer, lengthVar.ptr)!!
        val length = lengthVar.value

        val lastByte = bytePtr[length - 1]
        require(lastByte == 0.toByte()) {
            "${llvm2string(initializer)}:\n  expected zero terminator, found $lastByte"
        }

        bytePtr.toKString()
    }

    LLVMValueKind.LLVMConstantAggregateZeroValueKind -> ""

    else -> error("Unexpected literal initializer: ${llvm2string(initializer)}")
}

private fun <T, K> List<T>.associateNonRepeatingBy(keySelector: (T) -> K): Map<K, T> =
        this.groupBy(keySelector)
                .mapValues { (key, values) ->
                    values.singleOrNull()
                            ?: error("multiple values found for $key: ${values.joinToString()}")
                }

private fun patchLiteral(
        global: LLVMValueRef,
        state: NativeGenerationState,
        generator: ObjCDataGenerator.CStringLiteralsGenerator,
        newValue: String
) {
    val llvm = state.llvm
    val module = LLVMGetGlobalParent(global)!!
    if (state.config.useLlvmOpaquePointers) {
        val newGlobal = generator.generate(module, state.llvm, newValue).llvm
        LLVMReplaceAllUsesWith(global, newGlobal)
    } else {
        val newFirstCharPtr = generator.generate(module, llvm, newValue).bitcast(llvm.int8PtrType).llvm
        generateSequence(LLVMGetFirstUse(global), { LLVMGetNextUse(it) }).forEach { use ->
            val firstCharPtr = LLVMGetUser(use)!!.also {
                require(it.isFirstCharPtr(llvm, global)) {
                    "Unexpected literal usage: ${llvm2string(it)}"
                }
            }
            LLVMReplaceAllUsesWith(firstCharPtr, newFirstCharPtr)
        }
    }
}

private fun LLVMValueRef.isFirstCharPtr(llvm: CodegenLlvmHelpers, global: LLVMValueRef): Boolean =
        this.type == llvm.int8PtrType &&
                LLVMIsConstant(this) != 0 && LLVMGetConstOpcode(this) == LLVMOpcode.LLVMGetElementPtr
                && LLVMGetNumOperands(this) == 3
                && LLVMGetOperand(this, 0) == global
                && LLVMGetOperand(this, 1).isZeroConst()
                && LLVMGetOperand(this, 2).isZeroConst()

private fun LLVMValueRef?.isZeroConst(): Boolean =
        this != null && LLVMGetValueKind(this) == LLVMValueKind.LLVMConstantIntValueKind
                && LLVMConstIntGetZExtValue(this) == 0L
