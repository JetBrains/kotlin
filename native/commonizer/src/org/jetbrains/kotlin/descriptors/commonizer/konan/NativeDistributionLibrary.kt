/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.library.KotlinLibrary

internal class NativeDistributionLibrary(
    val library: KotlinLibrary
) {
    val manifestData = NativeSensitiveManifestData.readFrom(library)
}

internal class NativeDistributionLibraries(
    val stdlib: NativeDistributionLibrary,
    val platformLibs: List<NativeDistributionLibrary>
) {
    constructor(stdlib: KotlinLibrary, platformLibs: List<KotlinLibrary>) : this(
        NativeDistributionLibrary(stdlib),
        platformLibs.map(::NativeDistributionLibrary)
    )

    val index: Map<String, NativeDistributionLibrary> = (platformLibs + stdlib).associateBy { it.manifestData.uniqueName }
}
