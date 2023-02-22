/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.kotlinLibrary

internal interface ObjCExportKlibMapper {
    fun getNamerFor(library: KotlinLibrary): ObjCExportNamer

    fun getHeaderInfoFor(library: KotlinLibrary): ObjCExportHeaderInfo

    companion object {
        fun create(
                globalConfig: ObjCExportGlobalConfig,
                headerInfos: List<ObjCExportHeaderInfo>,
        ): ObjCExportKlibMapper {
            return ObjCExportKlibMapperImpl(ObjCExportKlibMappingBuilder(globalConfig, headerInfos))
        }
    }
}

/**
 * TODO: Should be easily parseable from CLI arg (JSON file)?
 */
internal class ObjCExportHeaderInfo(
        val topLevelPrefix: String,
        val modules: List<ModuleDescriptor>,
        val frameworkName: String,
        val headerName: String,
) {
    override fun equals(other: Any?): Boolean = other is ObjCExportHeaderInfo &&
            other.frameworkName == frameworkName &&
            other.headerName == headerName

    override fun hashCode(): Int = frameworkName.hashCode() * 31 + headerName.hashCode()
}

internal class ObjCExportKlibMappingEntry(
        val klib: KotlinLibrary,
        val namer: ObjCExportNamer,
)

internal class ObjCExportKlibMappingBuilder(
        val globalConfig: ObjCExportGlobalConfig,
        val headerInfos: List<ObjCExportHeaderInfo>,
) {

    private val stdlibNamer = ObjCExportStdlibNamer.create(globalConfig.stdlibPrefix)

    private val klibMapping = mutableMapOf<KotlinLibrary, ObjCExportNamer>()
    private val headerInfoMapping = mutableMapOf<KotlinLibrary, ObjCExportHeaderInfo>()

    fun getHeaderInfo(library: KotlinLibrary): ObjCExportHeaderInfo = headerInfoMapping.getOrPut(library) {
        headerInfos.find { it.modules.any { it.kotlinLibrary == library } }
                ?: error("library ${library.libraryName} does not belong to any header")
    }

    fun getOrCreateNamer(library: KotlinLibrary): ObjCExportNamer = klibMapping.getOrPut(library) {
        val headerInfo = headerInfos.find { it.modules.any { it.kotlinLibrary == library } }
                ?: error("library ${library.libraryName} does not belong to any header")
        createObjCExportNamer(headerInfo)
    }

    fun getStdlibNamer(): ObjCExportStdlibNamer =
            stdlibNamer

    private fun createObjCExportNamer(headerInfo: ObjCExportHeaderInfo): ObjCExportNamer {
        val unitSuspendFunctionExport = globalConfig.unitSuspendFunctionExport
        val mapper = ObjCExportMapper(globalConfig.frontendServices.deprecationResolver, unitSuspendFunctionExport = unitSuspendFunctionExport)
        val objcGenerics = globalConfig.objcGenerics
        val disableSwiftMemberNameMangling = globalConfig.disableSwiftMemberNameMangling
        val ignoreInterfaceMethodCollisions = globalConfig.ignoreInterfaceMethodCollisions
        return ObjCExportNamerImpl(
                headerInfo.modules.toSet(),
                headerInfo.modules.first().builtIns,
                stdlibNamer,
                mapper,
                headerInfo.topLevelPrefix,
                local = false,
                objcGenerics = objcGenerics,
                disableSwiftMemberNameMangling = disableSwiftMemberNameMangling,
                ignoreInterfaceMethodCollisions = ignoreInterfaceMethodCollisions,
        )
    }
}

private class ObjCExportKlibMapperImpl(private val builder: ObjCExportKlibMappingBuilder) : ObjCExportKlibMapper {
    override fun getNamerFor(library: KotlinLibrary): ObjCExportNamer =
            builder.getOrCreateNamer(library)

    override fun getHeaderInfoFor(library: KotlinLibrary): ObjCExportHeaderInfo =
            builder.getHeaderInfo(library)
}

