/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginConvention
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File

class DefaultDistribution(private val project: Project) : Distribution {

    private val basePluginConvention: BasePluginConvention
        get() = project.convention.plugins["base"] as BasePluginConvention

    override var directory: File by property {
        project.buildDir.resolve(basePluginConvention.distsDirName)
    }
}