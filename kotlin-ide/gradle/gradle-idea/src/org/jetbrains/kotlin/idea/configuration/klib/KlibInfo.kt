/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.klib

import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

sealed class KlibInfo(
    val path: File,
    val sourcePaths: Collection<File>,
    val name: String
)

class NativeDistributionKlibInfo(
    path: File,
    sourcePaths: Collection<File>,
    name: String,
    val target: KonanTarget? // null means "common"
) : KlibInfo(path, sourcePaths, name)

class NativeDistributionCommonizedKlibInfo(
    path: File,
    sourcePaths: Collection<File>,
    name: String,
    val ownTarget: KonanTarget?, // null means "common"
    val commonizedTargets: Set<KonanTarget>
) : KlibInfo(path, sourcePaths, name)

// TODO: add other subclasses as necessary
