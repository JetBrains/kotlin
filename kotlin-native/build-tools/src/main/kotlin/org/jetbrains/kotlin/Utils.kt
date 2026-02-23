/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.konan.target.*
import java.io.File

//region Project properties.

val Project.platformManager
    get() = extensions.getByType<PlatformManager>()

val Project.kotlinNativeDist: File
    get() {
        val validPropertiesNames = listOf(
                "konan.home",
                "org.jetbrains.kotlin.native.home",
                "kotlin.native.home"
        )
        val propertyValue = validPropertiesNames.firstNotNullOfOrNull { providers.gradleProperty(it).orNull }
        return isolated.rootProject.projectDirectory.asFile.resolve(propertyValue ?: "dist")
    }

val Project.nativeBundlesLocation
    get() = providers.gradleProperty("nativeBundlesLocation").orNull?.let { file(it) } ?: project.projectDir

fun projectOrFiles(proj: Project, notation: String): Any? {
    val ext = proj.extensions.getByType(ExtraPropertiesExtension::class.java)
    val propertyMapper = if (ext.has("notationMapping")) ext.get("notationMapping") else return proj.project(notation)
    val mapping = (propertyMapper as? Map<*, *>)?.get(notation) as? String ?: return proj.project(notation)
    return proj.files(mapping).also {
        proj.logger.info("MAPPING: $notation -> ${it.asPath}")
    }
}

//endregion

//region Task dependency.

val Project.isDefaultNativeHome: Boolean
    get() = kotlinNativeDist.absolutePath == project(":kotlin-native").isolated.projectDirectory.dir("dist").asFile.absolutePath

//endregion

internal val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()!!

internal val FileCollection.isNotEmpty: Boolean
    get() = !isEmpty
