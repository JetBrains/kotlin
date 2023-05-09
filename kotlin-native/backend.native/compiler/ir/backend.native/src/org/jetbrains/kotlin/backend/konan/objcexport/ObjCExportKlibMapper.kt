/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.DeserializedSourceFile
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.resolve.descriptorUtil.module

/**
 * Uniquely identifies each framework.
 */
data class ObjCExportFrameworkId(val name: String)

/**
 * Uniquely identifies each Objective-C header.
 */
data class ObjCExportHeaderId(val frameworkId: ObjCExportFrameworkId, val name: String)

class ObjCExportModuleInfo(
        val module: ModuleDescriptor,
        val exported: Boolean,
)

data class ObjCExportFrameworkStructure(
        val name: String,
        val topLevelPrefix: String,
        val modulesInfo: List<ObjCExportModuleInfo>,
        val headerName: String,
)

data class ObjCExportStructure(
        val frameworks: List<ObjCExportFrameworkStructure>
) {
    fun reverseTopSortFrameworks(config: KonanConfig): List<ObjCExportFrameworkStructure> {
        val frameworkByModule = frameworks.flatMap { framework -> framework.modulesInfo.map { it.module.kotlinLibrary to framework } }.toMap()
        val visitedFrameworks = mutableSetOf<ObjCExportFrameworkStructure>()
        val result = mutableListOf<ObjCExportFrameworkStructure>()
        val sortedLibraries = config.resolvedLibraries.getFullList(TopologicalLibraryOrder)
        sortedLibraries.forEach {
            val frameworkStructure = frameworkByModule[it] ?: return@forEach
            if (frameworkStructure !in visitedFrameworks) {
                visitedFrameworks += frameworkStructure
                result += frameworkStructure
            }
        }
        return result
    }
}

/**
 * Maps [KotlinLibrary] to a framework.
 */
interface ObjCExportFrameworkIdProvider {
    fun getFrameworkId(library: KotlinLibrary): ObjCExportFrameworkId
}

fun ObjCExportFrameworkIdProvider(structure: ObjCExportStructure): ObjCExportFrameworkIdProvider =
        ObjCExportFrameworkIdProviderImpl(structure)

private class ObjCExportFrameworkIdProviderImpl(private val structure: ObjCExportStructure) : ObjCExportFrameworkIdProvider {

    private val cache = mutableMapOf<KotlinLibrary, ObjCExportFrameworkId>()

    override fun getFrameworkId(library: KotlinLibrary): ObjCExportFrameworkId = cache.getOrPut(library) {
        val framework = structure.frameworks.first { it.modulesInfo.any { it.module.kotlinLibrary == library } }
        ObjCExportFrameworkId(framework.name)
    }
}

/**
 * Provides [ObjCExportHeaderId] for each Kotlin declaration
 */
interface ObjCExportHeaderIdProvider {
    fun getHeaderId(declaration: DeclarationDescriptor): ObjCExportHeaderId

    fun getHeaderId(sourceFile: SourceFile): ObjCExportHeaderId

    fun getStdlibHeaderId(): ObjCExportHeaderId
}

fun ObjCExportHeaderIdProvider(
        frameworkProvider: ObjCExportFrameworkIdProvider,
        structure: ObjCExportStructure,
        stdlib: KotlinLibrary,
): ObjCExportHeaderIdProvider = ObjCExportHeaderIdProviderImpl(frameworkProvider, structure, stdlib)

