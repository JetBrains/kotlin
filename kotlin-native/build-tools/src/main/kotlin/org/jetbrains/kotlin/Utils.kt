/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.tools.libname
import java.io.File

//region Project properties.

val Project.platformManager
    get() = extensions.getByType<PlatformManager>()

val Project.kotlinNativeDist: File
    get() = rootProject.project(":kotlin-native").run {
        val validPropertiesNames = listOf(
                "konan.home",
                "org.jetbrains.kotlin.native.home",
                "kotlin.native.home"
        )
        rootProject.file(validPropertiesNames.firstOrNull { hasProperty(it) }?.let { findProperty(it) } ?: "dist")
    }

val Project.nativeBundlesLocation
    get() = file(findProperty("nativeBundlesLocation") ?: project.projectDir)

fun projectOrFiles(proj: Project, notation: String): Any? {
    val propertyMapper = proj.findProperty("notationMapping") ?: return proj.project(notation)
    val mapping = (propertyMapper as? Map<*, *>)?.get(notation) as? String ?: return proj.project(notation)
    return proj.files(mapping).also {
        proj.logger.info("MAPPING: $notation -> ${it.asPath}")
    }
}

//endregion

//region Task dependency.

val Project.isDefaultNativeHome: Boolean
    get() = kotlinNativeDist.absolutePath == project(":kotlin-native").file("dist").absolutePath

//endregion

internal val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()!!

internal val FileCollection.isNotEmpty: Boolean
    get() = !isEmpty

/**
 * Given a [FileCollection] and [rootsMap]: a map from directory roots to their names,
 * return a sorted list of files.
 *
 * `.` is a special "default" root. All files that are under that root, or not under
 * any root at all, will be returned as relative to the "default" root. All other files
 * are returned as absolute files.
 *
 * This is useful for generating stable order of files for build reproducibility.
 */
fun FileCollection.stableSortedForReproducibility(rootsMap: Map<File, String>): List<File> = files.map {
    val file = it.canonicalFile.normalize()
    val defaultRootName = "."
    val defaultRoot = rootsMap.entries.find { (_, rootName) -> rootName == defaultRootName }
    checkNotNull(defaultRoot) {
        "$rootsMap must contain a root named $defaultRootName"
    }
    rootsMap.firstNotNullOfOrNull { (root, rootName) ->
        if (rootName != defaultRootName && file.startsWith(root)) {
            file to "${rootName}${File.separator}${file.toRelativeString(root)}"
        } else {
            null
        }
    } ?: run {
        val result = file.relativeTo(defaultRoot.key)
        result to result.path
    }
}.sortedBy { it.second }.map { it.first }

/**
 * Interpret [configuration] of [CppUsage.LIBRARY_LINK] as a combination of `-L` and `-l` flags for the compiler.
 *
 * The output will be in stable order as by [stableSortedForReproducibility], and files relative to the root named `.`
 * will be passed as relative paths, others as absolute.
 */
fun Project.asLinkFlags(configuration: Configuration, rootsMap: Map<File, String>): List<String> {
    val attr = configuration.attributes.getAttribute(CppUsage.USAGE_ATTRIBUTE)?.name
    require(attr == CppUsage.LIBRARY_LINK) {
        "Expected ${CppUsage.LIBRARY_LINK}, but $this is $attr"
    }
    val dynamicLibraries = configuration.incoming.artifactView {
        attributes {
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.DYNAMIC_LIB))
        }
    }.files
    val staticLibraries = configuration.incoming.artifactView {
        attributes {
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.LINK_ARCHIVE))
        }
    }.files
    return buildList {
        dynamicLibraries.stableSortedForReproducibility(rootsMap).flatMapTo(this) {
            listOf("-L${it.parentFile.path}", "-l${libname(it)}")
        }
        staticLibraries.stableSortedForReproducibility(rootsMap).flatMapTo(this) {
            listOf("-L${it.parentFile.path}", "-l${libname(it)}")
        }
    }
}
