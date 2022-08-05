/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

/**
 * SX — Swift Export. Represents both Objective-C and Swift declarations.
 */



interface SXElement {
}


interface SXContainer : SXElement {
    val elements: List<SXElement>
}

/**
 * Stores additional information about declaration
 */
interface SXProps {
    val props: Map<String, Any?>
}

/**
 * Unique identifier for [SXElement] to find it quickly.
 */
@JvmInline
value class SXId(val hash: Int)

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

class SXObjCHeader : SXContainer, SXElement {
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
}

class SXClangModuleBuilder(
        val kotlinModules: List<ModuleDescriptor>,
        val namer: ObjCExportNamer,
        val headerPerModule: Boolean,
) {
    private val theModule = SXClangModule()
    private val theHeader = SXObjCHeader()

    private val moduleToHeader = mutableMapOf<ModuleDescriptor, SXObjCHeader>()

    init {
        theModule.addObjCHeader(theHeader)
        kotlinModules.forEach {
            moduleToHeader[it] = theHeader
        }
    }

    private fun wireup() {
        // Conservative and redundant, but correct, so a good start.
        moduleToHeader.entries.forEach { (ktModule, header) ->
            ktModule.allDependencyModules.map { dependency ->
                val headerDependency = moduleToHeader[dependency]
                if (headerDependency == null) {
                    // Some unknown header. If it wasn't present in mapping, then
                    // probably we don't need it at all.
                    println("Trying to link unexpected module ${dependency.name}")
                } else {
                    if (header != headerDependency) {
                        header.addImport(headerDependency)
                    }
                }
            }
        }
    }

    fun build(): SXClangModule {
        wireup()
        return theModule
    }

    fun findHeaderForDeclaration(declarationDescriptor: DeclarationDescriptor): SXObjCHeader {
        declarationDescriptor.hashCode()
        return theHeader
    }

    fun findHeaderForStdlib(): SXObjCHeader {
        return theHeader
    }
}

class SXObjCHeaderBuilder(val kotlinModules: List<ModuleDescriptor>)