/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.compileStubSourceWithSourceSetName
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.settingsBuildScriptInjection
import org.jetbrains.kotlin.gradle.uklibs.PublishedProject
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.publish

internal fun KGPBaseTest.publishMultiplatformLibrary(
    gradleVersion: GradleVersion,
    projectName: String = "multiplatformLibrary",
    configure: KotlinMultiplatformExtension.() -> Unit = {
        iosArm64()
        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
    },
): PublishedProject = project("empty", gradleVersion) {
    plugins {
        kotlin("multiplatform")
    }
    settingsBuildScriptInjection {
        settings.rootProject.name = projectName
    }
    buildScriptInjection {
        project.applyMultiplatform(configure)
    }
}.publish(publisherConfiguration = PublisherConfiguration())