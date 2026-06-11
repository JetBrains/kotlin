/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

interface KotlinWasmDevServer : Task {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val contentDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    @get:Option(option = "port", description = "Set a port for the dev server.")
    val port: Property<Int>

    @get:Input
    @get:Option(option = "host", description = "Set a host for the dev server.")
    val host: Property<String>
}
