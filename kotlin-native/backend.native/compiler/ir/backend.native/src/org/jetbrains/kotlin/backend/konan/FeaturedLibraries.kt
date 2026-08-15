/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.utils.mapToSetOrEmpty

internal fun ModuleDescriptor.getExportedDependencies(config: NativeSecondStageCompilationConfig): List<ModuleDescriptor> =
        getDescriptorsFromLibraries((config.loadedKlibs.exported + config.loadedKlibs.included).toSet())

internal fun ModuleDescriptor.getIncludedLibraryDescriptors(config: NativeSecondStageCompilationConfig): List<ModuleDescriptor> =
        getDescriptorsFromLibraries(config.loadedKlibs.included.toSet())

private fun ModuleDescriptor.getDescriptorsFromLibraries(libraries: Set<KotlinLibrary>): List<ModuleDescriptor> {
    val libraryNames = libraries.mapToSetOrEmpty { it.uniqueName }
    return allDependencyModules.filter { it.name.asStringStripSpecialMarkers() in libraryNames }
}
