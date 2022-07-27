/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private var LocalProperties: MutableMap<Project, Properties> = ConcurrentHashMap()

/**
 * Resolved properties from `local.properties` file(s).
 * Resolution first loads the properties from [Project::getRootDir],
 * and then potentially overrides them with [Project::getProjectDir] values.
 */
internal val Project.localProperties: Properties
    get() = LocalProperties[this] ?: Properties().also { props ->
        rootDir.resolve("local.properties").takeIf(File::exists)?.reader()?.use(props::load)
        projectDir.resolve("local.properties").takeIf(File::exists)?.reader()?.use(props::load)
    }.also { LocalProperties[this] = it }

/**
 * A resolved property lookup that combines the following sources with increasing priority:
 * 1. `gradle.properties`
 * 2. `local.properties`
 * 3. Gradle command line properties (`-P`)
 */
internal fun Project.findCompositeProperty(key: String): Any? {
    return gradle.startParameter.projectProperties[key] ?: localProperties[key] ?: findProperty(key)
}
