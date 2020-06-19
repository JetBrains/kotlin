/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.cocoapods

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.gradle.api.tasks.*
import java.io.File

open class CocoapodsExtension(private val project: Project) {
    @get:Input
    val version: String
        get() = project.version.toString()

    /**
     * Configure authors of the pod built from this project.
     */
    @Optional
    @Input
    var authors: String? = null

    /**
     * Configure existing file `Podfile`.
     */
    @Optional
    @InputFile
    var podfile: File? = null

    @get:Input
    internal var needPodspec: Boolean = true

    /**
     * Setup plugin not to produce podspec file for cocoapods section
     */
    fun noPodspec() {
        needPodspec = false
    }

    /**
     * Configure license of the pod built from this project.
     */
    @Optional
    @Input
    var license: String? = null

    /**
     * Configure description of the pod built from this project.
     */
    @Optional
    @Input
    var summary: String? = null

    /**
     * Configure homepage of the pod built from this project.
     */
    @Optional
    @Input
    var homepage: String? = null

    @Nested
    val ios: PodspecPlatformSettings = PodspecPlatformSettings("ios")

    @Nested
    val osx: PodspecPlatformSettings = PodspecPlatformSettings("osx")

    @Nested
    val tvos: PodspecPlatformSettings = PodspecPlatformSettings("tvos")

    @Nested
    val watchos: PodspecPlatformSettings = PodspecPlatformSettings("watchos")

    /**
     * Configure framework name of the pod built from this project.
     */
    @Input
    var frameworkName: String = project.name.asValidFrameworkName()

    private val _pods = project.container(CocoapodsDependency::class.java)

    // For some reason Gradle doesn't consume the @Nested annotation on NamedDomainObjectContainer.
    @get:Nested
    protected val podsAsTaskInput: List<CocoapodsDependency>
        get() = _pods.toList()

    /**
     * Returns a list of pod dependencies.
     */
    // Already taken into account as a task input in the [podsAsTaskInput] property.
    @get:Internal
    val pods: NamedDomainObjectSet<CocoapodsDependency>
        get() = _pods

    /**
     * Add a CocoaPods dependency to the pod built from this project.
     */
    @JvmOverloads
    fun pod(name: String, version: String? = null, podspec: File? = null, moduleName: String = name.split("/")[0]) {
        check(_pods.findByName(name) == null) { "Project already has a CocoaPods dependency with name $name" }
        _pods.add(CocoapodsDependency(name, version, podspec, moduleName))
    }

    data class CocoapodsDependency(
        private val name: String,
        @get:Optional @get:Input val version: String?,
        @get:Optional @get:InputFile val podspec: File?,
        @get:Input val moduleName: String
    ) : Named {
        @Input
        override fun getName(): String = name
    }

    data class PodspecPlatformSettings(
        private val name: String,
        @get:Optional @get:Input var deploymentTarget: String? = null
    ) : Named {

        @Input
        override fun getName(): String = name
    }
}