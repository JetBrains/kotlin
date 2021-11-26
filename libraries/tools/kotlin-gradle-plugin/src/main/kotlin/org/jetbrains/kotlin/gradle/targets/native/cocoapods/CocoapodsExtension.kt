/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.cocoapods

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.gradle.api.tasks.*
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency.PodLocation.*
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBinary
import java.io.File
import java.net.URI

open class CocoapodsExtension(private val project: Project) {
    /**
     * Configure version of the pod
     */
    var version: String? = null

    /**
     * Configure authors of the pod built from this project.
     */
    var authors: String? = null

    /**
     * Configure existing file `Podfile`.
     */
    var podfile: File? = null

    internal var needPodspec: Boolean = true

    /**
     * Setup plugin not to produce podspec file for cocoapods section
     */
    fun noPodspec() {
        needPodspec = false
    }

    /**
     * Setup cocoapods-generate to produce xcodeproj compatible with static libraries
     */
    fun useLibraries() {
        useLibraries = true
    }

    internal var useLibraries: Boolean = false

    /**
     * Configure name of the pod built from this project.
     */
    var name: String = project.name.asValidFrameworkName()

    /**
     * Configure license of the pod built from this project.
     */
    var license: String? = null

    /**
     * Configure description of the pod built from this project.
     */
    var summary: String? = null

    /**
     * Configure homepage of the pod built from this project.
     */
    var homepage: String? = null

    /**
     * Configure location of the pod built from this project.
     */
    var source: String? = null

    /**
     * Configure other podspec attributes
     */
    var extraSpecAttributes: MutableMap<String, String> = mutableMapOf()

    /**
     * Configure framework of the pod built from this project.
     */
    fun framework(configure: Framework.() -> Unit) = configureRegisteredFrameworks(configure)

    /**
     * Configure framework of the pod built from this project.
     */
    fun framework(configure: Action<Framework>) = framework {
        configure.execute(this)
    }

    val ios: PodspecPlatformSettings = PodspecPlatformSettings("ios")

    val osx: PodspecPlatformSettings = PodspecPlatformSettings("osx")

    val tvos: PodspecPlatformSettings = PodspecPlatformSettings("tvos")

    val watchos: PodspecPlatformSettings = PodspecPlatformSettings("watchos")

    /**
     * Configure framework name of the pod built from this project.
     */
    @Deprecated("Use 'baseName' property within framework{} block to configure framework name")
    var frameworkName: String
        get() = frameworkNameInternal
        set(value) {
            configureRegisteredFrameworks {
                baseName = value
            }
        }

    internal var frameworkNameInternal: String = project.name.asValidFrameworkName()

    internal var useDynamicFramework: Boolean = false

    /**
     * Configure custom Xcode Configurations to Native Build Types mapping
     */
    val xcodeConfigurationToNativeBuildType: MutableMap<String, NativeBuildType> = mutableMapOf(
        "Debug" to NativeBuildType.DEBUG,
        "Release" to NativeBuildType.RELEASE
    )

    /**
     * Configure output directory for pod publishing
     */
    var publishDir: File = CocoapodsBuildDirs(project).publish

    internal val specRepos = SpecRepos()

    private val _pods = project.container(CocoapodsDependency::class.java)

    val podsAsTaskInput: List<CocoapodsDependency>
        get() = _pods.toList()

    /**
     * Returns a list of pod dependencies.
     */
    val pods: NamedDomainObjectSet<CocoapodsDependency>
        get() = _pods

