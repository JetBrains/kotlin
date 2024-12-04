/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.*

/**
 * This class provides methods to generate Objective-C RTTI and other data.
 * It is mostly based on `clang/lib/CodeGen/CGObjCMac.cpp`, and supports only subset of operations
 * required for our purposes (thus simplified).
 *
 * [finishModule] must be called exactly once after all required data was generated.
 */
internal class ObjCDataGenerator(val codegen: CodeGenerator) {

    val context = codegen.context
    val llvm = codegen.llvm

    fun finishModule() {
        addModuleClassList(
                definedClasses,
                "OBJC_LABEL_CLASS_$",
                "__DATA,__objc_classlist,regular,no_dead_strip"
        )
    }

    private val selectorRefs = mutableMapOf<String, ConstPointer>()
    private val classRefs = mutableMapOf<String, ConstPointer>()

    fun genSelectorRef(selector: String): ConstPointer = selectorRefs.getOrPut(selector) {
        val literal = selectors.get(selector)
        val global = codegen.staticData.placeGlobal("OBJC_SELECTOR_REFERENCES_", literal)
        global.setLinkage(LLVMLinkage.LLVMPrivateLinkage)
        LLVMSetExternallyInitialized(global.llvmGlobal, 1)
        global.setAlignment(codegen.runtime.pointerAlignment)
        global.setSection("__DATA,__objc_selrefs,literal_pointers,no_dead_strip")

        llvm.compilerUsedGlobals += global.llvmGlobal

        global.pointer
    }

    fun genClassRef(name: String): ConstPointer = classRefs.getOrPut(name) {
        val classGlobal = getClassGlobal(name, isMetaclass = false)
        val global = codegen.staticData.placeGlobal("OBJC_CLASSLIST_REFERENCES_\$_", classGlobal).also {
            it.setLinkage(LLVMLinkage.LLVMPrivateLinkage)
            it.setSection("__DATA,__objc_classrefs,regular,no_dead_strip")
            it.setAlignment(codegen.runtime.pointerAlignment)
        }

        llvm.compilerUsedGlobals += global.pointer.llvm

        global.pointer.bitcast(pointerType(llvm.int8PtrType))
    }

    private val classObjectType = codegen.runtime.objCClassObjectType

    fun exportClass(name: String) {
        llvm.usedGlobals += getClassGlobal(name, isMetaclass = false).llvm
        llvm.usedGlobals += getClassGlobal(name, isMetaclass = true).llvm
    }

    private fun getClassGlobal(name: String, isMetaclass: Boolean): ConstPointer {
        val prefix = if (isMetaclass) {
            "OBJC_METACLASS_\$_"
        } else {
            "OBJC_CLASS_\$_"
        }

        val globalName = prefix + name

        // TODO: refactor usages and use [Global] class.
        val llvmGlobal = LLVMGetNamedGlobal(llvm.module, globalName)
                ?: codegen.importObjCGlobal(globalName, classObjectType)

        return constPointer(llvmGlobal)
    }

    private val emptyCache = constPointer(
            codegen.importObjCGlobal("_objc_empty_cache", codegen.runtime.objCCache)
    )

    fun emitEmptyClass(name: String, superName: String) {
        emitClass(name, superName, instanceMethods = emptyList())
    }

    class Method(val selector: String, val encoding: String, val imp: ConstPointer)

