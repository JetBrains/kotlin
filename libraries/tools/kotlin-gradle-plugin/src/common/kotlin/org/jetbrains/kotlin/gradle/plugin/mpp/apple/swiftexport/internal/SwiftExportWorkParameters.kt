/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

internal interface SwiftExportTaskParameters {

    @get:Input
    val bridgeModuleName: Property<String>

    @get:Input
    @get:Optional
    val stableDeclarationsOrder: Property<Boolean>

    @get:Input
    @get:Optional
    val renderDocComments: Property<Boolean>

    @get:Input
    val swiftModules: ListProperty<SwiftExportedModule>

    @get:OutputDirectory
    val outputPath: DirectoryProperty

    @get:OutputFile
    val swiftModulesFile: RegularFileProperty
}