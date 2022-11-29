/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.util.slicedMap.Slices
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages
import java.io.File

object VersionReader {
    private val VERSIONS_SLICE: WritableSlice<ModuleDescriptor, RuntimeVersions> = Slices.createSimpleSlice()

    fun getVersionsForCurrentModuleFromTrace(module: ModuleDescriptor, trace: BindingTrace): RuntimeVersions? {
        trace.get(VERSIONS_SLICE, module)?.let { return it }
        val versions = getVersionsForCurrentModule(module) ?: return null
        trace.record(VERSIONS_SLICE, module, versions)
        return versions
    }

    fun getVersionsForCurrentModuleFromContext(module: ModuleDescriptor, context: BindingContext?): RuntimeVersions? {
        context?.get(VERSIONS_SLICE, module)?.let { return it }
        return getVersionsForCurrentModule(module)
    }

    fun getVersionsForCurrentModule(module: ModuleDescriptor): RuntimeVersions? {
        val markerClass = module.findClassAcrossModuleDependencies(
            ClassId(
                SerializationPackages.packageFqName,
                Name.identifier(SerialEntityNames.KSERIALIZER_CLASS)
            )
        ) ?: return null
        return CommonVersionReader.computeRuntimeVersions(markerClass.source)
    }

    fun canSupportInlineClasses(module: ModuleDescriptor, trace: BindingTrace): Boolean {
        // Klibs do not have manifest file, unfortunately, so we hope for the better
        return CommonVersionReader.canSupportInlineClasses(getVersionsForCurrentModuleFromTrace(module, trace))
    }

    // This method is needed to keep compatibility with IDE plugin
    fun getVersionsFromManifest(runtimeLibraryPath: File): RuntimeVersions {
        return CommonVersionReader.getVersionsFromManifest(runtimeLibraryPath)
    }
}
