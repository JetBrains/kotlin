/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.plugin.internal.BasePluginConfiguration
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory

/**
 * The base name to use for archive files.
 */
internal val Project.archivesName
    get() = variantImplementationFactory<BasePluginConfiguration.BasePluginConfigurationVariantFactory>()
        .getInstance(this)
        .archivesName


/**
 * Returns the directory to generate JAR archives into.
 *
 * @return The directory. Never returns null.
 */
internal val Project.libsDirectory
    get() = variantImplementationFactory<BasePluginConfiguration.BasePluginConfigurationVariantFactory>()
        .getInstance(this)
        .libsDirectory


/**
 * Returns the directory to generate TAR and ZIP archives into.
 *
 * @return The directory. Never returns null.
 */
internal val Project.distsDirectory
    get() = variantImplementationFactory<BasePluginConfiguration.BasePluginConfigurationVariantFactory>()
        .getInstance(this)
        .distsDirectory

internal val Project.konanDistribution: KonanDistribution
    get() = KonanDistribution(project.file(konanHome))
