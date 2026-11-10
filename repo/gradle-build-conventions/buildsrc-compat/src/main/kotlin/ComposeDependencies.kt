import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import java.net.URI

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
private val Project.composeSnapshotVersionCatalog: VersionCatalog
    get() = project.extensions.getByType(VersionCatalogsExtension::class.java).find("composeRuntimeSnapshot").get()
private val Project.libsVersionCatalog: VersionCatalog
    get() = project.extensions.getByType(VersionCatalogsExtension::class.java).find("libs").get()
private fun Project.composeStableVersion() = libsVersionCatalog.findVersion("compose.stable").get().requiredVersion
private fun Project.composeRuntimeVersion() = composeSnapshotVersionCatalog.findVersion("runtime.version").get().requiredVersion

val Project.androidXMavenLocalPath: String?
    get() = kotlinBuildProperties.stringProperty("compose.aosp.root").orNull

fun RepositoryHandler.androidXMavenLocal(androidXMavenLocalPath: String?) {
    if (androidXMavenLocalPath != null) {
        maven("$androidXMavenLocalPath/out/dist/repository/")
    }
}

fun RepositoryHandler.composeGoogleMaven(composeStableVersion: String) {
    google {
        content {
            includeGroup("androidx.collection")
            includeGroup("androidx.compose.runtime")
            includeGroup("androidx.annotation")
            includeVersion("androidx.compose.foundation", "foundation-layout", composeStableVersion)
            includeVersion("androidx.compose.foundation", "foundation-layout-desktop", composeStableVersion)
            includeVersion("androidx.compose.foundation", "foundation", composeStableVersion)
            includeVersion("androidx.compose.foundation", "foundation-desktop", composeStableVersion)
            includeVersion("androidx.compose.animation", "animation", composeStableVersion)
            includeVersion("androidx.compose.animation", "animation-desktop", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-desktop", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-graphics", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-graphics-desktop", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-text", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-text-desktop", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-unit", composeStableVersion)
            includeVersion("androidx.compose.ui", "ui-unit-desktop", composeStableVersion)
        }
    }
}

fun Project.composeRuntime() = compose("runtime", "runtime", composeRuntimeVersion())
fun Project.composeRuntimeAnnotations() = compose("runtime", "runtime-annotation", composeRuntimeVersion())
fun Project.composeRuntimeAnnotationsJs() = compose("runtime", "runtime-annotation-js", composeRuntimeVersion())
fun Project.composeRuntimeAnnotationsJvm() = compose("runtime", "runtime-annotation-jvm", composeRuntimeVersion())
fun Project.composeRuntimeJs() = compose("runtime", "runtime-js", composeRuntimeVersion())
fun Project.composeRuntimeDesktop() = compose("runtime", "runtime-desktop", composeRuntimeVersion())
fun Project.compose(group: String, module: String, version: String = composeStableVersion()) =
    "androidx.compose.$group:$module:$version"

/**
 * Hosts that run Compose compiler Kotlin/Native tests on CI.
 * Compose runtime is not published for macosX64.
 */
private val composeNativeTestHosts = listOf("linuxx64", "macosarm64", "iossimulatorarm64", "mingwx64")

/**
 * Adds implicit dependencies on the host-specific klibs of a Kotlin Multiplatform library [notation] (`group:name:version`)
 * for each of [composeNativeTestHosts], so that they are included in dependency verification metadata.
 */
fun Project.composeNativeImplicitDependency(notation: String) {
    val (group, name, version) = notation.split(":")
    composeNativeTestHosts.forEach { host ->
        dependencies.add("implicitDependencies", "$group:$name-$host:$version") {
            isTransitive = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
            }
        }
    }
}
