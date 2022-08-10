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

class SXClangModuleBuilder(
        private val kotlinModules: Set<ModuleDescriptor>,
        private val headerPerModule: Boolean,
        private val umbrellaHeaderName: String,
        val containsStdlib: Boolean,
        private val stdlibHeaderProvider: () -> SXObjCHeader
) {
    private val theModule = SXClangModule()
    private val theHeader = SXObjCHeader(umbrellaHeaderName)
    private val theStdlibHeader: SXObjCHeader? = SXObjCHeader("Kotlin.h")
            .takeIf { containsStdlib && headerPerModule }

    private var umbrellaHeader: SXObjCHeader? = null

    private val moduleToHeader = mutableMapOf<ModuleDescriptor, SXObjCHeader>()

    init {
        addStdlib()
        if (headerPerModule) {
            kotlinModules.forEach { module ->
                val header = addHeaderFor(module)
                moduleToHeader[module] = header
                if (header.name == umbrellaHeaderName) {
                    umbrellaHeader = header
                }
            }
        } else {
            kotlinModules.forEach {
                moduleToHeader[it] = theHeader
            }
            theModule.addObjCHeader(theHeader)
        }
    }

    private fun addStdlib() {
        if (theStdlibHeader == null) return
        val stdlib = if (containsStdlib) {
            kotlinModules.first { it.isNativeStdlib() }
        } else {
            kotlinModules.first().allDependencyModules.first { it.isNativeStdlib() }
        }
        moduleToHeader[stdlib] = findHeaderForStdlib()
        theModule.addObjCHeader(theStdlibHeader)
    }

    private fun addHeaderFor(module: ModuleDescriptor): SXObjCHeader {
        val header = SXObjCHeader(inferHeaderName(module))
        // A bit conservative, but ok for now.
        header.addImport(findHeaderForStdlib())
        theModule.addObjCHeader(header)
        return header
    }

    private fun inferHeaderName(module: ModuleDescriptor): String {
        // TODO: Better normalization
        val normalized = module.name.asStringStripSpecialMarkers()
                .replace('.', '_')
                .replace(':', '_')
        return "$normalized.h"
    }

    fun build(): SXClangModule {
        umbrellaHeader?.let { umbrellaHeader ->
            theModule.headers
                    .filter { it.name != umbrellaHeaderName }
                    .forEach {
                        umbrellaHeader.addImport(it)
                    }
        }
        return theModule
    }

    fun findHeaderForDeclaration(declarationDescriptor: DeclarationDescriptor): SXObjCHeader {
        if (!headerPerModule) return theHeader
        return moduleToHeader.getOrPut(declarationDescriptor.module) {
            addHeaderFor(declarationDescriptor.module)
        }
    }

    fun findHeaderForStdlib(): SXObjCHeader = when {
        !containsStdlib -> stdlibHeaderProvider()
        headerPerModule -> theStdlibHeader!!
        else -> theHeader
    }
}