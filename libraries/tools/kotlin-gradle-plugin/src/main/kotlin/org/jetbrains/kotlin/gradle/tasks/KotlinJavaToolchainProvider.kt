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

abstract class KotlinJavaToolchainProvider @Inject constructor(
    objects: ObjectFactory,
    projectLayout: ProjectLayout,
    gradle: Gradle
) : KotlinJavaToolchain {

    private val defaultJdkSetter by lazy(LazyThreadSafetyMode.NONE) {
        DefaultJdkSetter(
            objects,
            projectLayout,
            GradleVersion.version(gradle.gradleVersion)
        )
    }

    private val defaultJavaToolchainSetter by lazy(LazyThreadSafetyMode.NONE) {
        if (GradleVersion.version(gradle.gradleVersion) >= TOOLCHAIN_SUPPORTED_VERSION) {
            DefaultJavaToolchainSetter(objects)
        } else {
            null
        }
    }

    @get:Internal
    internal val jdkProvider: JdkProvider =
        if (GradleVersion.version(gradle.gradleVersion) < TOOLCHAIN_SUPPORTED_VERSION) {
            defaultJdkSetter
        } else {
            defaultJavaToolchainSetter!!
        }

    final override val javaVersion: Provider<JavaVersion>
        get() = jdkProvider.javaVersion

    final override val jdk: KotlinJavaToolchain.JdkSetter get() = defaultJdkSetter

    final override val toolchain: KotlinJavaToolchain.JavaToolchainSetter
        get() = defaultJavaToolchainSetter
            ?: throw GradleException("Toolchain support is available from $TOOLCHAIN_SUPPORTED_VERSION")

    internal interface JdkProvider {
        val currentJvm: Property<Jvm>
        val javaExecutable: RegularFileProperty
        val javaVersion: Property<JavaVersion>
        val jdkToolsJar: Provider<File?>

        companion object {
            fun jdkToolsProperty(
                objectsFactory: ObjectFactory,
                currentJvm: Provider<Jvm>
            ): Property<File?> = objectsFactory
                .propertyWithConvention(
                    currentJvm.flatMap { jvm ->
                        objectsFactory.propertyWithConvention(jvm.toolsJar)
                    }
                )

            fun defaultJdkToolsJarProvider(
                objectsFactory: ObjectFactory,
                javaVersion: Property<JavaVersion>,
                jdkTools: Property<File?>
            ): Provider<File?> = jdkTools
                .orElse(javaVersion.flatMap {
                    if (it < JavaVersion.VERSION_1_9) {
                        throw GradleException(
                            "Kotlin could not find the required JDK tools in the Java installation. " +
                                    "Make sure Kotlin compilation is running on a JDK, not JRE."
                        )
                    } else {
                        objectsFactory.propertyWithConvention<File?>(null)
                    }
                })
        }
    }

    private class DefaultJdkSetter(
        private val objectsFactory: ObjectFactory,
        projectLayout: ProjectLayout,
        private val currentGradleVersion: GradleVersion
    ) : KotlinJavaToolchain.JdkSetter,
        JdkProvider {

        override val currentJvm: Property<Jvm> = objectsFactory
            .property(Jvm.current())
            .chainedFinalizeValueOnRead()

        override val javaExecutable: RegularFileProperty = objectsFactory
            .fileProperty()
            .convention(
                currentJvm.flatMap { jvm ->
                    projectLayout.file(
                        objectsFactory.property<File>(
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

        override val javaVersion: Property<JavaVersion> = objectsFactory
            .propertyWithConvention(
                currentJvm.map { jvm ->
                    jvm.javaVersion
                        ?: throw GradleException(
                            "Kotlin could not get java version for the JDK installation: " +
                                    jvm.javaHome?.let { "'$it' " }.orEmpty()
                        )
                }
            )

        private val _jdkToolsJar: Property<File?> = JdkProvider.jdkToolsProperty(
            objectsFactory,
            currentJvm
        )
        override val jdkToolsJar: Provider<File?> = JdkProvider.defaultJdkToolsJarProvider(
            objectsFactory,
            javaVersion,
            _jdkToolsJar
        )

        override fun use(
            jdkHomeLocation: File,
            jdkVersion: JavaVersion
        ) {
            if (currentGradleVersion >= TOOLCHAIN_SUPPORTED_VERSION) {
                throw GradleException(
                    "Please use Java toolchains instead"
                )
            }

            val jvm = Jvm.forHome(jdkHomeLocation) as Jvm

            javaExecutable.set(jvm.javaExecutable)
            _jdkToolsJar.set(jvm.toolsJar)
            javaVersion.set(jdkVersion)
        }
    }

    private class DefaultJavaToolchainSetter(
        objectsFactory: ObjectFactory
    ) : KotlinJavaToolchain.JavaToolchainSetter,
        JdkProvider {

        private val javaLauncher: Property<JavaLauncher> = objectsFactory.property()

        override val currentJvm: Property<Jvm> = objectsFactory
            .propertyWithConvention(
                javaLauncher.map {
                    Jvm.forHome(it.metadata.installationPath.asFile) as Jvm
                }
            )

        override val javaExecutable: RegularFileProperty = objectsFactory
            .fileProperty()
            .apply {
                set(javaLauncher.map { it.executablePath })
            }

        override val javaVersion: Property<JavaVersion> = objectsFactory
            .property(
                javaLauncher.map {
                    JavaVersion.toVersion(it.metadata.languageVersion.asInt())
                }
            )

        private val _jdkToolsJar = JdkProvider.jdkToolsProperty(
            objectsFactory,
            currentJvm
        )

        override val jdkToolsJar: Provider<File?> = JdkProvider.defaultJdkToolsJarProvider(
            objectsFactory,
            javaVersion,
            _jdkToolsJar
        )

        override fun use(
            javaLauncher: Provider<JavaLauncher>
        ) {
            this.javaLauncher.set(javaLauncher)
        }
    }
}