    /**
     * Add a CocoaPods dependency to the pod built from this project.
     */
    @JvmOverloads
    fun pod(name: String, version: String? = null, path: File? = null, moduleName: String = name.asModuleName()) {
        // Empty string will lead to an attempt to create two podDownload tasks.
        // One is original podDownload and second is podDownload + pod.name
        require(name.isNotEmpty()) { "Please provide not empty pod name to avoid ambiguity" }
        var podSource = path
        if (path != null && !path.isDirectory) {
            val pattern = "\\W*pod(.*\"${name}\".*)".toRegex()
            val buildScript = project.buildFile
            val lines = buildScript.readLines()
            val lineNumber = lines.indexOfFirst { pattern.matches(it) }
            val warnMessage = if (lineNumber != -1) run {
                val lineContent = lines[lineNumber].trimIndent()
                val newContent = lineContent.replace(path.name, "")
                """
                |Deprecated DSL found on ${buildScript.absolutePath}${File.pathSeparator}${lineNumber + 1}:
                |Found: "${lineContent}"
                |Expected: "${newContent}"
                |Please, change the path to avoid this warning.
                |
            """.trimMargin()
            } else
                """
                |Deprecated DSL is used for pod "$name".
                |Please, change its path from ${path.path} to ${path.parentFile.path} 
                |
            """.trimMargin()
            project.logger.warn(warnMessage)
            podSource = path.parentFile
        }
        addToPods(CocoapodsDependency(name, moduleName, version, podSource?.let { Path(it) }))
    }


    /**
     * Add a CocoaPods dependency to the pod built from this project.
     */
    fun pod(name: String, configure: CocoapodsDependency.() -> Unit) {
        // Empty string will lead to an attempt to create two podDownload tasks.
        // One is original podDownload and second is podDownload + pod.name
        require(name.isNotEmpty()) { "Please provide not empty pod name to avoid ambiguity" }
        val dependency = CocoapodsDependency(name, name.asModuleName())
        dependency.configure()
        addToPods(dependency)
    }

    /**
     * Add a CocoaPods dependency to the pod built from this project.
     */
    fun pod(name: String, configure: Closure<*>) = pod(name) {
        ConfigureUtil.configure(configure, this)
    }

    private fun addToPods(dependency: CocoapodsDependency) {
        val name = dependency.name
        check(_pods.findByName(name) == null) { "Project already has a CocoaPods dependency with name $name" }
        _pods.add(dependency)
    }

    /**
     * Add spec repositories (note that spec repository is different from usual git repository).
     * Please refer to <a href="https://guides.cocoapods.org/making/private-cocoapods.html">cocoapods documentation</a>
     * for additional information.
     * Default sources (cdn.cocoapods.org) implicitly included.
     */
    fun specRepos(configure: SpecRepos.() -> Unit) = specRepos.configure()

    /**
     * Add spec repositories (note that spec repository is different from usual git repository).
     * Please refer to <a href="https://guides.cocoapods.org/making/private-cocoapods.html">cocoapods documentation</a>
     * for additional information.
     * Default sources (cdn.cocoapods.org) implicitly included.
     */
    fun specRepos(configure: Closure<*>) = specRepos {
        ConfigureUtil.configure(configure, this)
    }

    private fun configureRegisteredFrameworks(configure: Framework.() -> Unit) {
        project.multiplatformExtension.supportedTargets().all { target ->
            target.binaries.withType(Framework::class.java) { framework ->
                framework.configure()
                frameworkNameInternal = framework.baseName
                useDynamicFramework = framework.isStatic.not()
                if (useDynamicFramework) {
                    configureLinkingOptions(framework)
                }
            }
        }
    }

    internal fun configureLinkingOptions(binary: NativeBinary, setRPath: Boolean = false) {
        pods.all { pod ->
            binary.linkerOpts("-framework", pod.moduleName)

            binary.linkTaskProvider.configure { task ->

                val podBuildTaskProvider = project.getPodBuildTaskProvider(binary.target, pod)
                task.inputs.file(podBuildTaskProvider.map { it.buildSettingsFile })
                task.dependsOn(podBuildTaskProvider)

                task.doFirst { _ ->
                    val podBuildSettings = project.getPodBuildSettingsProperties(binary.target, pod)
                    binary.linkerOpts.addAll(podBuildSettings.frameworkSearchPaths.map { "-F$it" })
                    if (setRPath) {
                        binary.linkerOpts.addAll(podBuildSettings.frameworkSearchPaths.flatMap { listOf("-rpath", it) })
                    }
                }
            }
        }
    }

