/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility

package org.jetbrains.kotlin.gradle.targets.native.tasks

import java.io.File
import java.io.Reader
import java.util.*

data class PodBuildSettingsProperties(
    internal val buildDir: String,
    internal val configuration: String,
    val configurationBuildDir: String,
    internal val podsTargetSrcRoot: String,
    internal val cflags: String? = null,
    internal val headerPaths: String? = null,
    internal val publicHeadersFolderPath: String? = null,
    internal val frameworkPaths: String? = null
) {

    fun writeSettings(
        buildSettingsFile: File
    ) {
        buildSettingsFile.parentFile.mkdirs()
        buildSettingsFile.delete()
        buildSettingsFile.createNewFile()

        check(buildSettingsFile.exists()) { "Unable to create file ${buildSettingsFile.path}!" }

        with(buildSettingsFile) {
            appendText("$BUILD_DIR=$buildDir\n")
            appendText("$CONFIGURATION=$configuration\n")
            appendText("$CONFIGURATION_BUILD_DIR=$configurationBuildDir\n")
            appendText("$PODS_TARGET_SRCROOT=$podsTargetSrcRoot\n")
            cflags?.let { appendText("$OTHER_CFLAGS=$it\n") }
            headerPaths?.let { appendText("$HEADER_SEARCH_PATHS=$it\n") }
            publicHeadersFolderPath?.let { appendText("$PUBLIC_HEADERS_FOLDER_PATH=$it\n") }
            frameworkPaths?.let { appendText("$FRAMEWORK_SEARCH_PATHS=$it") }
        }
    }

    companion object {
        const val BUILD_DIR = "BUILD_DIR"
        const val CONFIGURATION = "CONFIGURATION"
        const val CONFIGURATION_BUILD_DIR = "CONFIGURATION_BUILD_DIR"
        const val PODS_TARGET_SRCROOT = "PODS_TARGET_SRCROOT"
        const val OTHER_CFLAGS = "OTHER_CFLAGS"
        const val HEADER_SEARCH_PATHS = "HEADER_SEARCH_PATHS"
        const val PUBLIC_HEADERS_FOLDER_PATH = "PUBLIC_HEADERS_FOLDER_PATH"
        const val FRAMEWORK_SEARCH_PATHS = "FRAMEWORK_SEARCH_PATHS"

        internal fun readSettingsFromFile(file: File): PodBuildSettingsProperties {
            return file.reader().use { readSettingsFromReader(it) }
        }

        fun readSettingsFromReader(reader: Reader): PodBuildSettingsProperties {
            with(Properties()) {
                load(reader)
                return PodBuildSettingsProperties(
                    readProperty(BUILD_DIR),
                    readProperty(CONFIGURATION),
                    readProperty(CONFIGURATION_BUILD_DIR),
                    readProperty(PODS_TARGET_SRCROOT),
                    readNullableProperty(OTHER_CFLAGS),
                    readNullableProperty(HEADER_SEARCH_PATHS),
                    readNullableProperty(PUBLIC_HEADERS_FOLDER_PATH),
                    readNullableProperty(FRAMEWORK_SEARCH_PATHS)
                )
            }
        }

        private fun Properties.readProperty(propertyName: String) =
            readNullableProperty(propertyName) ?: error("$propertyName property is absent")

        private fun Properties.readNullableProperty(propertyName: String) =
            getProperty(propertyName)
    }
}
