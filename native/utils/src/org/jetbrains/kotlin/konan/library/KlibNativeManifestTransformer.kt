/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.substituteFor
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibManifestTransformer
import java.util.Properties

/**
 * An extension of [KlibLoader] specifically for Kotlin/Native libraries that allows doing the proper substitution of properties
 * in the manifest based on the specified [target].
 */
class KlibNativeManifestTransformer(private val target: KonanTarget) : KlibManifestTransformer {
    override fun transform(manifestProperties: Properties) = manifestProperties.substituteFor(target)
}
