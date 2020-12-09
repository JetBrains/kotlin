/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.api

import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR
import java.io.File


public fun interface CommonizerOutputLayout {
    public fun getTargetDirectory(root: File, target: CommonizerTarget): File
}

public object NativeDistributionCommonizerOutputLayout : CommonizerOutputLayout {
    override fun getTargetDirectory(root: File, target: CommonizerTarget): File {
        return when (target) {
            is LeafCommonizerTarget -> root.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR).resolve(target.name)
            is SharedCommonizerTarget -> root.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
        }
    }
}

public object HierarchicalCommonizerOutputLayout : CommonizerOutputLayout {
    override fun getTargetDirectory(root: File, target: CommonizerTarget): File {
        return root.resolve(target.identityString)
    }
}

