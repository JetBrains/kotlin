/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodePlugin
import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport

const val NPM_REGISTRY = "https://packages.jetbrains.team/npm/p/kt/kotlin-dependencies"
const val NODE_DIST = "https://cache-redirector.jetbrains.com/nodejs.org/dist"
const val YARN_DIST = "https://cache-redirector.jetbrains.com/github.com/yarnpkg/yarn/releases/download"

fun Project.configureJsCacheRedirector() {
    plugins.withType<NodePlugin> {
        extensions.configure<NodeExtension> {
            distBaseUrl.set(NODE_DIST)
        }
    }

    project.allprojects {
        pluginManager.withPlugin("com.github.node-gradle.node") {
            val npmSetRegistry = tasks.register<NpmTask>("npmSetRegistry") {
                group = NodePlugin.NPM_GROUP
                args.addAll(
                    listOf("config", "set", "registry", NPM_REGISTRY, "--location=project")
                )
            }
            tasks.named("npmInstall").configure {
                dependsOn(npmSetRegistry)
            }
        }

        plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
            tasks.named("kotlinNpmInstall").configure {
                doFirst {
                    layout.buildDirectory.file("js/.npmrc")
                        .get().asFile.writeText("registry=$NPM_REGISTRY")
                }
            }
        }
    }

    plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class) {
        extensions.configure(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec::class.java) {
            downloadBaseUrl.set(NODE_DIST)
        }
    }

    afterEvaluate {
        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().downloadBaseUrl.set(YARN_DIST)
            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().yarnLockMismatchReport.set(YarnLockMismatchReport.WARNING)
        }
    }
}
