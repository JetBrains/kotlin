/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport.sx

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClassForwardDeclaration
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.resolve.descriptorUtil.module

/**
 * SX — Swift Export. Represents both Objective-C and Swift declarations.
 */
class SXClangModule {

    private val moduleDependencies: MutableList<SXClangModule> = mutableListOf()

    val headers: MutableSet<SXObjCHeader> = mutableSetOf()

    fun addObjCHeader(header: SXObjCHeader) {
        headers += header
    }

    fun addDependency(module: SXClangModule) {
        moduleDependencies += module
    }
}

data class SXHeaderImport(val headerName: String, val local: Boolean) {
    fun constructImportStatement(): String {
        val lBracket = if (local) "\"" else "<"
        val rBracket = if (local) "\"" else ">"
        return "#import $lBracket$headerName$rBracket"
    }
}

class SXObjCHeader(val name: String) {
    val topLevelDeclarations: List<ObjCTopLevel<*>>
        get() = declarations.toList()

    val classForwardDeclarations = mutableListOf<ObjCClassForwardDeclaration>()
    val protocolForwardDeclarations = mutableListOf<String>()

    val headerImports = mutableSetOf<SXHeaderImport>()

    private val declarations = mutableListOf<ObjCTopLevel<*>>()

    fun addClassForwardDeclaration(fd: ObjCClassForwardDeclaration) {
        classForwardDeclarations += fd
    }

    fun addProtocolForwardDeclaration(fd: String) {
        protocolForwardDeclarations += fd
    }

    fun addTopLevelDeclaration(stub: ObjCTopLevel<*>) {
        declarations += stub
    }

    fun hasDeclarationWithName(name: String): Boolean =
            declarations.find { it.name == name } != null

    fun addImport(header: String) {
        headerImports += SXHeaderImport(header, local = false)
    }

    fun addImport(header: SXObjCHeader) {
        if (header == this) return
        headerImports += SXHeaderImport(header.name, local = true)
    }

    override fun toString(): String {
        return "SXHeader($name)"
    }
}

/**
 * Abstact clang module building facility.
 *
 * Possible cases:
 * - Whole world, single header
 * - Whole world, header per module
 * - Standard library, single header
 * - Multiple modules, single header
 * - Multiple modules, header per module (is it really possible?)
 * - Single module, single header (~ stdlib)
 * - Single module, multiple headers (really? E.g. per class separation?)
 */
sealed interface SXClangModuleBuilder {
    /**
     * Finalize all building procedures and generate [SXClangModule]
     */
    fun build(): SXClangModule

    /**
     * Returns [SXObjCHeader] that should contain ObjC representation of [declarationDescriptor]
     * Returns null if there is no such header in the module.
     */
    fun findHeaderForDeclaration(declarationDescriptor: DeclarationDescriptor): SXObjCHeader?
}

/**
 * Module builder for a whole-world.
 */
class SingleHeaderWholeWorldClangModuleBuilder(
        private val stdlibHeaderName: String
) : SXClangModuleBuilder, ModuleBuilderWithStdlib {

    private val theModule: SXClangModule by lazy { SXClangModule() }
    private val theHeader: SXObjCHeader by lazy {
        SXObjCHeader(stdlibHeaderName).also {
            theModule.addObjCHeader(it)
        }
    }

    override fun getStdlibHeader(): SXObjCHeader {
        return theHeader
    }

    override fun build(): SXClangModule {
        return theModule
    }

    override fun findHeaderForDeclaration(declarationDescriptor: DeclarationDescriptor): SXObjCHeader? {
        return theHeader
    }
}

interface ModuleBuilderWithStdlib : SXClangModuleBuilder {
    fun getStdlibHeader(): SXObjCHeader
}

/**
 * Module builder for an isolated non-stdlib kotlin modules.
 */
class SimpleClangModuleBuilder(
        private val umbrellaHeaderName: String,
        private val stdlibHeaderProvider: () -> SXObjCHeader
) : SXClangModuleBuilder {

    private val theModule: SXClangModule by lazy { SXClangModule() }
    private val theHeader: SXObjCHeader by lazy {
        SXObjCHeader(umbrellaHeaderName).also {
            it.addImport(stdlibHeaderProvider())
            theModule.addObjCHeader(it)
        }
    }


    override fun build(): SXClangModule {
        return theModule
    }

    override fun findHeaderForDeclaration(declarationDescriptor: DeclarationDescriptor): SXObjCHeader? {
        return theHeader
    }
}

/**
 * Module builder for an isolated standard library.
 */
class StdlibClangModuleBuilder(
        private val stdlibModule: ModuleDescriptor,
        private val stdlibHeaderName: String,
) : SXClangModuleBuilder, ModuleBuilderWithStdlib {

    private val theModule: SXClangModule by lazy { SXClangModule() }
    private val theHeader: SXObjCHeader by lazy {
        SXObjCHeader(stdlibHeaderName).also {
            theModule.addObjCHeader(it)
        }
    }

    override fun getStdlibHeader(): SXObjCHeader {
        return theHeader
    }

    override fun build(): SXClangModule {
        return theModule
    }

    override fun findHeaderForDeclaration(declarationDescriptor: DeclarationDescriptor): SXObjCHeader? =
            theHeader.takeIf { declarationDescriptor.module == stdlibModule }
}