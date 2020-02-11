/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

abstract class KotlinBinaryContainer<T : KotlinCompilation<*>, R : DomainObjectSet<*>>(
    project: Project,
    platformType: KotlinPlatformType
) : KotlinOnlyTarget<T>(project, platformType) {
    abstract val binaries: R

    fun binaries(configure: R.() -> Unit) {
        binaries.configure()
    }

    fun binaries(configure: Closure<*>) {
        ConfigureUtil.configure(configure, binaries)
    }
}