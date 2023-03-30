/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis", "PackageDirectoryMismatch") // All tasks should be inherited only by Gradle, Old package for compatibility

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.gradle.plugin.cocoapods.platformLiteral
import org.jetbrains.kotlin.konan.target.Family
import java.io.File

/**
 * The task generates a synthetic project with all cocoapods dependencies
 */
abstract class PodGenTask : CocoapodsTask() {

    init {
        onlyIf {
            pods.get().isNotEmpty()
        }
    }

    @get:InputFile
    internal abstract val podspec: Property<File>

    @get:Input
    internal abstract val podName: Property<String>

    @get:Input
    internal abstract val useLibraries: Property<Boolean>

    @get:Input
    internal abstract val family: Property<Family>

    @get:Nested
    internal abstract val platformSettings: Property<PodspecPlatformSettings>

    @get:Nested
    internal abstract val specRepos: Property<SpecRepos>

    @get:Nested
    internal abstract val pods: ListProperty<CocoapodsDependency>

    @get:OutputFile
    val podfile: Provider<File> = family.map { project.cocoapodsBuildDirs.synthetic(it).resolve("Podfile") }

    @TaskAction
    fun generate() {
        val specRepos = specRepos.get().getAll()

        val podfile = this.podfile.get()
        podfile.createNewFile()

        val podfileContent = getPodfileContent(specRepos, family.get().platformLiteral)
        podfile.writeText(podfileContent)
    }

    private fun getPodfileContent(specRepos: Collection<String>, xcodeTarget: String) =
        buildString {

            specRepos.forEach {
                appendLine("source '$it'")
            }

            appendLine("target '$xcodeTarget' do")
            if (useLibraries.get().not()) {
                appendLine("\tuse_frameworks!")
            }
            val settings = platformSettings.get()
            val deploymentTarget = settings.deploymentTarget
            if (deploymentTarget != null) {
                appendLine("\tplatform :${settings.name}, '$deploymentTarget'")
            } else {
                appendLine("\tplatform :${settings.name}")
            }
            pods.get().mapNotNull {
                buildString {
                    append("pod '${it.name}'")

                    val version = it.version
                    val source = it.source

                    if (source != null) {
                        append(", ${source.getPodSourcePath()}")
                    } else if (version != null) {
                        append(", '$version'")
                    }

                }
            }.forEach { appendLine("\t$it") }
            appendLine("end\n")
            //disable signing for all synthetic pods KT-54314
            append(
                """
                post_install do |installer|
                  installer.pods_project.targets.each do |target|
                    target.build_configurations.each do |config|
                      config.build_settings['EXPANDED_CODE_SIGN_IDENTITY'] = ""
                      config.build_settings['CODE_SIGNING_REQUIRED'] = "NO"
                      config.build_settings['CODE_SIGNING_ALLOWED'] = "NO"
                    end
                  end
                end
                """.trimIndent()
            )
            appendLine()
        }
}
