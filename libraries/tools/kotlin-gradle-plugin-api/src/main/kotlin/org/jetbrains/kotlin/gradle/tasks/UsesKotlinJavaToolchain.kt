/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.JavaVersion
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import java.io.File

interface UsesKotlinJavaToolchain {
    @get:Nested
    val kotlinJavaToolchainProvider: Provider<out KotlinJavaToolchain>

    @get:Internal
    val kotlinJavaToolchain: KotlinJavaToolchain
        get() = kotlinJavaToolchainProvider.get()
}

interface KotlinJavaToolchain {
    @get:Input
    val javaVersion: Provider<JavaVersion>

    /**
     * Set JDK to use for Kotlin compilation.
     *
     * Major JDK version is considered as compile task input.
     *
     * @param jdkHomeLocation path to JDK.
     * *Note*: project build will fail on providing here JRE instead of JDK!
     * @param jdkVersion provided JDK version
     */
    fun setJdkHome(
        jdkHomeLocation: File,
        jdkVersion: JavaVersion
    )

    /**
     * Set JDK to use for Kotlin compilation.
     *
     * @see [setJdkHome]
     */
    fun setJdkHome(
        jdkHomeLocation: String,
        jdkVersion: JavaVersion
    ) = setJdkHome(File(jdkHomeLocation), jdkVersion)
}
