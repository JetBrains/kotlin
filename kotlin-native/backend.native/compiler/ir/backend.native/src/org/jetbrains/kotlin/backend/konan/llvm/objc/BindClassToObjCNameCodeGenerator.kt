/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import llvm.LLVMAddGlobal
import llvm.LLVMGetNamedFunction
import llvm.LLVMGetNamedGlobal
import llvm.LLVMLinkage
import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.common.getCompilerMessageLocation
import org.jetbrains.kotlin.backend.konan.NativeBackendDiagnostics
import org.jetbrains.kotlin.backend.konan.isCache
import org.jetbrains.kotlin.backend.konan.isFinalBinary
import org.jetbrains.kotlin.backend.konan.ir.ClassLayoutBuilder
import org.jetbrains.kotlin.backend.konan.ir.annotations.BindClassToObjCName
import org.jetbrains.kotlin.backend.konan.ir.annotations.allBindClassToObjCName
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.ConstPointer
import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface
import org.jetbrains.kotlin.backend.konan.llvm.LlvmFunctionSignature
import org.jetbrains.kotlin.backend.konan.llvm.LlvmRetType
import org.jetbrains.kotlin.backend.konan.llvm.computePrivateTypeInfoSymbolName
import org.jetbrains.kotlin.backend.konan.llvm.computeTypeInfoSymbolName
import org.jetbrains.kotlin.backend.konan.llvm.constPointer
import org.jetbrains.kotlin.backend.konan.llvm.importGlobal
import org.jetbrains.kotlin.backend.konan.llvm.toProto
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.KotlinToObjCMethodAdapter.Companion.KotlinToObjCMethodAdapter
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCTypeAdapter.Companion.ObjCTypeAdapterForBindClassToObjCName
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.WritableTypeInfoOverrideError
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.bindObjCExportTypeAdapterTo
import org.jetbrains.kotlin.backend.konan.serialization.SerializedFileReference
import org.jetbrains.kotlin.backend.konan.serialization.SerializedObjCAdapter
import org.jetbrains.kotlin.backend.konan.serialization.SerializedObjCReverseBridge
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.library.KotlinLibrary

internal fun CodeGenerator.processBindClassToObjCNameAnnotations(file: IrFile) {
    val reverseBridgesByClass = collectReverseBridgeAdapters(file)

    file.allBindClassToObjCName.forEach {
        val layoutBuilder = generationState.context.getLayoutBuilder(it.kotlinClass)
        val isInterface = it.kotlinClass.isInterface
        val vtableSize = if (isInterface) -1 else layoutBuilder.vtableEntries.size
        val itableSize = if (isInterface) layoutBuilder.interfaceVTableEntries.size else 0
        val reverseBridges = reverseBridgesByClass[it.kotlinClass] ?: emptyList()
        val adapter = ObjCTypeAdapterForBindClassToObjCName(
                it.kotlinClass, it.objCName, vtableSize, itableSize, reverseBridges.map { bridge -> KotlinToObjCMethodAdapter(bridge) }
        )
        val typeAdapter = staticData.placeGlobal("", adapter).pointer
        val adaptersMap = if (it.kotlinClass.isInterface) {
            generationState.bindClassToObjCNameInterfaceAdapters
        } else {
            generationState.bindClassToObjCNameClassAdapters
        }
        adaptersMap[it.objCName] = typeAdapter

        // KT-89121: Only the final binary emits the sorted adapter tables in which the runtime looks up the bindings,
        // and the files of a cached library are not compiled into it. So describe the adapter in
        // the cache for it to be rebuilt there, see [emitBindClassToObjCNameAdaptersFromCaches].
        if (generationState.config.produce.isCache)
            generationState.objCAdapters += buildSerializedObjCAdapter(file, it, isInterface, vtableSize, itableSize, reverseBridges)

        try {
            bindObjCExportTypeAdapterTo(it.kotlinClass, typeAdapter)
        } catch (e: WritableTypeInfoOverrideError) {
            val reason = when (e.reason) {
                WritableTypeInfoOverrideError.Reason.NON_OVERRIDABLE -> "class cannot have ObjC class attachments"
                WritableTypeInfoOverrideError.Reason.ALREADY_OVERRIDDEN -> "another ObjC class is already bound"
            }
            context.diagnosticReporter.report(
                    NativeBackendDiagnostics.NATIVE_BACKEND_ERROR,
                    "Cannot bind ObjC class `${it.objCName}` to ${it.kotlinClass.kotlinFqName}: $reason",
                    it.annotationElement.getCompilerMessageLocation(file)
            )
        }
    }
}

