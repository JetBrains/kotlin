/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.mpp.resources.unzip
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.FileInputStream
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.assertEquals
import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.ArchiveUklibTask
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.diagnostics.UklibFragmentsChecker

@ExperimentalKotlinGradlePluginApi
@MppGradlePluginTests
@DisplayName("Smoke test uklib artifact publication")
class UklibPublicationITWIP : KGPBaseTest() {

    @GradleTest
    fun `test`(
        gradleVersion: GradleVersion,
    ) {
        val res = project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosX64()
                    macosArm64()
                    macosX64()
                    // sourceSets.macosMain.get().compileSource("expect fun foo()")
                    applyHierarchyTemplate {
                        group("a") {
                            withIosArm64()
                            withIosX64()
                        }
                        group("b") {
                            withMacosArm64()
                            withMacosX64()
                        }
                    }
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "transitive"))
        println(res)
    }

}