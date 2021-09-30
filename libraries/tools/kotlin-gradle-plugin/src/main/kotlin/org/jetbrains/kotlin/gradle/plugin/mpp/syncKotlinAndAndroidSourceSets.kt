/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.AbstractAndroidProjectHandler.Companion.kotlinSourceSetNameForAndroidSourceSet
import org.jetbrains.kotlin.gradle.plugin.addConvention
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinSourceSetFactory
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinSourceSetFactory.Companion.defaultSourceFolder
import java.io.File

internal fun syncKotlinAndAndroidSourceSets(target: KotlinAndroidTarget) {
    val project = target.project
    val android = project.extensions.getByName("android") as BaseExtension

    android.sourceSets.all { androidSourceSet ->
        val kotlinSourceSetName = kotlinSourceSetNameForAndroidSourceSet(target, androidSourceSet.name)
        val kotlinSourceSet = project.kotlinExtension.sourceSets.maybeCreate(kotlinSourceSetName)
        androidSourceSet.kotlinSourceSet = kotlinSourceSet

        createDefaultDependsOnEdges(target, kotlinSourceSet, androidSourceSet)
        syncKotlinAndAndroidSourceDirs(target, kotlinSourceSet, androidSourceSet)
        syncKotlinAndAndroidResources(target, kotlinSourceSet, androidSourceSet)

        ifKaptEnabled(project) {
            Kapt3GradleSubplugin.createAptConfigurationIfNeeded(project, androidSourceSet.name)
        }
    }
}

internal var AndroidSourceSet.kotlinSourceSet: KotlinSourceSet
    get() = checkNotNull(kotlinSourceSetOrNull) { "Missing kotlinSourceSet for Android source set $name" }
    private set(value) {
        addConvention(KOTLIN_DSL_NAME, value)
    }

internal val AndroidSourceSet.kotlinSourceSetOrNull: KotlinSourceSet?
    get() = getConvention(KOTLIN_DSL_NAME) as? KotlinSourceSet

private fun createDefaultDependsOnEdges(
    target: KotlinAndroidTarget,
    kotlinSourceSet: KotlinSourceSet,
    androidSourceSet: AndroidSourceSet
) {
    val commonSourceSetName = when (androidSourceSet.name) {
        "main" -> "commonMain"
        "test" -> "commonTest"
        "androidTest" -> "commonTest"
        else -> return
    }
    val commonSourceSet = target.project.kotlinExtension.sourceSets.findByName(commonSourceSetName) ?: return
    kotlinSourceSet.dependsOn(commonSourceSet)
}

private fun syncKotlinAndAndroidSourceDirs(
    target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, androidSourceSet: AndroidSourceSet
) {
    val disambiguationClassifier = target.disambiguationClassifier

    /*
    Mitigate ambiguity!
    Example: disambiguationClassifier="android"
    Source Directory "src/androidTest/kotlin"
        -- could be claimed by kotlin {android}Test (unit test)
        -- could be claimed by Android androidTest (instrumented test)

    The Kotlin source set would win in this scenario.
     */
    if (disambiguationClassifier == null || !androidSourceSet.name.startsWith(disambiguationClassifier)) {
        kotlinSourceSet.kotlin.srcDir("src/${androidSourceSet.name}/kotlin")
    }

    kotlinSourceSet.kotlin.srcDirs(*androidSourceSet.java.srcDirs.toTypedArray())

    /*
    Make sure to include user configuration as well.
    Unfortunately, there does not exist any "ad-hoc" API like `all`.
    Therefore we sync the directories once again after evaluation
     */
    target.project.whenEvaluated {
        kotlinSourceSet.kotlin.srcDirs(*androidSourceSet.java.srcDirs.toTypedArray())
    }
}

private fun syncKotlinAndAndroidResources(
    target: KotlinAndroidTarget,
    kotlinSourceSet: KotlinSourceSet,
    androidSourceSet: AndroidSourceSet
) {
    /*
    Non-MPP projects will not have a different name for kotlin source sets vs Android source sets
    Registering resource folders is unnecessary therefore.
     */
    if (kotlinSourceSet.name == androidSourceSet.name) {
        return
    }

    val project = target.project

    androidSourceSet.resources.srcDirs(kotlinSourceSet.resources.srcDirs)
    if (androidSourceSet.resources.srcDirs.isNotEmpty()) {
        androidSourceSet.resources.srcDir(defaultSourceFolder(project, kotlinSourceSet.name, "resources"))
        kotlinSourceSet.resources.srcDirs(androidSourceSet.resources.srcDirs)
    }

    if (androidSourceSet.assets.srcDirs.isNotEmpty()) {
        androidSourceSet.assets.srcDir(defaultSourceFolder(project, kotlinSourceSet.name, "assets"))
    }

    if (androidSourceSet.res.srcDirs.isNotEmpty()) {
        androidSourceSet.res.srcDir(defaultSourceFolder(project, kotlinSourceSet.name, "res"))
    }

    if (androidSourceSet.aidl.srcDirs.isNotEmpty()) {
        androidSourceSet.aidl.srcDir(defaultSourceFolder(project, kotlinSourceSet.name, "aidl"))
    }

    if (androidSourceSet.renderscript.srcDirs.isNotEmpty()) {
        androidSourceSet.renderscript.srcDir(defaultSourceFolder(project, kotlinSourceSet.name, "rs"))
    }

    if (androidSourceSet.jni.srcDirs.isNotEmpty()) {
        androidSourceSet.jni.srcDir(defaultSourceFolder(project, kotlinSourceSet.name, "jni"))
    }

    if (androidSourceSet.jniLibs.srcDirs.isNotEmpty()) {
        androidSourceSet.jniLibs.srcDir(defaultSourceFolder(project, kotlinSourceSet.name, "jniLibs"))
    }

    if (androidSourceSet.shaders.srcDirs.isNotEmpty()) {
        androidSourceSet.shaders.srcDir(defaultSourceFolder(project, kotlinSourceSet.name, "shaders"))
    }
}

