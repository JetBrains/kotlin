/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import kotlin.test.Test

class KT62518KaptWorksWithIndirectDeps {
    @Test
    fun `let kapt extend from another configuration`() {
        val project = buildProjectWithJvm {
            plugins.apply(Kapt3GradleSubplugin::class.java)
        }
        val dep = project.dependencies.create("unresolved:dependency")
        val baseConfig: NamedDomainObjectProvider<Configuration> = project.configurations.register("baseConfig") {
            it.dependencies.add(dep)
        }
        project.configurations.named(Kapt3GradleSubplugin.MAIN_KAPT_CONFIGURATION_NAME) {
            it.extendsFrom(baseConfig.get())
        }
        project.evaluate()

        val taskSpecificConfig = project.configurations.getByName("kaptClasspath_kaptKotlin")

        assert(dep in taskSpecificConfig.allDependencies)
    }
}