/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.util.applyKotlinJvmPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.main
import org.jetbrains.kotlin.gradle.util.test
import kotlin.test.Test
import kotlin.test.fail

class KT58280JvmWithJavaTestCompileClasspath {
    /**
     * Context:
     * https://youtrack.jetbrains.com/issue/KT-58280/org.jetbrains.kotlin.jvm-Gradle-plugin-contributes-build-directories-to-the-test-compile-classpath
     *
     * This is not necessarily a 'regression' as IntelliJ and CLI compilations work fine.
     * Tools like eclipse did not expect this output.
     *
     * The commit the initially (and accidentally) changed the behavior was:
     * [Gradle] Implement KotlinWithJavaCompilation with underlying KotlinCompilationImpl Sebastian Sellmair* 04.10.22, 17:16
     * af198825899df9943814e2cb54d39868fff399fb
     *
     * This test 'fixates' the old behaviour.
     */
    @Test
    fun `test - KT58280 - jvmWithJava Target does not add main classes to test compile classpath`() {
        val project = buildProject()
        project.plugins.apply("java-library")
        project.applyKotlinJvmPlugin()
        project.repositories.mavenLocal()
        project.repositories.mavenCentralCacheRedirector()
        val kotlin = project.kotlinJvmExtension

        /* This kind of association is not required for java: java plugin handles this separately */
        kotlin.target.compilations.test.internal.configurations.compileDependencyConfiguration.resolvedConfiguration.files.forEach { file ->
            if (file in kotlin.target.compilations.main.output.allOutputs) {
                fail("Unexpected file in test compile dependencies: $file")
            }
        }
    }
}