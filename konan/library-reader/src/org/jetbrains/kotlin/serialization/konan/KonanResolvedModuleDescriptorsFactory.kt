/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolver.KonanLibraryResolveResult
import org.jetbrains.kotlin.storage.StorageManager

interface KonanResolvedModuleDescriptorsFactory {

    val moduleDescriptorFactory: KonanDeserializedModuleDescriptorFactory

    /**
     * Given the [resolvedLibraries] creates the list of [ModuleDescriptorImpl]s with properly installed
     * inter-dependencies. The result of this method is returned in a form of [KonanResolvedModuleDescriptors] instance.
     *
     * Please use this method with care: Unless this method accepts `null` for [builtIns], it is not recommended to
     * invoke it this way. If you are compiling a source module, please supply the non-null [builtIns] from the
     * source module, so that all modules created in your compilation session will share the same built-ins instance.
     *
     * Otherwise (if `null` was supplied), a new instance of [KotlinBuiltIns] will be created. The created built-ins
     * instance will be shared by all modules created in this method. But this instance will have no connection
     * with probably existing built-ins instance of your source module(s).
     */
    fun createResolved(
        resolvedLibraries: KonanLibraryResolveResult,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        languageVersionSettings: LanguageVersionSettings,
        customAction: ((KonanLibrary, ModuleDescriptorImpl) -> Unit)? = null,
        customCapabilitiesGenerator: ((KonanLibrary) -> Map<ModuleDescriptor.Capability<*>, Any?>)? = null
    ): KonanResolvedModuleDescriptors
}

class KonanResolvedModuleDescriptors(

    /**
     * The list of modules each representing an individual Kotlin/Native library. All modules
     * in this list have properly installed dependencies, i.e. module has all necessary dependencies
     * on other modules plus a dependency on the [forwardDeclarationsModule].
     */
    val resolvedDescriptors: List<ModuleDescriptorImpl>,

    /**
     * This is a module which "contains" forward declarations.
     * Note: this module should be unique per compilation and should always be the last dependency of any module.
     */
    val forwardDeclarationsModule: ModuleDescriptorImpl
)
