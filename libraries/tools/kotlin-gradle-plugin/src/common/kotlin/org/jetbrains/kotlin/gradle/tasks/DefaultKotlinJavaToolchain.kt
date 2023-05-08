/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.toolchain.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.chainedFinalizeValueOnRead
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import java.io.File
import javax.inject.Inject

internal abstract class DefaultKotlinJavaToolchain @Inject constructor(
    private val objects: ObjectFactory,
    projectLayout: ProjectLayout,
    jvmCompilerOptions: () -> KotlinJvmCompilerOptions?
) : KotlinJavaToolchain {

    @get:Internal
    internal val gradleJvm: Provider<Jvm> = objects
        .property(Jvm.current())
        .chainedDisallowChanges()
        .chainedFinalizeValueOnRead()

    @get:Internal
    internal val providedJvm: Property<Jvm> = objects
        .property<Jvm>()
        .chainedFinalizeValueOnRead()

    @get:Internal
    internal val buildJvm: Provider<Jvm> = objects
        .property(providedJvm.orElse(gradleJvm))
        .chainedDisallowChanges()
        .chainedFinalizeValueOnRead()

    final override val javaVersion: Provider<JavaVersion> = objects
        .property(
            buildJvm
                .map { jvm ->
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
            buildJvm.flatMap { jvm ->
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
    internal val jdkToolsJar: Provider<File?> = getToolsJarFromJvm(buildJvm, javaVersion)

    @get:Internal
    internal val currentJvmJdkToolsJar: Provider<File?> = getToolsJarFromJvm(
        gradleJvm,
        gradleJvm.map {
            // Current JVM should always have java version
            it.javaVersion!!
        }
    )

    final override val jdk: KotlinJavaToolchain.JdkSetter = DefaultJdkSetter(
        providedJvm,
        objects,
        jvmCompilerOptions
    )

    final override val toolchain: KotlinJavaToolchain.JavaToolchainSetter =
        DefaultJavaToolchainSetter(
            providedJvm,
            jvmCompilerOptions
        )

    private class DefaultJdkSetter(
        private val providedJvm: Property<Jvm>,
        private val objects: ObjectFactory,
        private val jvmCompilerOptions: () -> KotlinJvmCompilerOptions?
    ) : KotlinJavaToolchain.JdkSetter {

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

            providedJvm.set(
                objects.providerWithLazyConvention {
                    Jvm.discovered(jdkHomeLocation, null, jdkVersion)
                }
            )

            jvmCompilerOptions()?.let {
                wireJvmTargetToJvm(it, providedJvm)
            }
        }
    }

    internal class DefaultJavaToolchainSetter(
        private val providedJvm: Property<Jvm>,
        private val jvmCompilerOptions: () -> KotlinJvmCompilerOptions?
    ) : KotlinJavaToolchain.JavaToolchainSetter {

        internal fun useAsConvention(
            javaLauncher: Provider<JavaLauncher>
        ) {
            providedJvm.convention(javaLauncher.map(::mapToJvm))
        }

        override fun use(
            javaLauncher: Provider<JavaLauncher>
        ) {
            providedJvm.set(javaLauncher.map(::mapToJvm))

            jvmCompilerOptions()?.let {
                wireJvmTargetToJvm(it, providedJvm)
            }
        }
    }

    companion object {

        internal fun wireJvmTargetToJvm(
            jvmCompilerOptions: KotlinJvmCompilerOptions,
            toolchainJvm: Provider<Jvm>
        ) {
            jvmCompilerOptions.jvmTarget.convention(
                toolchainJvm.map { jvm ->
                    // For Java 9 and Java 10 JavaVersion returns "1.9" or "1.10" accordingly
                    // that is not accepted by Kotlin compiler
                    val normalizedVersion = when (jvm.javaVersion) {
                        JavaVersion.VERSION_1_9 -> "9"
                        JavaVersion.VERSION_1_10 -> "10"
                        else -> jvm.javaVersion.toString()
                    }
                    JvmTarget.fromTarget(normalizedVersion)
                }.orElse(JvmTarget.DEFAULT)
            )
        }

        private fun wireJvmTargetToToolchain(
            jvmCompilerOptions: KotlinJvmCompilerOptions,
            javaLauncher: Provider<JavaLauncher>
        ): Unit = wireJvmTargetToJvm(
            jvmCompilerOptions,
            javaLauncher.map(::mapToJvm)
        )

        internal fun wireJvmTargetToToolchain(
            compilerOptions: KotlinJvmCompilerOptions,
            project: Project,
        ) {
            project.plugins.withId("org.gradle.java-base") {
                val toolchainService = project.extensions.findByType(JavaToolchainService::class.java)
                    ?: error("Gradle JavaToolchainService is not available!")
                val toolchainSpec = project.extensions
                    .getByType(JavaPluginExtension::class.java)
                    .toolchain
                val javaLauncher = toolchainService.launcherFor(toolchainSpec)
                wireJvmTargetToToolchain(compilerOptions, javaLauncher)
            }
        }

        private fun mapToJvm(javaLauncher: JavaLauncher): Jvm {
            val metadata = javaLauncher.metadata
            val javaVersion = JavaVersion.toVersion(metadata.languageVersion.asInt())
            return Jvm.discovered(
                metadata.installationPath.asFile,
                null,
                javaVersion
            )
        }
    }
}
