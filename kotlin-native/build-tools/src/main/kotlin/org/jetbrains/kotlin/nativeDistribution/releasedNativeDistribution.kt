/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nativeDistribution

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.konan.target.HostManager

private enum class ArchiveType(val extension: String) {
    TAR_GZ("tar.gz"),
    ZIP("zip");

    override fun toString() = extension

    companion object {
        val HOST_DEFAULT = if (HostManager.hostIsMingw) ZIP else TAR_GZ
    }
}

fun Project.releasedNativeDistributionConfiguration(version: String): Configuration {
    val name = "releasedNativeDistributionV${version}"
    return try {
        configurations.create(name) {
            isTransitive = false
            isCanBeConsumed = false
            isCanBeResolved = true
        }.also {
            dependencies {
                // declared to be included in verification-metadata.xml
                "implicitDependencies"("org.jetbrains.kotlin:kotlin-native-prebuilt:$version:macos-aarch64@tar.gz")
                "implicitDependencies"("org.jetbrains.kotlin:kotlin-native-prebuilt:$version:macos-x86_64@tar.gz")
                "implicitDependencies"("org.jetbrains.kotlin:kotlin-native-prebuilt:$version:linux-x86_64@tar.gz")
                "implicitDependencies"("org.jetbrains.kotlin:kotlin-native-prebuilt:$version:windows-x86_64@zip")
                it("org.jetbrains.kotlin:kotlin-native-prebuilt:$version:${HostManager.platformName()}@${ArchiveType.HOST_DEFAULT}")
            }
        }
    } catch (_: InvalidUserDataException) {
        configurations.getByName(name)
    }
}
