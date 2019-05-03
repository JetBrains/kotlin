/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.lite

import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.konan.library.konanCommonLibraryPath
import java.io.File

object LiteKonanDistributionProvider {
    fun getDistribution(konanHomeDir: File): LiteKonanDistribution? {
        val stdlib = LiteKonanLibraryFacade.getDistributionLibraryProvider(konanHomeDir)
            .getLibrary(konanHomeDir.resolve(konanCommonLibraryPath(KONAN_STDLIB_NAME))) as? LiteKonanLibraryImpl ?: return null

        return LiteKonanDistribution(
            konanHomeDir,
            KonanVersion.fromString(stdlib.compilerVersion),
            stdlib.compilerVersion
        )
    }
}