private class ObjCExportHeaderIdProviderImpl(
        private val frameworkProvider: ObjCExportFrameworkIdProvider,
        private val structure: ObjCExportStructure,
        private val stdlib: KotlinLibrary,
) : ObjCExportHeaderIdProvider {

    override fun getHeaderId(declaration: DeclarationDescriptor): ObjCExportHeaderId {
        val frameworkId = frameworkProvider.getFrameworkId(declaration.module.kotlinLibrary)
        val frameworkStructure = structure.frameworks.first { it.name == frameworkId.name }
        val headerName = frameworkStructure.headerName
        return ObjCExportHeaderId(frameworkId, headerName)
    }

    override fun getHeaderId(sourceFile: SourceFile): ObjCExportHeaderId {
        require(sourceFile is DeserializedSourceFile)
        val frameworkId = frameworkProvider.getFrameworkId(sourceFile.library)
        val frameworkStructure = structure.frameworks.first { it.name == frameworkId.name }
        val headerName = frameworkStructure.headerName
        return ObjCExportHeaderId(frameworkId, headerName)
    }

    override fun getStdlibHeaderId(): ObjCExportHeaderId {
        val frameworkId = frameworkProvider.getFrameworkId(stdlib)
        val frameworkStructure = structure.frameworks.first { it.name == frameworkId.name }
        val headerName = frameworkStructure.headerName
        return ObjCExportHeaderId(frameworkId, headerName)
    }
}

interface ObjCClassGeneratorProvider {
    fun getClassGenerator(headerId: ObjCExportHeaderId): ObjCExportClassGenerator
}

fun ObjCClassGeneratorProvider(mapping: Map<ObjCExportFrameworkId, ObjCExportClassGenerator>): ObjCClassGeneratorProvider =
        ObjCClassGeneratorProviderImpl(mapping)

private class ObjCClassGeneratorProviderImpl(
        private val mapping: Map<ObjCExportFrameworkId, ObjCExportClassGenerator>
) : ObjCClassGeneratorProvider {
    override fun getClassGenerator(headerId: ObjCExportHeaderId): ObjCExportClassGenerator =
            mapping.getValue(headerId.frameworkId)
}

interface ObjCNamerProvider {
    fun getNamer(headerId: ObjCExportHeaderId): ObjCExportNamer

    fun getStdlibNamer(): ObjCExportStdlibNamer
}

fun ObjCNamerProvider(
        globalConfig: ObjCExportGlobalConfig,
        structure: ObjCExportStructure,
): ObjCNamerProvider = ObjCNamerProviderImpl(globalConfig, structure)

private class ObjCNamerProviderImpl(
        private val globalConfig: ObjCExportGlobalConfig,
        private val structure: ObjCExportStructure,
) : ObjCNamerProvider {

    private val cache = mutableMapOf<ObjCExportFrameworkId, ObjCExportNamer>()

    override fun getNamer(headerId: ObjCExportHeaderId): ObjCExportNamer = cache.getOrPut(headerId.frameworkId) {
        val frameworkStructure = structure.frameworks.find { it.name == headerId.frameworkId.name }!!
        createObjCExportNamer(frameworkStructure)
    }

    override fun getStdlibNamer(): ObjCExportStdlibNamer {
        return stdlibNamer
    }

    private val stdlibNamer = ObjCExportStdlibNamer.create(globalConfig.stdlibPrefix)

    private fun createObjCExportNamer(structure: ObjCExportFrameworkStructure): ObjCExportNamer {
        val unitSuspendFunctionExport = globalConfig.unitSuspendFunctionExport
        val mapper = ObjCExportMapper(globalConfig.frontendServices.deprecationResolver, unitSuspendFunctionExport = unitSuspendFunctionExport)
        val objcGenerics = globalConfig.objcGenerics
        val disableSwiftMemberNameMangling = globalConfig.disableSwiftMemberNameMangling
        val ignoreInterfaceMethodCollisions = globalConfig.ignoreInterfaceMethodCollisions
        return ObjCExportNamerImpl(
                structure.modulesInfo.toSet(),
                structure.modulesInfo.first().module.builtIns,
                stdlibNamer,
                mapper,
                structure.topLevelPrefix,
                local = false,
                objcGenerics = objcGenerics,
                disableSwiftMemberNameMangling = disableSwiftMemberNameMangling,
                ignoreInterfaceMethodCollisions = ignoreInterfaceMethodCollisions,
        )
    }
}