/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.isFinalBinary
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.objcexport.NSNumberKind
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer

internal fun linkObjC(context: Context) {
    val config = context.config
    if (!(config.produce.isFinalBinary && config.target.family.isAppleFamily)) return

    val patchBuilder = PatchBuilder(context)
    patchBuilder.addObjCPatches()

    val bitcodeFile = config.objCNativeLibrary
    val parsedModule = parseBitcodeFile(bitcodeFile)

    patchBuilder.buildAndApply(parsedModule)

    val failed = LLVMLinkModules2(context.llvmModule!!, parsedModule)
    if (failed != 0) {
        throw Error("failed to link $bitcodeFile")
    }
}

private class PatchBuilder(val context: Context) {
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

    val objCExportNamer = context.objCExport.namer

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

    addExportedClass(objCExportNamer.kotlinAnyName, "KotlinBase", "refHolder", "permanent")

    addExportedClass(objCExportNamer.mutableSetName, "KotlinMutableSet", "setHolder")
    addExportedClass(objCExportNamer.mutableMapName, "KotlinMutableDictionary", "mapHolder")

    addExportedClass(objCExportNamer.kotlinNumberName, "KotlinNumber")
    NSNumberKind.values().mapNotNull { it.mappedKotlinClassId }.forEach {
        addExportedClass(objCExportNamer.numberBoxName(it), "Kotlin${it.shortClassName}", "value_")
    }
}

private fun PatchBuilder.buildAndApply(llvmModule: LLVMModuleRef) {
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
                if (patch.newValue != value) patchLiteral(global, patch.generator, patch.newValue)
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
        generator: ObjCDataGenerator.CStringLiteralsGenerator,
        newValue: String
) {
    val module = LLVMGetGlobalParent(global)!!

    val newFirstCharPtr = generator.generate(module, newValue).getElementPtr(0).llvm

    generateSequence(LLVMGetFirstUse(global), { LLVMGetNextUse(it) }).forEach { use ->
        val firstCharPtr = LLVMGetUser(use)!!.also {
            require(it.isFirstCharPtr(global)) {
                "Unexpected literal usage: ${llvm2string(it)}"
            }
        }
        LLVMReplaceAllUsesWith(firstCharPtr, newFirstCharPtr)
    }
}

private fun LLVMValueRef.isFirstCharPtr(global: LLVMValueRef): Boolean =
        this.type == int8TypePtr &&
                LLVMIsConstant(this) != 0 && LLVMGetConstOpcode(this) == LLVMOpcode.LLVMGetElementPtr
                && LLVMGetNumOperands(this) == 3
                && LLVMGetOperand(this, 0) == global
                && LLVMGetOperand(this, 1).isZeroConst()
                && LLVMGetOperand(this, 2).isZeroConst()

private fun LLVMValueRef?.isZeroConst(): Boolean =
        this != null && LLVMGetValueKind(this) == LLVMValueKind.LLVMConstantIntValueKind
                && LLVMConstIntGetZExtValue(this) == 0L
