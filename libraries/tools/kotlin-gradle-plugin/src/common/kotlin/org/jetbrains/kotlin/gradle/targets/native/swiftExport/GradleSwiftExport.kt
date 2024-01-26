/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.swiftExport

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.createResolvable
import org.jetbrains.kotlin.gradle.utils.findResolvable
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.gradle.utils.setAttribute

internal const val KOTLIN_NATIVE_SIR_CLASSPATH_CONFIGURATION_NAME = "kotlinNativeSirClasspath"

private const val SIR_RUNTIME_EMBEDDABLE = "sir-runner-embeddable"

internal fun Project.maybeCreateSwiftExportClasspathConfiguration(): Configuration {
    return configurations.findResolvable(KOTLIN_NATIVE_SIR_CLASSPATH_CONFIGURATION_NAME)
        ?: project.configurations.createResolvable(KOTLIN_NATIVE_SIR_CLASSPATH_CONFIGURATION_NAME)
            .run {
                attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                attributes.setAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
                attributes.setAttribute(Usage.USAGE_ATTRIBUTE, usageByName(Usage.JAVA_RUNTIME))
                defaultDependencies { dependencies ->
                    dependencies.add(
                        project.dependencies.create("$KOTLIN_MODULE_GROUP:$SIR_RUNTIME_EMBEDDABLE:${getKotlinPluginVersion()}")
                    )
                }
            }
}