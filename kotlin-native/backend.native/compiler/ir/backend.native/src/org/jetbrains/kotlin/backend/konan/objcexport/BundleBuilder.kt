/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.Family

/**
 * Builds Apple bundle directory.
 */
internal class BundleBuilder(
        private val config: KonanConfig,
        private val infoPListBuilder: InfoPListBuilder,
        private val mainPackageGuesser: MainPackageGuesser,
) {
    fun build(
            moduleDescriptor: ModuleDescriptor,
            bundleDirectory: File,
            name: String,
    ) {
        val target = config.target
        val bundleContents = when (target.family) {
            Family.IOS,
            Family.WATCHOS,
            Family.TVOS -> bundleDirectory
            Family.OSX -> bundleDirectory.child("Contents")
            else -> error(target)
        }.apply { mkdirs() }

        bundleContents.child("Info.plist").run {
            val infoPlistContents = infoPListBuilder.build(name, mainPackageGuesser, moduleDescriptor)
            writeBytes(infoPlistContents.toByteArray())
        }
    }
}