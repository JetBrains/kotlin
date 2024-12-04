/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.androidsdkprovisioner

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test

private typealias TaskName = String

abstract class AndroidSdkProvisionerExtension {
    fun Task.requireAsTaskInput(provisioningType: ProvisioningType) {
        getSdkPath(provisioningType)
    }

    fun Test.provideToThisTaskAsSystemProperty(
        provisioningType: ProvisioningType,
        propertyName: String = provisioningType.defaultSystemPropertyName,
    ) {
        val pathProvider = getSdkPath(provisioningType)
        jvmArgumentProviders.add {
            // there's not yet a way to pass Provider to `systemProperty`: https://github.com/gradle/gradle/issues/12247
            listOf("-D$propertyName=${pathProvider.get()}")
        }
    }

    fun JavaExec.provideToThisTaskAsSystemProperty(
        provisioningType: ProvisioningType,
        propertyName: String = provisioningType.defaultSystemPropertyName,
    ) {
        val pathProvider = getSdkPath(provisioningType)
        jvmArgumentProviders.add {
            // there's not yet a way to pass Provider to `systemProperty`: https://github.com/gradle/gradle/issues/12247
            listOf("-D$propertyName=${pathProvider.get()}")
        }
    }

    fun JavaExec.provideToThisTaskAsEnvironmentVariable(provisioningType: ProvisioningType, envVariableName: String = "ANDROID_HOME") {
        val pathProvider = getSdkPath(provisioningType)
        doFirst {
            // there's not yet a way to pass Provider to `environment`
            environment(envVariableName, pathProvider.get())
        }
    }

    fun Project.registerAcceptLicensesTask(acceptLicensesTaskName: TaskName = "acceptAndroidSdkLicenses"): TaskProvider<*> {
        return maybeRegisterAcceptLicensesTask(acceptLicensesTaskName)
    }

    private fun Task.getSdkPath(provisioningType: ProvisioningType): Provider<String> {
        val sdkConfiguration: FileCollection = project.configurations.getByName(provisioningType.configurationName)
        dependsOn(sdkConfiguration)
        val sdkProvider = project.provider { sdkConfiguration.singleFile }
        when (provisioningType.type) {
            ProvisionedFileType.FILE -> inputs.file(sdkProvider)
            ProvisionedFileType.DIRECTORY -> inputs.dir(sdkProvider)
        }
        return sdkProvider.map { it.canonicalPath }
    }

    private fun Project.maybeRegisterAcceptLicensesTask(taskName: String): TaskProvider<*> {
        val task =
            if (taskName in tasks.names)
                tasks.named(taskName)
            else
                tasks.register(taskName) {
                    val androidSdkConfiguration = configurations.getByName(ProvisioningType.SDK.configurationName)
                    val androidSdk = objects.fileProperty().apply { set { androidSdkConfiguration.singleFile } }

                    doFirst {
                        val sdkLicensesDir = androidSdk.get().asFile.resolve("licenses").also {
                            if (!it.exists()) it.mkdirs()
                        }

                        val sdkLicenses = listOf(
                            "8933bad161af4178b1185d1a37fbf41ea5269c55",
                            "d56f5187479451eabf01fb78af6dfcb131a6481e",
                            "24333f8a63b6825ea9c5514f83c2829b004d1fee",
                        )
                        val sdkPreviewLicense = "84831b9409646a918e30573bab4c9c91346d8abd"

                        val sdkLicenseFile = sdkLicensesDir.resolve("android-sdk-license")
                        if (!sdkLicenseFile.exists()) {
                            sdkLicenseFile.createNewFile()
                            sdkLicenseFile.writeText(
                                sdkLicenses.joinToString(separator = System.lineSeparator())
                            )
                        } else {
                            sdkLicenses
                                .subtract(sdkLicenseFile.readText().lines().toSet())
                                .forEach { sdkLicenseFile.appendText("$it${System.lineSeparator()}") }
                        }

                        val sdkPreviewLicenseFile = sdkLicensesDir.resolve("android-sdk-preview-license")
                        if (!sdkPreviewLicenseFile.exists()) {
                            sdkPreviewLicenseFile.writeText(sdkPreviewLicense)
                        } else {
                            if (sdkPreviewLicense != sdkPreviewLicenseFile.readText().trim()) {
                                sdkPreviewLicenseFile.writeText(sdkPreviewLicense)
                            }
                        }
                    }
                }
        return task
    }
}