/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

/**
 * SX — Swift Export. Represents both Objective-C and Swift declarations.
 */


interface SXElement {
}


interface SXContainer : SXElement {
    val elements: List<SXElement>
}

interface SXNamespace

class SXClangModule() : SXNamespace, SXContainer {

    private val moduleDependencies: MutableList<SXClangModule> = mutableListOf()

    val headers: MutableList<SXObjCHeader> = mutableListOf()

    override val elements: List<SXElement>
        get() = headers.flatMap(SXObjCHeader::elements)

    fun addObjCHeader(header: SXObjCHeader) {
        headers += header
    }

    fun addDependency(module: SXClangModule) {
        moduleDependencies += module
    }
}

class SXObjCHeader(val name: String) : SXContainer, SXElement {
    override val elements: List<SXElement>
        get() = declarations

    val topLevelDeclarations: List<ObjCTopLevel<*>>
        get() = declarations.toList()

    val classForwardDeclarations = mutableListOf<ObjCClassForwardDeclaration>()
    val protocolForwardDeclarations = mutableListOf<String>()

    val imports = mutableListOf<String>()
    val headerImports = mutableListOf<SXObjCHeader>()

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

    // TODO: Make it smarter, track in [module]
    fun addImport(header: String) {
        imports += header
    }

    fun addImport(header: SXObjCHeader) {
        headerImports += header
    }

    override fun toString(): String {
        return "SXHeader($name)"
    }
}

class SXClangModuleBuilder(
        val kotlinModules: List<ModuleDescriptor>,
        val namer: ObjCExportNamer,
        val headerPerModule: Boolean,
        val umbrellaHeaderName: String,
) {
    private val theModule = SXClangModule()
    private val theHeader = SXObjCHeader(umbrellaHeaderName)
    private val theStdlibHeader = SXObjCHeader("Kotlin.h")

    private val moduleToHeader = mutableMapOf<ModuleDescriptor, SXObjCHeader>()

    init {
        if (headerPerModule) {
            theModule.addObjCHeader(theStdlibHeader)
            kotlinModules.forEach { module ->
                val header = SXObjCHeader(inferHeaderName(module))
                // Conservative, but ok.
                header.addImport(theStdlibHeader)
                theModule.addObjCHeader(header)
                moduleToHeader[module] = header
            }
        } else {
            theModule.addObjCHeader(theHeader)
            kotlinModules.forEach {
                moduleToHeader[it] = theHeader
            }
        }
        val stdlib = kotlinModules.first().allDependencyModules.first { it.isNativeStdlib() }
        moduleToHeader[stdlib] = findHeaderForStdlib()
    }

    private fun inferHeaderName(module: ModuleDescriptor): String {
        return "${module.name.asStringStripSpecialMarkers()}.h"
    }

    fun build(): SXClangModule {
        return theModule
    }

    fun findHeaderForDeclaration(declarationDescriptor: DeclarationDescriptor): SXObjCHeader {
        return moduleToHeader[declarationDescriptor.module]
                ?: error("Failed to find header for ${declarationDescriptor.fqNameSafe}")
    }

    fun findHeaderForStdlib(): SXObjCHeader {
        return if (headerPerModule) theStdlibHeader else theHeader
    }
}