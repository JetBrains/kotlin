/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
    private val myName: String,
    private val myKotlinVersion: String,
    private val myProjectType: KotlinProject.ProjectType,
    private val mySourceSets: Collection<SourceSet>,
    private val myExpectedByDependencies: Collection<String>,
    private val myExperimentalFeatures: ExperimentalFeatures
) : KotlinProject, Serializable {

    override fun getModelVersion(): Long {
        return serialVersionUID
    }

    override fun getName(): String {
        return myName
    }

    override fun getKotlinVersion(): String {
        return myKotlinVersion
    }

    override fun getProjectType(): KotlinProject.ProjectType {
        return myProjectType
    }

    override fun getSourceSets(): Collection<SourceSet> {
        return mySourceSets
    }

    override fun getExpectedByDependencies(): Collection<String> {
        return myExpectedByDependencies
    }

    override fun getExperimentalFeatures(): ExperimentalFeatures {
        return myExperimentalFeatures
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}