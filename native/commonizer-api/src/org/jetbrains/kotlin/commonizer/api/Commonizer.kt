/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.api

import java.io.File
import kotlin.jvm.Throws

public interface Commonizer {
    @Throws(Throwable::class)
    public operator fun invoke(
        konanHome: File,
        targetLibraries: Set<File>,
        dependencyLibraries: Set<File>,
        outputHierarchy: SharedCommonizerTarget,
        outputDirectory: File
    )
}

