/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.build.androidsdkprovisioner.AndroidSdkProvisionerExtension
import org.jetbrains.kotlin.build.androidsdkprovisioner.ProvisioningType

@Suppress("unused")
class JpsCompatiblePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.configurations.maybeCreate(EmbeddedComponents.CONFIGURATION_NAME)
        project.extensions.create("pill", PillExtension::class.java)

        // 'jpsTest' does not require the 'tests-jar' artifact
        project.configurations.create("jpsTest")

        if (project == project.rootProject) {
            val androidSdkRequired = System.getProperty("pill.android.tests", "false") == "true"
            if (androidSdkRequired) {
                project.plugins.apply("android-sdk-provisioner")
            }
            project.tasks.register("pill") {
                dependsOn(":pill:pill-importer:pill")

                if (androidSdkRequired) {
                    project.extensions.configure(AndroidSdkProvisionerExtension::class.java) {
                        requireAsTaskInput(ProvisioningType.SDK)
                        requireAsTaskInput(ProvisioningType.PLATFORM_JAR)
                    }
                }
            }

            project.tasks.register("unpill") {
                dependsOn(":pill:pill-importer:unpill")
            }
        }
    }
}