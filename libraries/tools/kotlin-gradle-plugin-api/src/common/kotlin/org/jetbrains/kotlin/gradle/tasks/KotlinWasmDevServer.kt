/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

/**
 * Represents a development server task for serving Wasm outputs locally.
 *
 * This interface defines properties to configure and manage the behavior of the development server.
 * It is usually implemented as part of the Kotlin Gradle plugin setup to facilitate easier
 * local debugging of web applications.
 *
 * The server serves files from three directories:
 * - [contentDirectory] with the built application, whose files are all available;
 * - the npm root package directory, which contains the project's npm dependencies;
 * - the project root directory, from which only Kotlin sources (`*.kt` files) are available,
 *   so that a browser can show the original sources referenced by source maps.
 *
 * Any other file of the project root directory is not served, as it may contain secrets,
 * and requests are accepted only for local host names and IP addresses.
 */
@ExperimentalWasmDsl
interface KotlinWasmDevServer : Task {
    /**
     * The directory containing the content to be served by the development server.
     *
     * This property specifies where the static files (HTML, CSS, JS, etc.) for the WASM application are located.
     * It is used to configure the root directory from which files will be served during development.
     *
     * All files of this directory are served, as it contains only the produced application artifacts.
     * Additionally, the development server serves Kotlin sources of the project root directory,
     * which are required to make source maps work in a browser.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val contentDirectory: DirectoryProperty

    /**
     * The port number on which the development server will listen.
     *
     * This property allows configuration of the network port for the development server.
     * If not specified, a default port 8080 will be used.
     */
    @get:Input
    @get:Optional
    @get:Option(option = "port", description = "Set a port for the dev server.")
    val port: Property<Int>

    /**
     * The host address on which the development server will listen.
     *
     * This property allows configuration of the network host for the development server.
     * By default, it typically listens on localhost (127.0.0.1) or all interfaces (0.0.0.0).
     */
    @get:Input
    @get:Option(option = "host", description = "Set a host for the dev server.")
    val host: Property<String>
}
