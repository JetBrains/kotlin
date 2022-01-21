/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName

/**
 * Registers a [Jar] task with the variant's compilation outputs and attaches this artifact to the given configuration.
 */
val KotlinFragmentCompilationOutputsJarArtifact = FragmentArtifacts<KotlinGradleVariant> {
    artifact(project.locateOrRegisterTask<Jar>(fragment.disambiguateName("jar")) {
        it.from(fragment.compilationOutputs.allOutputs)
        it.archiveClassifier.set(dashSeparatedName(fragment.name, fragment.containingModule.moduleClassifier))
    })
}
