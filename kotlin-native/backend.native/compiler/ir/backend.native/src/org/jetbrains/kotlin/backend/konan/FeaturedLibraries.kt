/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.SyntheticModulesOrigin
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin

internal fun ModuleDescriptor.getExportedDependencies(config: NativeSecondStageCompilationConfig): List<ModuleDescriptor> =
        getDescriptorsFromLibraries((config.loadedKlibs.exported + config.loadedKlibs.included).toSet())

internal fun ModuleDescriptor.getIncludedLibraryDescriptors(config: NativeSecondStageCompilationConfig): List<ModuleDescriptor> =
        getDescriptorsFromLibraries(config.loadedKlibs.included.toSet())

private fun ModuleDescriptor.getDescriptorsFromLibraries(libraries: Set<KotlinLibrary>) =
    allDependencyModules.filter {
        when (val origin = it.klibModuleOrigin) {
            CurrentKlibModuleOrigin, SyntheticModulesOrigin -> false
            is DeserializedKlibModuleOrigin -> origin.library in libraries
        }
    }
