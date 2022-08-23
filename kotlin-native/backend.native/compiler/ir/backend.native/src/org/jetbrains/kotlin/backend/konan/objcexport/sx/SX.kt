/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport.sx

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClassForwardDeclaration
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.module

/**
 * SX — Swift Export. Represents both Objective-C and Swift declarations.
 */
class SXClangModule(val name: String) {

    val moduleDependencies: MutableSet<String> = mutableSetOf()

    val headers: MutableSet<SXObjCHeader> = mutableSetOf()

    fun addObjCHeader(header: SXObjCHeader) {
        headers += header
    }

    fun addDependency(moduleName: String) {
        if (moduleName == name) return
        moduleDependencies += moduleName
    }
}

data class SXHeaderImport(val headerName: String, val local: Boolean) {
    fun constructImportStatement(): String {
        val lBracket = if (local) "\"" else "<"
        val rBracket = if (local) "\"" else ">"
        return "#import $lBracket$headerName$rBracket"
    }
}

class SXObjCHeader(
        val name: String,
        private val moduleDependencyTracker: (String) -> Unit = {}) {
    val topLevelDeclarations: List<ObjCTopLevel<*>>
        get() = declarations.toList()

    val classForwardDeclarations = mutableSetOf<ObjCClassForwardDeclaration>()
    val protocolForwardDeclarations = mutableSetOf<String>()

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

    fun addImport(header: String, moduleName: String? = null) {
        headerImports += SXHeaderImport(header, local = false)
        moduleName?.let(moduleDependencyTracker)
    }

    fun addImport(header: SXObjCHeader, moduleName: String? = null) {
        if (header == this) return
        headerImports += SXHeaderImport(header.name, local = true)
        moduleName?.let(moduleDependencyTracker)
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

    val moduleName: String

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
        override val moduleName: String
) : SXClangModuleBuilder, ModuleBuilderWithStdlib {

    private val theModule: SXClangModule by lazy { SXClangModule(moduleName) }
    private val theHeader: SXObjCHeader by lazy {
        SXObjCHeader("$moduleName.h").also {
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
        private val modules: Set<ModuleDescriptor>,
        override val moduleName: String,
        private val stdlibProvider: () -> ModuleBuilderWithStdlib
) : SXClangModuleBuilder {

    private val theModule: SXClangModule by lazy { SXClangModule(moduleName) }
    private val theHeader: SXObjCHeader by lazy {
        SXObjCHeader("$moduleName.h", theModule::addDependency).also {
            it.addImport(stdlibProvider().getStdlibHeader(), stdlibProvider().moduleName)
            theModule.addObjCHeader(it)
        }
    }

    override fun build(): SXClangModule {
        return theModule
    }

    override fun findHeaderForDeclaration(declarationDescriptor: DeclarationDescriptor): SXObjCHeader? {
        return theHeader.takeIf { declarationDescriptor.module in modules }
    }
}

/**
 * Module builder for an isolated standard library.
 */
class StdlibClangModuleBuilder(
        private val stdlibModule: ModuleDescriptor,
        override val moduleName: String,
) : SXClangModuleBuilder, ModuleBuilderWithStdlib {

    private val theModule: SXClangModule by lazy { SXClangModule(moduleName) }
    private val theHeader: SXObjCHeader by lazy {
        SXObjCHeader("$moduleName.h", theModule::addDependency).also {
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