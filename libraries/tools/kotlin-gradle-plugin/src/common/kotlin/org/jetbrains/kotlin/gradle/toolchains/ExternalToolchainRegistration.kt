/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExternalToolchainDsl::class)

package org.jetbrains.kotlin.gradle.toolchains

import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl

/**
 * How a tool is provisioned for the build, selected by the `<tool>ToolchainMode` Gradle property.
 */
internal enum class ExternalToolchainMode {

    /**
     * The tool distribution is downloaded and installed by the download-based toolchain service.
     */
    DOWNLOAD,

    /**
     * A tool already installed on the machine is used, and nothing is downloaded.
     */
    PREINSTALLED,

    /**
     * The toolchain service is not used - the tool is provisioned per project by its environment spec
     * and setup task, which is the behaviour of the Kotlin Gradle Plugin before the toolchain services.
     */
    DISABLE,
    ;
}

/**
 * The name the toolchain service of [serviceType] is registered under.
 *
 * It includes the class loader hash, so that several Kotlin Gradle Plugin versions loaded in the same build
 * do not clash on the same registration.
 */
internal fun externalToolchainServiceName(serviceType: Class<out BuildService<*>>): String =
    "${serviceType.name}_${serviceType.classLoader.hashCode()}"

/**
 * Returns the toolchain service registered for the build under [serviceName], if any.
 */
@Suppress("UNCHECKED_CAST")
internal fun <S : BuildService<*>> Gradle.findExternalToolchainService(serviceName: String): Provider<out S>? =
    sharedServices.registrations.findByName(serviceName)?.service as Provider<out S>?

internal fun <S : BuildService<*>> Gradle.findExternalToolchainService(serviceType: Class<S>): Provider<out S>? =
    findExternalToolchainService(externalToolchainServiceName(serviceType))

/**
 * Registers [implementation] as the toolchain service of the build under [serviceName], unless one is already registered.
 */
@Suppress("UNCHECKED_CAST")
internal fun <S : BuildService<*>, P : BuildServiceParameters> Gradle.registerExternalToolchainService(
    serviceName: String,
    implementation: Class<out BuildService<*>>,
    configure: (P) -> Unit,
): Provider<out S> = sharedServices.registerIfAbsent(
    serviceName,
    implementation as Class<BuildService<BuildServiceParameters>>,
) { spec ->
    configure(spec.parameters as P)
} as Provider<out S>
