/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.artifacts.Dependency
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleVariant
import org.jetbrains.kotlin.gradle.kpm.util.FragmentNameDisambiguationOmittingMain
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.dashSeparatedLowercaseName

/**
 * Registers a [Jar] task with the variant's compilation outputs and attaches this artifact to the given configuration.
 */
val KotlinFragmentCompilationOutputsJarArtifact = FragmentArtifacts<KotlinGradleVariant> {
    val jar = project.locateOrRegisterTask<Jar>(fragment.outputsJarTaskName) {
        it.from(fragment.compilationOutputs.allOutputs)
        it.archiveAppendix.set(dashSeparatedLowercaseName(fragment.name, fragment.containingModule.moduleClassifier))
    }
    artifact(jar)
    fragment.project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, jar)
}

internal val KotlinGradleVariant.outputsJarTaskName: String
    get() = FragmentNameDisambiguationOmittingMain(containingModule, name).disambiguateName("jar")