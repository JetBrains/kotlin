/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer

/**
 * A plugin DSL extension for configuring common options for the entire project.
 *
 * Use the extension in your build script in the `kotlin` block:
 * ```kotlin
 * kotlin {
 *    // Your extension configuration
 * }
 * ```
 *
 * @since 2.1.0
 */
@Suppress("DEPRECATION")
@Deprecated("Replaced with 'KotlinBaseExtension'", ReplaceWith("KotlinBaseExtension"))
@KotlinGradlePluginDsl
interface KotlinTopLevelExtension : KotlinTopLevelExtensionConfig, KotlinSourceSetContainer {

    /**
     * Configures [Java toolchain](https://docs.gradle.org/current/userguide/toolchains.html)
     * both for Kotlin JVM and Java tasks in the project.
     *
     * @param action - action to configure [JavaToolchainSpec]
     */
    fun jvmToolchain(action: Action<JavaToolchainSpec>)

    /**
     * Configures [Java toolchain](https://docs.gradle.org/current/userguide/toolchains.html)
     * both for Kotlin JVM and Java tasks in the project.
     *
     * @param jdkVersion - JDK version as number. For example, 17 for Java 17.
     */
    fun jvmToolchain(jdkVersion: Int)

    /**
     * Configures Kotlin daemon JVM arguments for all tasks in this project.
     *
     * **Note**: In case other projects are using different JVM arguments,
     * a new instance of Kotlin daemon will be started.
     */
    @ExperimentalKotlinGradlePluginApi
    @get:JvmSynthetic
    var kotlinDaemonJvmArgs: List<String>

    /**
     * The version of the Kotlin compiler.
     *
     * By default, the Kotlin Build Tools API implementation of the same version as the KGP is used.
     *
     * Be careful with reading the property's value as eager reading will finalize the value and prevent it from being configured.
     *
     * Note: Currently only has an effect if the `kotlin.compiler.runViaBuildToolsApi` Gradle property is set to `true`.
     */
    @ExperimentalKotlinGradlePluginApi
    @ExperimentalBuildToolsApi
    val compilerVersion: Property<String>

    /**
     * Can be used to configure objects that are not yet created, or will be created in
     * 'afterEvaluate' (for example, typically for Android source sets containing flavors and buildTypes).
     *
     * Will fail project evaluation if the domain object is not created before 'afterEvaluate' listeners in the buildscript.
     *
     * @param configure Called inline if the value is already present. Called once the domain object is created.
     */
    @ExperimentalKotlinGradlePluginApi
    fun <T : Named> NamedDomainObjectContainer<T>.invokeWhenCreated(
        name: String,
        configure: T.() -> Unit
    )
}