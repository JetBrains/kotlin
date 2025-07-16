/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodePlugin
import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport

const val DEFAULT_YARN_REGISTRY = "https://registry.yarnpkg.com"
const val NPM_REGISTRY = "https://cache-redirector.jetbrains.com/registry.npmjs.org"
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
            val npmRcFile = layout.projectDirectory.file(".npmrc")
            val npmSetRegistry = tasks.register<NpmTask>("npmSetRegistry") {
                group = NodePlugin.NPM_GROUP
                args.addAll(
                    listOf("config", "set", "registry", NPM_REGISTRY, "--location=project")
                )
                outputs.file(npmRcFile)
            }

            tasks.named("npmInstall").configure {
                dependsOn(npmSetRegistry)
            }

            tasks.register<Delete>("cleanNpmRc") {
                delete = setOf(npmRcFile)
            }

            tasks.named("clean").configure {
                dependsOn("cleanNpmRc")
            }
        }

        plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
            val npmRcFile = layout.buildDirectory.file("js/.npmrc")
            tasks.named("kotlinNpmInstall").configure {
                doFirst {
                    npmRcFile.get().asFile.writeText("registry=$NPM_REGISTRY")
                }
            }

            tasks.register<Delete>("cleanNpmRc") {
                delete = setOf(npmRcFile)
            }
        }
    }

    plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class) {
        extensions.configure(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec::class.java) {
            downloadBaseUrl.set(NODE_DIST)
        }
    }

    rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().restoreYarnLockTaskProvider.configure {
            doLast {
                // yarn 1.x doesn't and won't support overriding registry used in yarn.lock, so we need to replace it manually
                // https://github.com/yarnpkg/yarn/issues/6436#issuecomment-426728911
                val lockFile = outputFile.get()
                lockFile.writeText(lockFile.readText().replace(DEFAULT_YARN_REGISTRY, NPM_REGISTRY))
            }
        }
    }

    afterEvaluate {
        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().downloadBaseUrl.set(YARN_DIST)
            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().yarnLockMismatchReport.set(YarnLockMismatchReport.WARNING)
        }
    }
}
