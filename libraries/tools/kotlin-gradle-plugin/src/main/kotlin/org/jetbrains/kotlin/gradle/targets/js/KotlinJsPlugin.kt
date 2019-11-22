/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import groovy.lang.Closure
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.reflect.TypeOf.typeOf
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.configureDefaultVersionsResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsSingleTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.utils.checkGradleCompatibility

interface Npm {
    operator fun invoke(packageName: String, version: String = "*"): NpmDependency

    operator fun invoke(org: String, packageName: String, version: String = "*"): NpmDependency
}

open class KotlinJsPlugin(
    private val kotlinPluginVersion: String
) : Plugin<Project> {

    override fun apply(project: Project) {
        // TODO get rid of this plugin, too? Use the 'base' plugin instead?
        // in fact, the attributes schema of the Java base plugin may be required to consume non-MPP Kotlin/JS libs,
        // so investigation is needed
        project.plugins.apply(JavaBasePlugin::class.java)

        checkGradleCompatibility()

        val dependencies = project.dependencies as ExtensionAware

        val npm: Npm = object : Npm, Closure<NpmDependency>(dependencies) {
            override operator fun invoke(packageName: String, version: String): NpmDependency {
                return NpmDependency(project, null, packageName, version)
            }

            override operator fun invoke(org: String, packageName: String, version: String): NpmDependency {
                return NpmDependency(project, org, packageName, version)
            }

            override fun call(vararg args: Any?): NpmDependency {
                val size = args.size
                if (size > 3) throw IllegalArgumentException(
                    """
                    Unable to add NPM dependency by $args
                    - npm('packageName') -> packageName:*
                    - npm('packageName', 'version') -> packageName:version
                    - npm('org', 'packageName', 'version') -> org/packageName:version
                    """.trimIndent()
                )

                if (size == 3) {
                    val (org, packageName, version) = args
                        .map { it as String }

                    return invoke(
                        org = org,
                        packageName = packageName,
                        version = version
                    )
                }

                val packageName = args[0] as String
                val version = if (size > 1) args[1] as String else null

                return if (version != null) {
                    invoke(
                        packageName = packageName,
                        version = version
                    )
                } else {
                    invoke(
                        packageName = packageName
                    )
                }
            }
        }

        dependencies
            .extensions
            .add(
                typeOf<Npm>(Npm::class.java),
                "npm",
                npm
            )

        val kotlinExtension = project.kotlinExtension as KotlinJsProjectExtension
        configureDefaultVersionsResolutionStrategy(project, kotlinPluginVersion)

        val target = KotlinJsSingleTargetPreset(project, kotlinPluginVersion).createTarget("Js")

        kotlinExtension.target = target

        project.components.addAll(target.components)
    }
}