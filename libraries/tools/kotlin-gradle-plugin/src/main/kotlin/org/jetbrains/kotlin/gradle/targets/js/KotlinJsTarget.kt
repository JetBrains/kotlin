/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator.Companion.runTaskNameSuffix
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator.Companion.testTaskNameSuffix
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.TaskHolder
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinBrowserJs
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinNodeJs
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

class KotlinJsTarget(project: Project, platformType: KotlinPlatformType) :
    KotlinOnlyTarget<KotlinJsCompilation>(project, platformType), KotlinJsTargetDsl {

    val testTaskName get() = lowerCamelCaseName(disambiguationClassifier, testTaskNameSuffix)
    val testTask: TaskHolder<KotlinTestReport>
        get() = project.kotlinTestRegistry.getOrCreateAggregatedTestTask(
            name = testTaskName,
            description = "Run JS tests for all platforms"
        )

    val runTaskName get() = lowerCamelCaseName(disambiguationClassifier, runTaskNameSuffix)
    val runTask
        get() = project.tasks.maybeCreate(runTaskName).also {
            it.description = "Run js on all configured platforms"
            if (runTaskName != runTaskNameSuffix) {
                project.whenEvaluated {
                    project.tasks.maybeCreate(runTaskNameSuffix).dependsOn(it)
                }
            }
        }

    val browser by lazy {
        KotlinBrowserJs(this).also {
            it.configure()
        }
    }
    val nodejs by lazy {
        KotlinNodeJs(this).also {
            it.configure()
        }
    }

    override fun browser(body: KotlinJsBrowserDsl.() -> Unit) {
        browser.body()
    }

    override fun nodejs(body: KotlinJsNodeDsl.() -> Unit) {
        nodejs.body()
    }

    fun useCommonJs() {
        compilations.all {
            it.compileKotlinTask.kotlinOptions {
                moduleKind = "commonjs"
                sourceMap = true
                sourceMapEmbedSources = null
            }
        }
    }
}