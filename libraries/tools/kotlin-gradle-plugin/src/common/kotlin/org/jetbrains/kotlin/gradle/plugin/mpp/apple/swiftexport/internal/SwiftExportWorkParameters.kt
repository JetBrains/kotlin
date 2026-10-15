/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.konan.target.KonanTarget

internal interface SwiftExportTaskParameters {

    @get:Input
    val bridgeModuleName: Property<String>

    @get:Input
    @get:Optional
    val stableDeclarationsOrder: Property<Boolean>

    @get:Input
    @get:Optional
    val swiftExportSettings: MapProperty<String, String>

    @get:Input
    val konanTarget: Property<KonanTarget>

    /**
     * The directory of the run, with [outputPath], [swiftModulesFile] and the support modules Swift Export writes next
     * to them. Emptied before every run.
     */
    @get:OutputDirectory
    val outputDirectory: DirectoryProperty
}

// Extensions, not interface members: Gradle generates the implementations, and KGP is compiled without JVM default
// methods.

/** The generated Swift and Kotlin sources. */
internal val SwiftExportTaskParameters.outputPath: Provider<Directory>
    get() = outputDirectory.dir("files")

internal val SwiftExportTaskParameters.swiftModulesFile: Provider<RegularFile>
    get() = outputDirectory.file("modules.json")