    fun emitClass(name: String, superName: String, instanceMethods: List<Method>) {
        val runtime = llvm.runtime

        val classRoType = runtime.objCClassRoType
        val methodType = runtime.objCMethodType
        val methodListType = runtime.objCMethodListType
        val protocolListType = runtime.objCProtocolListType
        val ivarListType = runtime.objCIVarListType
        val propListType = runtime.objCPropListType

        val classNameLiteral = classNames.get(name)

        fun emitInstanceMethodList(): ConstPointer {
            if (instanceMethods.isEmpty()) return NullPointer(methodListType)

            val methodStructs = instanceMethods.map {
                Struct(methodType, selectors.get(it.selector), encodings.get(it.encoding), it.imp.bitcast(llvm.int8PtrType))
            }

            val methodList = llvm.struct(
                    llvm.constInt32(LLVMABISizeOfType(codegen.llvmTargetData, methodType).toInt()),
                    llvm.constInt32(instanceMethods.size),
                    ConstArray(methodType, methodStructs)
            )

            val globalName = "\u0001l_OBJC_\$_INSTANCE_METHODS_$name"
            val global = codegen.staticData.placeGlobal(globalName, methodList).also {
                it.setLinkage(LLVMLinkage.LLVMPrivateLinkage)
                it.setAlignment(runtime.pointerAlignment)
                it.setSection("__DATA, __objc_const")
            }

            llvm.compilerUsedGlobals += global.llvmGlobal

            return global.pointer.bitcast(pointerType(methodListType))
        }

        fun buildClassRo(isMetaclass: Boolean): ConstPointer {
            // TODO: add NonFragileABI_Class_CompiledByARC flag?

            val flags: Int
            val start: Int
            val size: Int
            // TODO: stop using hard-coded values.
            if (isMetaclass) {
                flags = 1
                start = 40
                size = 40
            } else {
                flags = 0
                start = 8
                size = 8
            }

            val fields = mutableListOf<ConstValue>()

            fields += llvm.constInt32(flags)
            fields += llvm.constInt32(start)
            fields += llvm.constInt32(size)
            fields += NullPointer(llvm.int8Type) // ivar layout name
            fields += classNameLiteral
            fields += if (isMetaclass) NullPointer(methodListType) else emitInstanceMethodList()
            fields += NullPointer(protocolListType)
            fields += NullPointer(ivarListType)
            fields += NullPointer(llvm.int8Type) // ivar layout
            fields += NullPointer(propListType)

            val roValue = Struct(classRoType, fields)

            val roLabel = if (isMetaclass) {
                "\u0001l_OBJC_METACLASS_RO_\$_"
            } else {
                "\u0001l_OBJC_CLASS_RO_\$_"
            } + name

            val roGlobal = codegen.staticData.placeGlobal(roLabel, roValue).also {
                it.setLinkage(LLVMLinkage.LLVMPrivateLinkage)
                it.setAlignment(runtime.pointerAlignment)
                it.setSection("__DATA, __objc_const")
            }

            return roGlobal.pointer
        }

        fun buildClassObject(
                isMetaclass: Boolean,
                isa: ConstPointer,
                superClass: ConstPointer,
                classRo: ConstPointer
        ): ConstPointer {
            val fields = mutableListOf<ConstValue>()

            fields += isa
            fields += superClass
            fields += emptyCache
            val vtableEntryType = pointerType(functionType(llvm.int8PtrType, false, llvm.int8PtrType, llvm.int8PtrType))
            fields += NullPointer(vtableEntryType) // empty vtable
            fields += classRo

            val classObjectValue = Struct(classObjectType, fields)
            val classGlobal = getClassGlobal(name, isMetaclass = isMetaclass)

            LLVMSetInitializer(classGlobal.llvm, classObjectValue.llvm)
            LLVMSetSection(classGlobal.llvm, "__DATA, __objc_data")
            LLVMSetAlignment(classGlobal.llvm, LLVMABIAlignmentOfType(runtime.targetData, classObjectType))

            llvm.usedGlobals.add(classGlobal.llvm)

            return classGlobal
        }

        val metaclassObject = buildClassObject(
                isMetaclass = true,
                isa = getClassGlobal("NSObject", isMetaclass = true),
                superClass = getClassGlobal(superName, isMetaclass = true),
                classRo = buildClassRo(isMetaclass = true)
        )

        val classObject = buildClassObject(
                isMetaclass = false,
                isa = metaclassObject,
                superClass = getClassGlobal(superName, isMetaclass = false),
                classRo = buildClassRo(isMetaclass = false)
        )

        definedClasses.add(classObject)
    }

    private val definedClasses = mutableListOf<ConstPointer>()

    private fun addModuleClassList(elements: List<ConstPointer>, name: String, section: String) {
        if (elements.isEmpty()) return

        val global = codegen.staticData.placeGlobalArray(
                name,
                llvm.int8PtrType,
                elements.map { it.bitcast(llvm.int8PtrType) }
        )

        global.setAlignment(
                LLVMABIAlignmentOfType(
                        llvm.runtime.targetData,
                        LLVMGetInitializer(global.llvmGlobal)!!.type
                )
        )

        global.setSection(section)

        llvm.compilerUsedGlobals += global.llvmGlobal
    }

    private val classNames = CStringLiteralsTable(classNameGenerator)

    private val selectors = CStringLiteralsTable(selectorGenerator)

    private val encodings = CStringLiteralsTable(encodingGenerator)

    private inner class CStringLiteralsTable(val generator: CStringLiteralsGenerator) {

        private val literals = mutableMapOf<String, ConstPointer>()

        fun get(value: String) = literals.getOrPut(value) {
            val globalPointer = generator.generate(llvm.module, llvm, value)
            llvm.compilerUsedGlobals += globalPointer.llvm
            globalPointer.bitcast(llvm.int8PtrType)
        }
    }

    companion object {
        val classNameGenerator =
                CStringLiteralsGenerator("OBJC_CLASS_NAME_", "__TEXT,__objc_classname,cstring_literals")

        val selectorGenerator =
                CStringLiteralsGenerator("OBJC_METH_VAR_NAME_",  "__TEXT,__objc_methname,cstring_literals")

        private val encodingGenerator =
                CStringLiteralsGenerator("OBJC_METH_VAR_TYPE_", "__TEXT,__objc_methtype,cstring_literals")
    }

    class CStringLiteralsGenerator(val label: String, val section: String) {
        fun generate(module: LLVMModuleRef, llvm: CodegenLlvmHelpers, value: String): ConstPointer {
            val bytes = value.toByteArray(Charsets.UTF_8).map { llvm.constInt8(it) } + llvm.constInt8(0)
            val initializer = ConstArray(llvm.int8Type, bytes)
            val llvmGlobal = LLVMAddGlobal(module, initializer.llvmType, label)!!
            LLVMSetInitializer(llvmGlobal, initializer.llvm)

            LLVMSetGlobalConstant(llvmGlobal, 1)
            LLVMSetLinkage(llvmGlobal, LLVMLinkage.LLVMPrivateLinkage)
            LLVMSetSection(llvmGlobal, section)
            LLVMSetUnnamedAddr(llvmGlobal, 1)
            LLVMSetAlignment(llvmGlobal, 1)

            return constPointer(llvmGlobal)
        }
    }
}
