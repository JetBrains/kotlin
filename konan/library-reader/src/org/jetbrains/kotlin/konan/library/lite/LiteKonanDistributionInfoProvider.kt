/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.lite

import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.konan.library.konanCommonLibraryPath
import java.nio.file.Paths

class LiteKonanDistributionInfoProvider(private val konanHomeDir: String) {

    fun getDistributionInfo(): LiteKonanDistribution? {
        val stdlibInfo = LiteKonanLibraryInfoProvider(konanHomeDir).getDistributionLibraryInfo(
            Paths.get(konanHomeDir).resolve(konanCommonLibraryPath(KONAN_STDLIB_NAME))
        ) ?: return null

        val versionString = stdlibInfo.compilerVersion
        val version = versionString.substringBefore('-')
            .split('.')
            .takeIf { it.size == 3 }
            ?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == 3 }
            ?.let {
                KotlinVersion(it[0], it[1], it[2])
            } ?: return null

        return LiteKonanDistribution(konanHomeDir, version, versionString)
    }
}
