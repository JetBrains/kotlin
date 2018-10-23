/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.platform

import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.PlatformConfigurator
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.storage.StorageManager

object KonanPlatform : TargetPlatform("Native") {

    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {
        result.add(ImportPath.fromString("kotlin.native.*"))
    }

    override val multiTargetPlatform = MultiTargetPlatform.Specific(platformName)
    override val platformConfigurator: PlatformConfigurator = KonanPlatformConfigurator
}