private fun buildSerializedObjCAdapter(
        file: IrFile,
        binding: BindClassToObjCName,
        isInterface: Boolean,
        vtableSize: Int,
        itableSize: Int,
        reverseBridges: List<ReverseBridgeAdapter>,
): SerializedObjCAdapter {
    val kotlinClass = binding.kotlinClass
    // The very name the type info of the class is emitted under, see `IrClass.typeInfoPtr`.
    val typeInfoSymbolName = if (KonanBinaryInterface.isExported(kotlinClass)) {
        kotlinClass.computeTypeInfoSymbolName()
    } else {
        kotlinClass.computePrivateTypeInfoSymbolName(kotlinClass.file.path)
    }
    return SerializedObjCAdapter(
            file = SerializedFileReference(file),
            objCName = binding.objCName,
            isInterface = isInterface,
            typeInfoSymbolName = typeInfoSymbolName,
            vtableSize = vtableSize,
            itableSize = itableSize,
            reverseBridges = reverseBridges.map { bridge ->
                SerializedObjCReverseBridge(
                        selector = bridge.selector,
                        impl = bridge.kotlinImpl.name
                                ?: error("No symbol name for the bridge of `${bridge.selector}` of ${binding.objCName}"),
                        interfaceId = bridge.itablePlace.interfaceId,
                        itableSize = bridge.itablePlace.itableSize,
                        itableIndex = bridge.itablePlace.methodIndex,
                        vtableIndex = bridge.vtableIndex,
                )
            },
    )
}

/**
 * KT-89121: Rebuild the `@BindClassToObjCName` adapters described by the caches this binary links,
 * so that they make it into `Kotlin_ObjCExport_sorted{Class,Protocol}Adapters` (see `ObjCExportCodeGenerator.emitTypeAdapters`)
 * just like the adapters of the files compiled here.
 */
internal fun CodeGenerator.emitBindClassToObjCNameAdaptersFromCaches() {
    if (!context.config.isFinalBinary) return
    if (!context.config.target.family.isAppleFamily) return

    context.config.librariesWithDependencies().forEach { library ->
        val cache = context.config.cachedLibraries.getLibraryCache(library) ?: return@forEach
        cache.serializedObjCAdapters.forEach { emitObjCAdapterFromCache(library, it) }
    }
}

private fun CodeGenerator.emitObjCAdapterFromCache(
        library: KotlinLibrary,
        serialized: SerializedObjCAdapter,
) {
    val adaptersMap = if (serialized.isInterface) {
        generationState.bindClassToObjCNameInterfaceAdapters
    } else {
        generationState.bindClassToObjCNameClassAdapters
    }
    // A binding compiled into this very binary takes precedence, matching how `emitTypeAdapters` merges them.
    if (serialized.objCName in adaptersMap) return

    // The adapter refers to the bridge functions of the annotated file, so that file has to be linked in.
    // The type info of the bound class needs no marking of its own, even when it belongs to another file or library:
    // the cache build referred to it through `importGlobal` while emitting its own copy of this adapter, which
    // recorded a dependency of the annotated file on the defining one, and the dependencies of a cached file that is
    // linked in are linked in transitively as well (see `DependenciesTracker.CachedBitcodeDependenciesComputer`).
    markCachedFileAsUsed(library, serialized.file)

    val adapter = ObjCTypeAdapterForBindClassToObjCName(
            irClass = null,
            objCName = serialized.objCName,
            vtableSize = serialized.vtableSize,
            itableSize = serialized.itableSize,
            reverseAdapters = serialized.reverseBridges.map {
                KotlinToObjCMethodAdapter(
                        selector = it.selector,
                        itablePlace = ClassLayoutBuilder.InterfaceTablePlace(it.interfaceId, it.itableSize, it.itableIndex),
                        vtableIndex = it.vtableIndex,
                        kotlinImpl = importCachedFunctionAddress(it.impl),
                )
            },
            typeInfo = constPointer(importGlobal(serialized.typeInfoSymbolName, llvm.runtime.typeInfoType))
    )
    adaptersMap[serialized.objCName] = staticData.placeGlobal("", adapter).pointer
}

private fun CodeGenerator.markCachedFileAsUsed(library: KotlinLibrary, fileReference: SerializedFileReference) {
    val moduleDeserializer = context.moduleDeserializerProvider.getDeserializerOrNull(library) ?: return
    val irFile = with(moduleDeserializer) { fileReference.deserializationState.file }
    generationState.dependenciesTracker.add(irFile, onlyBitcode = true)
}

/**
 * A reference to the address of a function of a cache. The address is all that is needed here, so if the function
 * is not declared in this module yet, it is declared with an arbitrary signature.
 */
private fun CodeGenerator.importCachedFunctionAddress(name: String): ConstPointer {
    LLVMGetNamedFunction(llvm.module, name)?.let { return constPointer(it) }
    val signature = LlvmFunctionSignature(LlvmRetType(llvm.voidType, isObjectType = false))
    return addFunctionDefinition(signature.toProto(name, null, LLVMLinkage.LLVMExternalLinkage)).toConstPointer()
}
