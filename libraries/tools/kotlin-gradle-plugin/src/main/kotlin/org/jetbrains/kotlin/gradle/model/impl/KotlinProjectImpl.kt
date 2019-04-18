/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.ExperimentalFeatures
import org.jetbrains.kotlin.gradle.model.KotlinProject
import org.jetbrains.kotlin.gradle.model.SourceSet
import java.io.Serializable

/**
 * Implementation of the [KotlinProject] interface.
 */
data class KotlinProjectImpl(
    override val name: String,
    override val kotlinVersion: String,
    override val projectType: KotlinProject.ProjectType,
    override val sourceSets: Collection<SourceSet>,
    override val expectedByDependencies: Collection<String>,
    override val experimentalFeatures: ExperimentalFeatures
) : KotlinProject, Serializable {

    override val modelVersion = serialVersionUID

    companion object {
        private const val serialVersionUID = 1L
    }
}