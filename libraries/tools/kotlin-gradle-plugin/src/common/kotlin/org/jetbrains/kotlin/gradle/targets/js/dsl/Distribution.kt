/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import java.io.File

interface Distribution {
    @Deprecated("Use `distributionName` instead", ReplaceWith("distributionName"))
    var name: String?

    val distributionName: Property<String>

    @Deprecated("Use `outputDirectory` instead", ReplaceWith("outputDirectory"))
    var directory: File

    val outputDirectory: DirectoryProperty

    companion object {
        const val DIST = "dist"
    }
}