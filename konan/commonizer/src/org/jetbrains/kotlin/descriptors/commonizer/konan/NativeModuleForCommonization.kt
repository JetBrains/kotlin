/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import java.io.File

internal sealed class NativeModuleForCommonization(val module: ModuleDescriptorImpl) {
    class DeserializedModule(
        module: ModuleDescriptorImpl,
        val data: NativeSensitiveManifestData,
        val location: File
    ) : NativeModuleForCommonization(module)

    class SyntheticModule(module: ModuleDescriptorImpl) : NativeModuleForCommonization(module)
}
