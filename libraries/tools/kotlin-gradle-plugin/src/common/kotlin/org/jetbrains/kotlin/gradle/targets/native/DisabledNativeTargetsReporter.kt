/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

internal abstract class AggregateReporter {
    @Suppress("UNCHECKED_CAST")
    protected fun <T> getOrRegisterData(project: Project, propertyName: String): MutableList<T> =
        project.rootProject.extensions.getByType(ExtraPropertiesExtension::class.java).run {
            if (!has(propertyName)) {
                set(propertyName, mutableListOf<T>())
                project.gradle.taskGraph.whenReady { printWarning(project) }
            }
            get(propertyName)
        } as MutableList<T>

    protected abstract fun printWarning(project: Project)
}
