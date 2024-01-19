/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinSourceSetFactory
import org.jetbrains.kotlin.gradle.utils.*

internal object MultiplatformAndroidResourceDirConfigurator : KotlinAndroidSourceSetConfigurator {
    override fun configure(
        target: KotlinAndroidTarget,
        kotlinSourceSet: KotlinSourceSet,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") androidSourceSet: DeprecatedAndroidSourceSet
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
            androidSourceSet.resources.srcDir(KotlinSourceSetFactory.defaultSourceFolder(project, kotlinSourceSet.name, "resources"))
            kotlinSourceSet.resources.srcDirs(androidSourceSet.resources.srcDirs)
        }

        if (androidSourceSet.assets.srcDirs.isNotEmpty()) {
            androidSourceSet.assets.srcDir(KotlinSourceSetFactory.defaultSourceFolder(project, kotlinSourceSet.name, "assets"))
        }

        if (androidSourceSet.res.srcDirs.isNotEmpty()) {
            androidSourceSet.res.srcDir(KotlinSourceSetFactory.defaultSourceFolder(project, kotlinSourceSet.name, "res"))
        }

        if (androidSourceSet.aidl.srcDirs.isNotEmpty()) {
            androidSourceSet.aidl.srcDir(KotlinSourceSetFactory.defaultSourceFolder(project, kotlinSourceSet.name, "aidl"))
        }

        if (androidSourceSet.renderscript.srcDirs.isNotEmpty()) {
            androidSourceSet.renderscript.srcDir(KotlinSourceSetFactory.defaultSourceFolder(project, kotlinSourceSet.name, "rs"))
        }

        @Suppress("DEPRECATION")
        if (androidSourceSet.jni.srcDirs.isNotEmpty()) {
            androidSourceSet.jni.srcDir(KotlinSourceSetFactory.defaultSourceFolder(project, kotlinSourceSet.name, "jni"))
        }

        if (androidSourceSet.jniLibs.srcDirs.isNotEmpty()) {
            androidSourceSet.jniLibs.srcDir(KotlinSourceSetFactory.defaultSourceFolder(project, kotlinSourceSet.name, "jniLibs"))
        }

        if (androidSourceSet.shaders.srcDirs.isNotEmpty()) {
            androidSourceSet.shaders.srcDir(KotlinSourceSetFactory.defaultSourceFolder(project, kotlinSourceSet.name, "shaders"))
        }
    }
}