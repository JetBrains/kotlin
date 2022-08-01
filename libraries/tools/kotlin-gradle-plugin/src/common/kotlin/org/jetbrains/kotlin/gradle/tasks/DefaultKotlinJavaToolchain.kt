/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.toolchain.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.utils.chainedFinalizeValueOnRead
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import java.io.File
import javax.inject.Inject

internal abstract class DefaultKotlinJavaToolchain @Inject constructor(
    private val objects: ObjectFactory,
    projectLayout: ProjectLayout,
    kotlinCompileTaskProvider: () -> KotlinCompile?
) : KotlinJavaToolchain {

    @get:Internal
    internal val currentJvm: Provider<Jvm> = objects
        .property(Jvm.current())
        .chainedFinalizeValueOnRead()

    @get:Internal
    internal val providedJvm: Property<Jvm> = objects
        .propertyWithConvention(currentJvm)
        .chainedFinalizeValueOnRead()

    final override val javaVersion: Provider<JavaVersion> = objects
        .property(
            providedJvm.map { jvm ->
                jvm.javaVersion
                    ?: throw GradleException(
                        "Kotlin could not get java version for the JDK installation: " +
                                jvm.javaHome?.let { "'$it' " }.orEmpty()
                    )
            }
        )
        .chainedFinalizeValueOnRead()

    @get:Internal
    internal val javaExecutable: RegularFileProperty = objects
        .fileProperty()
        .value(
            providedJvm.flatMap { jvm ->
                projectLayout.file(
                    objects.property<File>(
                        jvm.javaExecutable
                            ?: throw GradleException(
                                "Kotlin could not find 'java' executable in the JDK installation: " +
                                        jvm.javaHome?.let { "'$it' " }.orEmpty()
                            )
                    )
                )
            }
        )
        .chainedFinalizeValueOnRead()

    private fun getToolsJarFromJvm(
        jvmProvider: Provider<Jvm>,
        javaVersionProvider: Provider<JavaVersion>
    ): Provider<File?> {
        return objects
            .propertyWithConvention(
                javaVersionProvider.flatMap { javaVersion ->
                    jvmProvider.map { jvm ->
                        jvm.toolsJar.also {
                            if (it == null && javaVersion < JavaVersion.VERSION_1_9) {
                                throw GradleException(
                                    "Kotlin could not find the required JDK tools in the Java installation. " +
                                            "Make sure Kotlin compilation is running on a JDK, not JRE."
                                )
                            }
                        }
                    }
                }
            )
    }

    @get:Internal
    internal val jdkToolsJar: Provider<File?> = getToolsJarFromJvm(providedJvm, javaVersion)

    @get:Internal
    internal val currentJvmJdkToolsJar: Provider<File?> = getToolsJarFromJvm(
        currentJvm,
        currentJvm.map {
            // Current JVM should always have java version
            it.javaVersion!!
        }
    )

    final override val jdk: KotlinJavaToolchain.JdkSetter = DefaultJdkSetter(
        providedJvm,
        objects,
        kotlinCompileTaskProvider
    )

    final override val toolchain: KotlinJavaToolchain.JavaToolchainSetter =
        DefaultJavaToolchainSetter(providedJvm, kotlinCompileTaskProvider)

    private abstract class JvmTargetUpdater(
        private val kotlinCompileTaskProvider: () -> KotlinCompile?
    ) {
        fun updateJvmTarget(
            jdkVersion: Provider<JavaVersion>
        ) {
            kotlinCompileTaskProvider()?.let { task ->
                task.compilerOptions.jvmTarget.convention(
                    jdkVersion.map { version ->
                        // For Java 9 and Java 10 JavaVersion returns "1.9" or "1.10" accordingly
                        // that is not accepted by Kotlin compiler
                        val normalizedVersion = when (version) {
                            JavaVersion.VERSION_1_9 -> "9"
                            JavaVersion.VERSION_1_10 -> "10"
                            else -> version.toString()
                        }
                        JvmTarget.fromTarget(normalizedVersion)
                    }
                )
            }
        }
    }

    private inner class DefaultJdkSetter(
        private val providedJvm: Property<Jvm>,
        private val objects: ObjectFactory,
        kotlinCompileTaskProvider: () -> KotlinCompile?
    ) : JvmTargetUpdater(kotlinCompileTaskProvider),
        KotlinJavaToolchain.JdkSetter {

        override fun use(
            jdkHomeLocation: File,
            jdkVersion: JavaVersion
        ) {
            require(jdkHomeLocation.isDirectory) {
                "Supplied jdkHomeLocation must be a valid directory. You supplied: $jdkHomeLocation"
            }
            require(jdkHomeLocation.exists()) {
                "Supplied jdkHomeLocation does not exist. You supplied: $jdkHomeLocation"
            }

            updateJvmTarget(javaVersion)
            providedJvm.set(
                objects.providerWithLazyConvention {
                    Jvm.discovered(jdkHomeLocation, null, jdkVersion)
                }
            )
        }
    }

    private inner class DefaultJavaToolchainSetter(
        private val providedJvm: Property<Jvm>,
        kotlinCompileTaskProvider: () -> KotlinCompile?
    ) : JvmTargetUpdater(kotlinCompileTaskProvider),
        KotlinJavaToolchain.JavaToolchainSetter {
        override fun use(
            javaLauncher: Provider<JavaLauncher>
        ) {
            updateJvmTarget(javaVersion)
            providedJvm.set(
                javaLauncher.map {
                    val metadata = javaLauncher.get().metadata
                    val javaVersion = JavaVersion.toVersion(metadata.languageVersion.asInt())
                    Jvm.discovered(
                        metadata.installationPath.asFile,
                        null,
                        javaVersion
                    )
                }
            )
        }
    }
}
