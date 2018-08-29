/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.isInterop

sealed class KonanModuleOrigin {

    companion object {
        val CAPABILITY = ModuleDescriptor.Capability<KonanModuleOrigin>("KonanModuleOrigin")
    }
}

sealed class CompiledKonanModuleOrigin: KonanModuleOrigin()

class DeserializedKonanModuleOrigin(val library: KonanLibrary) : CompiledKonanModuleOrigin()

object CurrentKonanModuleOrigin: CompiledKonanModuleOrigin()

object SyntheticModulesOrigin : KonanModuleOrigin()

internal fun KonanModuleOrigin.isInteropLibrary(): Boolean = when (this) {
    is DeserializedKonanModuleOrigin -> this.library.isInterop
    CurrentKonanModuleOrigin, SyntheticModulesOrigin -> false
}

val ModuleDescriptor.konanModuleOrigin get() = this.getCapability(KonanModuleOrigin.CAPABILITY)!!
