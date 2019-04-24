/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.platform

import org.jetbrains.kotlin.resolve.*

object KonanPlatform : TargetPlatform("Native") {
    override val platform = MultiTargetPlatform.Specific(platformName)
}
