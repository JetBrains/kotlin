/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.distsDirectory
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File

class DefaultDistribution(
    private val project: Project,
    override var name: String? = null
) : Distribution {

    override var directory: File by property {
        project.buildDir
            .let { buildDir ->
                name?.let { buildDir.resolve(it) }
                    ?: project.distsDirectory.asFile.get()
            }
    }
}
