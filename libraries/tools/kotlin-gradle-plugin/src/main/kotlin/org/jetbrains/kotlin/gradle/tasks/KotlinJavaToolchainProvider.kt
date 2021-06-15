/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.toolchain.*
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJavaToolchain.Companion.TOOLCHAIN_SUPPORTED_VERSION
import org.jetbrains.kotlin.gradle.utils.chainedFinalizeValueOnRead
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention
import java.io.File
import javax.inject.Inject
import kotlin.reflect.full.functions

abstract class KotlinJavaToolchainProvider @Inject constructor(
    private val objects: ObjectFactory,
    projectLayout: ProjectLayout,
    gradle: Gradle
) : KotlinJavaToolchain {

    private val currentGradleVersion = GradleVersion.version(gradle.gradleVersion)

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

    private fun getToolsJarFromJvm(jvmProvider: Provider<Jvm>): Provider<File?> {
        return objects
            .propertyWithConvention(
                jvmProvider.flatMap { jvm ->
                    objects.propertyWithConvention(jvm.toolsJar)
                }
            )
            .orElse(javaVersion.flatMap {
                if (it < JavaVersion.VERSION_1_9) {
                    throw GradleException(
                        "Kotlin could not find the required JDK tools in the Java installation. " +
                                "Make sure Kotlin compilation is running on a JDK, not JRE."
                    )
                } else {
                    objects.propertyWithConvention<File?>(null)
                }
            })
    }

    @get:Internal
    internal val jdkToolsJar: Provider<File?> = getToolsJarFromJvm(providedJvm)

    @get:Internal
    internal val currentJvmJdkToolsJar: Provider<File?> = getToolsJarFromJvm(currentJvm)

    final override val jdk: KotlinJavaToolchain.JdkSetter = DefaultJdkSetter(providedJvm, currentGradleVersion)

    private val defaultJavaToolchainSetter by lazy(LazyThreadSafetyMode.NONE) {
        if (currentGradleVersion >= TOOLCHAIN_SUPPORTED_VERSION) {
            DefaultJavaToolchainSetter(providedJvm)
        } else {
            null
        }
    }

    final override val toolchain: KotlinJavaToolchain.JavaToolchainSetter
        get() = defaultJavaToolchainSetter
            ?: throw GradleException("Toolchain support is available from $TOOLCHAIN_SUPPORTED_VERSION")

    private class DefaultJdkSetter(
        private val providedJvm: Property<Jvm>,
        private val currentGradleVersion: GradleVersion
    ) : KotlinJavaToolchain.JdkSetter {

        override fun use(
            jdkHomeLocation: File,
            jdkVersion: JavaVersion
        ) {
            require(jdkHomeLocation.isDirectory) {
                "Supplied jdkHomeLocation must be a valid directory. You supplied: $jdkHomeLocation"
            }
            require(jdkHomeLocation.exists()) {
                "Supplied jdkHomeLocation does not exists. You supplied: $jdkHomeLocation"
            }

            if (currentGradleVersion < GradleVersion.version("6.2.0")) {
                // Before Gradle 6.2.0 'Jvm.discovered' does not have 'implementationJavaVersion' parameter
                val jvm = Jvm::class.functions
                    .first { it.name == "discovered" }
                    .call(jdkHomeLocation, jdkVersion) as Jvm
                providedJvm.set(jvm)
            } else {
                providedJvm.set(
                    Jvm.discovered(jdkHomeLocation, null, jdkVersion)
                )
            }
        }
    }

    private inner class DefaultJavaToolchainSetter(
        private val providedJvm: Property<Jvm>
    ) : KotlinJavaToolchain.JavaToolchainSetter {
        override fun use(
            javaLauncher: Provider<JavaLauncher>
        ) {
            providedJvm.set(
                javaLauncher.map {
                    val metadata = javaLauncher.get().metadata
                    Jvm.discovered(
                        metadata.installationPath.asFile,
                        null,
                        JavaVersion.toVersion(metadata.languageVersion.asInt())
                    )
                }
            )
        }
    }
}