/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import java.io.File
import java.io.Serializable
import kotlin.jvm.Throws

public interface Commonizer : Serializable {
    @Throws(Throwable::class)
    public fun commonizeLibraries(
        konanHome: File,
        inputLibraries: Set<File>,
        dependencyLibraries: Set<CommonizerDependency>,
        outputCommonizerTarget: SharedCommonizerTarget,
        outputDirectory: File
    )
}
