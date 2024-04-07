/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.konan.target.Distribution

internal interface SwiftExportParameters : WorkParameters {

    @get:Input
    val bridgeModuleName: Property<String>

    @get:Input
    val swiftApiModuleName: Property<String>

    @get:Input
    val debugMode: Property<Boolean>

    @get:Input
    val konanDistribution: Property<Distribution>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sourceRoot: DirectoryProperty

    @get:OutputFile
    val swiftApiPath: RegularFileProperty

    @get:OutputFile
    val headerBridgePath: RegularFileProperty

    @get:OutputFile
    val kotlinBridgePath: RegularFileProperty
}