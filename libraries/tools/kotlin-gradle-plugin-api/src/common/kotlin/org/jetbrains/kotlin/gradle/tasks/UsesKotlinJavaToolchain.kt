/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.JavaVersion
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.jvm.toolchain.JavaLauncher
import java.io.File

/**
 * Represents a Kotlin task using the [Gradle toolchains for JVM projects](https://docs.gradle.org/current/userguide/toolchains.html)
 * feature inside its [TaskAction](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/TaskAction.html).
 *
 * The Gradle toolchains for JVM projects feature is used by our plugin to compile Kotlin/JVM.
 *
 * Use this interface to configure different tasks to use different JDK versions via
 * [Gradle's tasks API](https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:configuring_tasks).
 */
interface UsesKotlinJavaToolchain : Task {

    /**
     * Kotlin task configured JVM toolchain.
     *
     * This variable always has a value.
     */
    @get:Nested
    val kotlinJavaToolchainProvider: Provider<out KotlinJavaToolchain>

    /**
     * A helper shortcut to get [KotlinJavaToolchain] from [kotlinJavaToolchainProvider] without calling the `.get()` method.
     */
    @get:Internal
    val kotlinJavaToolchain: KotlinJavaToolchain
        get() = kotlinJavaToolchainProvider.get()
}

/**
 * The Kotlin JVM toolchain.
 *
 * This interface provides ways to configure the JDK either via [JdkSetter] by providing a path to JDK directly, or
 * via [JavaToolchainSetter] using the configured [JavaLauncher].
 *
 * The configured JDK Java version is exposed as a task input so that Gradle only reuses the task outputs stored in the
 * [build cache](https://docs.gradle.org/current/userguide/build_cache.html) with the same JDK version.
 */
interface KotlinJavaToolchain {

    /**
     * The configured JVM toolchain [JavaVersion].
     *
     * This property represents the configured JVM toolchain [JavaVersion] used for the task.
     * If the toolchain is not explicitly set, it defaults to the version of the JDK that Gradle is currently running.
     */
    @get:Input
    val javaVersion: Provider<JavaVersion>

    /**
     * Provides access to the [JdkSetter] to configure the JVM toolchain for the task using an explicit JDK location.
     */
    @get:Internal
    val jdk: JdkSetter

    /**
     * Provides access to the [JavaToolchainSetter] to configure JVM toolchain for the task
     * using the [Gradle JVM toolchain](https://docs.gradle.org/current/userguide/toolchains.html).
     */
    @get:Internal
    val toolchain: JavaToolchainSetter

    /**
     * Provides methods to configure the task using an explicit JDK location.
     */
    interface JdkSetter {
        /**
         * Configures the JVM toolchain to use the JDK located under the [jdkHomeLocation] absolute path.
         * The major JDK version from [javaVersion] is used as a task input so that Gradle avoids using task outputs
         * in the [build cache](https://docs.gradle.org/current/userguide/build_cache.html) that use different JDK versions.
         *
         * **Note**: The project build fails if the JRE version instead of the JDK version is provided.
         *
         * @param jdkHomeLocation The path to the JDK location on the machine
         * @param jdkVersion The JDK version located in the configured [jdkHomeLocation] path
         */
        fun use(
            jdkHomeLocation: File,
            jdkVersion: JavaVersion
        )

        /**
         * Configures the JVM toolchain to use the JDK located under the [jdkHomeLocation] absolute path.
         * The major JDK version from [javaVersion] is used as a task input so that Gradle avoids using task outputs
         * in the [build cache](https://docs.gradle.org/current/userguide/build_cache.html) that use different JDK versions.
         *
         * **Note**: The project build fails if the JRE version instead of the JDK version is provided.
         *
         * @param jdkHomeLocation The path to the JDK location on the machine
         * @param jdkVersion JDK version located in the configured [jdkHomeLocation] path.
         * Accepts any type that is accepted by [JavaVersion.toVersion].
         * @throws IllegalArgumentException if the given [jdkVersion] value cannot be converted
         */
        fun use(
            jdkHomeLocation: String,
            jdkVersion: Any
        ) = use(File(jdkHomeLocation), JavaVersion.toVersion(jdkVersion))
    }

    /**
     * Provides methods to configure the task using the [Gradle JVM toolchain](https://docs.gradle.org/current/userguide/toolchains.html).
     */
    interface JavaToolchainSetter {
        /**
         * Configures the JVM toolchain for a task using the [JavaLauncher] obtained from [org.gradle.jvm.toolchain.JavaToolchainService] via
         * the [org.gradle.api.plugins.JavaPluginExtension] extension.
         */
        fun use(
            javaLauncher: Provider<JavaLauncher>
        )
    }
}