    data class CocoapodsDependency(
        private val name: String,
        @get:Input var moduleName: String,
        @get:Optional @get:Input var version: String? = null,
        @get:Optional @get:Nested var source: PodLocation? = null,
        @get:Internal var extraOpts: List<String> = listOf(),
        @get:Internal var packageName: String = "cocoapods.$moduleName"
    ) : Named {
        @Input
        override fun getName(): String = name

        /**
         * Url to archived (tar, jar, zip) pod folder, that should contain the podspec file and all sources required by it.
         *
         * Archive name should match pod name.
         *
         * @param url url to tar, jar or zip archive.
         * @param flatten does archive contains subdirectory that needs to be expanded
         * @param isAllowInsecureProtocol enables communication with a repository over an insecure HTTP connection.
         */
        @JvmOverloads
        fun url(url: String, flatten: Boolean = false, isAllowInsecureProtocol: Boolean = false): PodLocation = Url(URI(url), flatten, isAllowInsecureProtocol)

        /**
         * Path to local pod
         */
        fun path(podspecDirectory: String): PodLocation = Path(File(podspecDirectory))

        /**
         * Path to local pod
         */
        fun path(podspecDirectory: File): PodLocation = Path(podspecDirectory)

        /**
         * Configure pod from git repository. The podspec file is expected to be in the repository root.
         */
        @JvmOverloads
        fun git(url: String, configure: (Git.() -> Unit)? = null): PodLocation {
            val git = Git(URI(url))
            if (configure != null) {
                git.configure()
            }
            return git
        }

        /**
         * Configure pod from git repository. The podspec file is expected to be in the repository root.
         */
        fun git(url: String, configure: Closure<*>) = git(url) {
            ConfigureUtil.configure(configure, this)
        }

        sealed class PodLocation {
            internal abstract fun getLocalPath(project: Project, podName: String): String

            data class Url(
                @get:Input val url: URI,
                @get:Input var flatten: Boolean,
                @get:Input var isAllowInsecureProtocol: Boolean
            ) : PodLocation() {
                override fun getLocalPath(project: Project, podName: String): String {
                    return project.cocoapodsBuildDirs.externalSources("url").resolve(podName).absolutePath
                }
            }

            data class Path(
                @get:InputDirectory
                @get:IgnoreEmptyDirectories
                val dir: File
            ) : PodLocation() {
                override fun getLocalPath(project: Project, podName: String): String {
                    return dir.absolutePath
                }
            }

            data class Git(
                @get:Input val url: URI,
                @get:Input @get:Optional var branch: String? = null,
                @get:Input @get:Optional var tag: String? = null,
                @get:Input @get:Optional var commit: String? = null
            ) : PodLocation() {
                override fun getLocalPath(project: Project, podName: String): String {
                    return project.cocoapodsBuildDirs.externalSources("git").resolve(podName).absolutePath
                }
            }
        }
    }

    data class PodspecPlatformSettings(
        private val name: String,
        @get:Optional @get:Input var deploymentTarget: String? = null
    ) : Named {

        @Input
        override fun getName(): String = name
    }

    class SpecRepos {
        @get:Internal
        internal val specRepos = mutableSetOf("https://cdn.cocoapods.org")

        fun url(url: String) {
            specRepos.add(url)
        }

        @Input
        internal fun getAll(): Collection<String> {
            return specRepos
        }
    }

    companion object {
        private fun String.asModuleName() = this
            .split("/")[0]     // Pick the module name from a subspec name.
            .replace('-', '_') // Support pods with dashes in names (see https://github.com/JetBrains/kotlin-native/issues/2884).
    }
}