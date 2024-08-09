/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties

private val Project.basePluginConfiguration: BasePluginExtension
    get() = extensions.getByType()

/**
 * The base name to use for archive files.
 */
internal val Project.archivesName
    get() = basePluginConfiguration.archivesName

/**
 * Returns the directory to generate JAR archives into.
 *
 * @return The directory. Never returns null.
 */
internal val Project.libsDirectory
    get() = basePluginConfiguration.libsDirectory

/**
 * Returns the directory to generate TAR and ZIP archives into.
 *
 * @return The directory. Never returns null.
 */
internal val Project.distsDirectory
    get() = basePluginConfiguration.distsDirectory

internal val Project.konanDistribution: KonanDistribution
    get() = KonanDistribution(nativeProperties.actualNativeHomeDirectory.get())

internal val Project.javaSourceSets: SourceSetContainer
    get() = extensions.getByType<JavaPluginExtension>().sourceSets

internal val Project.javaSourceSetsIfAvailable
    get() = extensions.findByType<JavaPluginExtension>()?.sourceSets