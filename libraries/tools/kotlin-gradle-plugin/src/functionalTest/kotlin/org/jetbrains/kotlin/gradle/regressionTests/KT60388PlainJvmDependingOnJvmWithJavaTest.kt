/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import kotlin.test.Test
import kotlin.test.fail

class KT60388PlainJvmDependingOnJvmWithJavaTest {
    private val rootProject = buildProject {
        allprojects { project -> project.enableDefaultStdlibDependency(false) }
    }

    @Test
    fun `test - plain jvm - depends on - jvm withJava`() {
        val producer = buildProjectWithMPP(projectBuilder = { withName("producer").withParent(rootProject) })
        val consumer = buildProjectWithJvm(projectBuilder = { withName("consumer").withParent(rootProject) })

        producer.multiplatformExtension.apply {
            jvm().withJava()
        }

        consumer.kotlinJvmExtension.apply {
            project.dependencies {
                "api"(project(":producer"))
            }
        }

        assertConsumerCanResolveProducer(producer, consumer)
    }

    @Test
    fun `test - plain jvm - depends on - jvm withJava and java plugin`() {
        val producer = buildProjectWithMPP(projectBuilder = { withName("producer").withParent(rootProject) })
        val consumer = buildProjectWithJvm(projectBuilder = { withName("consumer").withParent(rootProject) })

        producer.multiplatformExtension.apply {
            producer.plugins.apply("java")
            jvm().withJava()
        }

        consumer.kotlinJvmExtension.apply {
            project.dependencies {
                "api"(project(":producer"))
            }
        }

        assertConsumerCanResolveProducer(producer, consumer)
    }

    private fun assertConsumerCanResolveProducer(producer: Project, consumer: Project) {
        val compileDependencyConfiguration = consumer.project.configurations.getByName(
            consumer.kotlinJvmExtension.target.compilations.getByName("main").compileDependencyConfigurationName
        )

        val resolvedCompileDependencyConfiguration = compileDependencyConfiguration.resolvedConfiguration
        resolvedCompileDependencyConfiguration.rethrowFailure()

        resolvedCompileDependencyConfiguration.firstLevelModuleDependencies.let { resolvedDependencies ->
            val resolvedProducer = resolvedDependencies.any { dependency -> dependency.module.id.name == "producer" }
            if (!resolvedProducer) fail("Expected ${producer.displayName} to be resolved by ${consumer.displayName}")
        }
    }
}
