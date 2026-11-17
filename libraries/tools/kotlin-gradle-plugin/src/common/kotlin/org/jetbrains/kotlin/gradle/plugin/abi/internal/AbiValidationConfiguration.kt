/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi.internal

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApiKind

/**
 * Configuration for content and behaviour of ABI validation.
 *
 * @since 2.5.0
 */
@DelicateKotlinGradlePluginApi(DelicateKotlinGradlePluginApiKind.ABI_VALIDATION)
public interface AbiValidationConfiguration {

    /**
     * Adds a single JVM dump.
     */
    @DelicateKotlinGradlePluginApi(DelicateKotlinGradlePluginApiKind.ABI_VALIDATION)
    public fun addJvmDump(output: FileCollection)

    /**
     * Adds a named JVM dump.
     */
    @DelicateKotlinGradlePluginApi(DelicateKotlinGradlePluginApiKind.ABI_VALIDATION)
    public fun addJvmDump(name: String, output: FileCollection)

    /**
     * Adds a target for a klib dump.
     */
    @DelicateKotlinGradlePluginApi(DelicateKotlinGradlePluginApiKind.ABI_VALIDATION)
    public fun addKlibTarget(target: KlibTargetId, output: FileCollection)

    /**
     * Infers content of targets specified via [unsupportedTargets] based on declaration passed via [addKlibTarget].
     */
    @DelicateKotlinGradlePluginApi(DelicateKotlinGradlePluginApiKind.ABI_VALIDATION)
    public val keepLocallyUnsupportedTargets: Property<Boolean>

    /**
     * Specifies whether dumps should be automatically filled from Kotlin Plugin configuration.
     */
    @DelicateKotlinGradlePluginApi(DelicateKotlinGradlePluginApiKind.ABI_VALIDATION)
    public val useAutoconfigure: Property<Boolean>

    /**
     * Specifies target for which targets ABI declaration should be inferred but these declarations was not passed via [addKlibTarget].
     */
    @DelicateKotlinGradlePluginApi(DelicateKotlinGradlePluginApiKind.ABI_VALIDATION)
    public val unsupportedTargets: SetProperty<KlibTargetId>
}
