/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.utils.withType
import javax.inject.Inject

abstract class KotlinBrowserJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrNpmBasedSubTarget(target, "browser"),
    KotlinJsBrowserDsl {

    private val nodeJsRoot = project.rootProject.kotlinNodeJsRootExtension
    private val nodeJs = project.kotlinNodeJsEnvSpec

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside browser using karma and webpack"

    override fun configureTestDependencies(test: KotlinJsTest, binary: JsIrBinary) {
        test.dependsOn(
            nodeJsRoot.npmInstallTaskProvider,
        )
        with(nodeJs) {
            test.dependsOn(project.nodeJsSetupTaskProvider)
        }
        test.dependsOn(nodeJsRoot.packageManagerExtension.map { it.postInstallTasks })

        test.dependsOn(binary.linkSyncTask)
    }

    override fun configureDefaultTestFramework(test: KotlinJsTest) {
        if (test.testFramework == null) {
            test.useKarma {
                useChromeHeadless()
            }
        }

        if (test.enabled) {
            nodeJsRoot.taskRequirements.addTaskRequirements(test)
        }
    }

    override fun commonWebpackConfig(body: Action<KotlinWebpackConfig>) {
        webpackTask {
            it.webpackConfigApplier(body)
        }
        runTask {
            it.webpackConfigApplier(body)
        }
        testTask {
            it.onTestFrameworkSet {
                if (it is KotlinKarma) {
                    body.execute(it.webpackConfig)
                }
            }
        }
    }

    override fun runTask(body: Action<KotlinWebpack>) {
        subTargetConfigurators
            .withType<WebpackConfigurator>()
            .configureEach {
                it.configureRun(body)
            }
    }

    override fun webpackTask(body: Action<KotlinWebpack>) {
        subTargetConfigurators
            .withType<WebpackConfigurator>()
            .configureEach {
                it.configureBuild(body)
            }
    }

    companion object {
        internal const val WEBPACK_TASK_NAME = "webpack"
    }
}
