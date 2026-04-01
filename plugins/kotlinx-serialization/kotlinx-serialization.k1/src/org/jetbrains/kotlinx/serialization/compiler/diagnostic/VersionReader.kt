/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages
import java.io.File

object VersionReader {
    val cache = mutableMapOf<ModuleDescriptor, RuntimeVersions?>()

    fun getVersionsForCurrentModuleFromTrace(module: ModuleDescriptor): RuntimeVersions? = cache.getOrPut(module) {
        getVersionsForCurrentModule(module)
    }

    private fun getVersionsForCurrentModule(module: ModuleDescriptor): RuntimeVersions? {
        val markerClass = module.findClassAcrossModuleDependencies(
            ClassId(
                SerializationPackages.packageFqName,
                Name.identifier(SerialEntityNames.KSERIALIZER_CLASS)
            )
        ) ?: return null
        return CommonVersionReader.computeRuntimeVersions(markerClass.source)
    }

    // This method is needed to keep compatibility with IDE plugin
    fun getVersionsFromManifest(runtimeLibraryPath: File): RuntimeVersions {
        return CommonVersionReader.getVersionsFromManifest(runtimeLibraryPath)
    }
}
