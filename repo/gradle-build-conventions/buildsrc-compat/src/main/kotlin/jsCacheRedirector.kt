/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import com.github.gradle.node.exec.NodeExecConfiguration
import com.github.gradle.node.npm.exec.NpmExecRunner
import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.variant.VariantComputer
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport

const val DEFAULT_YARN_REGISTRY = "https://registry.yarnpkg.com"
const val NPM_REGISTRY_CACHE = "https://cache-redirector.jetbrains.com/registry.npmjs.org"

fun Project.configureJsCacheRedirector() {
    pluginManager.withPlugin("com.github.node-gradle.node") {
        tasks.withType<NpmTask>().configureEach {
            val command = npmCommand.orNull?.takeIf { it.isNotEmpty() }
                ?: args.get() // some tasks may be configured by putting command into args instead of npmCommand
            if (command.firstOrNull() in listOf("install", "ci")) {
                val workingDirectory = workingDir.orNull?.asFile ?: layout.projectDirectory.asFile
                val npmRcFile = workingDirectory.resolve(".npmrc")

                outputs.file(npmRcFile)

                doFirst {
                    logger.info("Setting Npm registry for $path to $NPM_REGISTRY_CACHE")
                    val nodeExecConfiguration =
                        NodeExecConfiguration(
                            listOf("config", "set", "registry", NPM_REGISTRY_CACHE, "--location=project"),
                            environment.get(),
                            workingDir.asFile.orNull,
                            ignoreExitValue.get(),
                            execOverrides.orNull
                        )
                    val npmExecRunner = objects.newInstance(NpmExecRunner::class.java)
                    npmExecRunner.executeNpmCommand(projectHelper, nodeExtension, nodeExecConfiguration, VariantComputer())
                }
            }
        }
    }

    if (project.parent == null) {
        plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
            the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().restoreYarnLockTaskProvider.configure {
                doLast {
                    // yarn 1.x doesn't and won't support overriding registry used in yarn.lock, so we need to replace it manually
                    // https://github.com/yarnpkg/yarn/issues/6436#issuecomment-426728911
                    val lockFile = outputFile.get()
                    lockFile.writeText(lockFile.readText().replace(DEFAULT_YARN_REGISTRY, NPM_REGISTRY_CACHE))
                }
            }
            the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().yarnLockMismatchReport.set(YarnLockMismatchReport.WARNING)
        }
    }
}
