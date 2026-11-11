/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.targets.web.npm.ObsoleteNpmDependencyModel

/**
 * Whether KGP JS and Wasm code avoids configuration that is incompatible with Isolated Projects.
 *
 * Set with the `kotlin.internal.web.isolatedProjectsSupport` property.
 */
internal enum class WebIsolatedProjectsSupport {

    /**
     * JS and Wasm tooling is configured in the tooling project (see `jsToolingProject()`),
     * which is the root project unless per-project npm shared dependencies and Isolated Projects are both enabled.
     *
     * This includes code marked with [ObsoleteNpmDependencyModel] (`RootResolver` npm dependency management),
     * and the Node.js and Yarn plugins applied to the root project.
     *
     * Incompatible with Isolated Projects.
     */
    Off,

    /**
     * Each project configures its own JS and Wasm tooling, and nothing is configured in the root project.
     * No [Off] code (root project configuration) is applied.
     *
     * Per-project tool provisioning (Node.js, D8, Binaryen, Swc, Wasmtime) is compatible with Isolated Projects,
     * so it is still applied.
     *
     * Intended to help test Isolated Projects support KT-80311.
     * Until KT-80311 is complete it is expected that [Preview] will break builds.
     */
    Preview,
}

internal val Project.webIsolatedProjectsSupport: WebIsolatedProjectsSupport
    get() = kotlinPropertiesProvider.webIsolatedProjectsSupport

/**
 * Check [WebIsolatedProjectsSupport] is [Off][WebIsolatedProjectsSupport.Off], before using code marked with [ObsoleteNpmDependencyModel].
 */
internal fun Project.checkWebIsolatedProjectsSupportOff(usage: () -> String) {
    check(webIsolatedProjectsSupport == WebIsolatedProjectsSupport.Off) {
        webIsolatedProjectsSupportOffRequiredMessage(usage())
    }
}

/**
 * Returns [provider] if [WebIsolatedProjectsSupport] is [Off][WebIsolatedProjectsSupport.Off].
 * Otherwise, returns a provider that fails when queried.
 */
internal inline fun <reified T : Any> Project.providerRequiringWebIsolatedProjectsSupportOff(
    usage: String,
    provider: () -> Provider<T>,
): Provider<T> =
    when (webIsolatedProjectsSupport) {
        WebIsolatedProjectsSupport.Off -> provider()
        WebIsolatedProjectsSupport.Preview -> provider { error(webIsolatedProjectsSupportOffRequiredMessage(usage)) }
    }

@PublishedApi
internal fun webIsolatedProjectsSupportOffRequiredMessage(usage: String): String =
    "$usage requires JS and Wasm Isolated Projects support to be ${WebIsolatedProjectsSupport.Off}, " +
            "but it is ${WebIsolatedProjectsSupport.Preview} (KT-80311)."
